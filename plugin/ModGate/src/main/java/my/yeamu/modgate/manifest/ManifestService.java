package my.yeamu.modgate.manifest;

import my.yeamu.modgate.ModGatePlugin;
import my.yeamu.modgate.config.GateConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Асинхронно скачивает manifest.json, проверяет подпись (если ключ задан)
 * и хранит последнюю удачную версию в памяти и в кэше на диске.
 */
public final class ManifestService {

    private final ModGatePlugin plugin;
    private final Path cacheFile;
    private final Path cacheSigFile;
    private final HttpClient http;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    /** Текущий манифест; volatile — читается из главного потока, пишется из HTTP-потока. */
    private volatile Manifest current;
    private volatile Instant lastSuccess;
    private volatile String lastError;
    private volatile boolean fromCache;

    public ManifestService(ModGatePlugin plugin) {
        this.plugin = plugin;
        this.cacheFile = plugin.getDataFolder().toPath().resolve("manifest-cache.json");
        this.cacheSigFile = plugin.getDataFolder().toPath().resolve("manifest-cache.json.sig");
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // NORMAL не пускает с https на http
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Загружает кэш с диска (вызывается при старте). */
    public void loadCache() {
        try {
            if (Files.exists(cacheFile)) {
                String json = Files.readString(cacheFile, StandardCharsets.UTF_8);
                current = Manifest.parse(json);
                fromCache = true;
                plugin.getLogger().info("Манифест загружен из кэша: версия " + current.version());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось прочитать кэш манифеста: " + e.getMessage());
        }
    }

    /** Запускает обновление манифеста, не блокируя вызывающий поток. */
    public CompletableFuture<Boolean> refresh() {
        GateConfig cfg = plugin.gateConfig();
        if (cfg.manifestUrl() == null) return CompletableFuture.completedFuture(false);
        if (!refreshing.compareAndSet(false, true)) return CompletableFuture.completedFuture(false);

        Duration timeout = Duration.ofSeconds(Math.max(1, cfg.httpTimeoutSeconds()));
        CompletableFuture<HttpResponse<byte[]>> manifestF = get(cfg.manifestUrl(), timeout);
        CompletableFuture<HttpResponse<byte[]>> sigF = cfg.publicKey().isEmpty()
                ? CompletableFuture.completedFuture(null)
                : get(URI.create(cfg.manifestUrl() + ".sig"), timeout);

        return manifestF.thenCombine(sigF, (resp, sigResp) -> {
            if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
            byte[] body = resp.body();
            byte[] sig = null;
            if (!cfg.publicKey().isEmpty()) {
                if (sigResp == null || sigResp.statusCode() != 200)
                    throw new IllegalStateException("нет файла подписи (.sig)");
                sig = Base64.getDecoder().decode(new String(sigResp.body(), StandardCharsets.UTF_8).trim());
                if (!verify(body, sig, cfg.publicKey()))
                    throw new IllegalStateException("подпись манифеста не совпала");
            }
            Manifest parsed = Manifest.parse(new String(body, StandardCharsets.UTF_8));
            writeCache(body, sig);
            return parsed;
        }).handle((parsed, err) -> {
            refreshing.set(false);
            if (err != null) {
                Throwable cause = err.getCause() != null ? err.getCause() : err;
                lastError = cause.getClass().getSimpleName() + ": " + cause.getMessage();
                plugin.getLogger().warning("Не удалось обновить манифест (" + lastError + "), использую "
                        + (current != null ? "кэш, версия " + current.version() : "— манифеста нет"));
                return false;
            }
            Manifest old = current;
            current = parsed;
            fromCache = false;
            lastSuccess = Instant.now();
            lastError = null;
            if (old == null || old.version() != parsed.version()) {
                plugin.getLogger().info("Актуальный манифест: версия " + parsed.version()
                        + ", ModSync " + parsed.modVersion());
            }
            return true;
        });
    }

    private CompletableFuture<HttpResponse<byte[]>> get(URI uri, Duration timeout) {
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("User-Agent", "ModGate/" + plugin.getPluginMeta().getVersion())
                .header("Cache-Control", "no-cache")
                .GET().build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenApply(r -> {
            // Защита от редиректа на не-HTTPS
            if (!"https".equalsIgnoreCase(r.uri().getScheme()))
                throw new IllegalStateException("ответ пришёл не по HTTPS");
            return r;
        });
    }

    static boolean verify(byte[] data, byte[] sig, String publicKeyB64) {
        try {
            PublicKey key = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64)));
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(key);
            s.update(data);
            return s.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    /** Атомарно сохраняет кэш на диск (мы уже в фоновом потоке). */
    private void writeCache(byte[] body, byte[] sig) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Path tmp = cacheFile.resolveSibling("manifest-cache.json.tmp");
            Files.write(tmp, body);
            Files.move(tmp, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            if (sig != null) Files.writeString(cacheSigFile, Base64.getEncoder().encodeToString(sig));
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить кэш манифеста: " + e.getMessage());
        }
    }

    /** Обновить и вызвать callback уже в главном потоке. */
    public void refreshThen(Consumer<Boolean> callback) {
        refresh().thenAccept(ok -> plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(ok)));
    }

    public Manifest current() { return current; }
    public Instant lastSuccess() { return lastSuccess; }
    public String lastError() { return lastError; }
    public boolean isFromCache() { return fromCache; }
}
