package my.yeamu.pluginsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Сердце PluginSync: скачивает plugins.json, сверяет с установленными jar,
 * скачивает новые версии в pending/ и при выключении сервера ставит их в plugins/.
 *
 * Вся работа с сетью и диском — в одном фоновом потоке (главный поток не блокируется).
 * Исключение — {@link #applyPending()}: он вызывается при остановке сервера, когда ждать уже можно.
 */
public final class UpdateService {

    /** Состояние одного плагина из списка. */
    public enum Status {
        UP_TO_DATE("&aактуален"),
        OUTDATED("&eесть обновление"),
        MISSING("&eне установлен"),
        PENDING("&bскачан, ждёт перезапуска"),
        NEWER("&7установлена версия новее"),
        SKIPPED("&7не управляется (managed/ignore)"),
        ERROR("&cошибка");

        public final String label;

        Status(String label) { this.label = label; }
    }

    public record PluginState(PluginManifest.Entry entry, String installedVersion, String installedFile,
                              Status status, String error) { }

    /** Ожидающая замена: newFile = null — удаление плагина. */
    public record Pending(String name, String version, String newFile, String oldFile, String sha256) { }

    /** Итог проверки — для сообщений админам. */
    public record Report(boolean ok, String error, int manifestVersion, boolean fromCache,
                         List<String> downloaded, List<String> available, List<String> failed,
                         List<String> removals) {
        public boolean somethingNew() { return !downloaded.isEmpty() || !removals.isEmpty(); }
    }

    /** Версия и файл плагина, найденного в plugins/. */
    private record Installed(String name, String version, Path file) { }

    /** Один плагин из пачки на скачивание. */
    private record Job(PluginManifest.Entry entry, String key, String installedVersion, String installedFile, String what) { }

    /** Шаг установки пачки — чтобы откатить, если что-то пошло не так. */
    private record Undo(Path placed, Path staged, Path old, Path saved) { }

    private final PluginSyncPlugin plugin;
    private final Path pluginsDir;
    private final Path pendingDir;
    private final Path pendingFile;
    private final Path cacheFile;
    private final Path cacheSig;
    private final Path logFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PluginSync-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final HttpClient http;

    /** name (lower) → ожидающая замена. */
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private volatile Map<String, PluginState> states = Map.of();
    private volatile PluginManifest manifest;
    private volatile Instant lastCheck;
    private volatile String lastError;

    public UpdateService(PluginSyncPlugin plugin) {
        this.plugin = plugin;
        this.pluginsDir = plugin.getDataFolder().toPath().toAbsolutePath().getParent();
        Path data = plugin.getDataFolder().toPath();
        this.pendingDir = data.resolve("pending");
        this.pendingFile = data.resolve("pending.json");
        this.cacheFile = data.resolve("plugins-cache.json");
        this.cacheSig = data.resolve("plugins-cache.json.sig");
        this.logFile = data.resolve("updates.log");
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // NORMAL не пускает с https на http
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ------------------------------------------------------------------ состояние

    public Map<String, PluginState> states() { return states; }
    public Map<String, Pending> pending() { return Map.copyOf(pending); }
    public boolean hasPending() { return !pending.isEmpty(); }
    public PluginManifest manifest() { return manifest; }
    public Instant lastCheck() { return lastCheck; }
    public String lastError() { return lastError; }
    public boolean isBusy() { return busy.get(); }

    /** Загружает pending.json после старта (на случай, если прошлая остановка была аварийной). */
    public void loadPending() {
        try {
            if (!Files.exists(pendingFile)) return;
            List<Pending> list = gson.fromJson(Files.readString(pendingFile, StandardCharsets.UTF_8),
                    new TypeToken<List<Pending>>() { }.getType());
            if (list == null) return;
            for (Pending p : list) {
                // Файл мог пропасть — тогда запись бесполезна, следующая проверка скачает заново
                if (p.newFile() != null && !Files.exists(pendingDir.resolve(p.newFile()))) continue;
                pending.put(p.name().toLowerCase(Locale.ROOT), p);
            }
            if (!pending.isEmpty()) {
                plugin.getLogger().info("Ждут перезапуска: " + String.join(", ", pendingNames()));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось прочитать pending.json: " + e.getMessage());
        }
    }

    public List<String> pendingNames() {
        List<String> out = new ArrayList<>();
        for (Pending p : pending.values()) {
            out.add(p.newFile() == null ? p.name() + " (удаление)" : p.name() + " " + p.version());
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    // ------------------------------------------------------------------ проверка

    /**
     * Проверить обновления. Вызывать из главного потока: здесь снимается список загруженных
     * плагинов, а всё остальное делается в фоне.
     *
     * @param download true — скачать даже в режиме notify
     * @return null, если проверка уже идёт
     */
    public CompletableFuture<Report> check(boolean download) {
        if (!busy.compareAndSet(false, true)) return null;
        SyncConfig cfg = plugin.syncConfig();
        // Версии загруженных плагинов — снимок в главном потоке
        Map<String, String> runtime = new HashMap<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            runtime.put(p.getName().toLowerCase(Locale.ROOT), p.getPluginMeta().getVersion());
        }
        boolean doDownload = download || cfg.mode() == SyncConfig.Mode.AUTO;
        return CompletableFuture.supplyAsync(() -> runCheck(cfg, runtime, doDownload), worker)
                .whenComplete((r, e) -> busy.set(false));
    }

    private Report runCheck(SyncConfig cfg, Map<String, String> runtime, boolean doDownload) {
        List<String> downloaded = new ArrayList<>();
        List<String> available = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> removals = new ArrayList<>();
        List<Job> jobs = new ArrayList<>();
        boolean fromCache = false;
        PluginManifest m;
        try {
            m = fetchManifest(cfg);
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            m = loadCache(cfg);
            if (m == null) {
                log("ОШИБКА списка плагинов: " + lastError);
                plugin.getLogger().warning("Список плагинов недоступен (" + lastError + "), кэша нет");
                lastCheck = Instant.now();
                return new Report(false, lastError, 0, false, downloaded, available, failed, removals);
            }
            fromCache = true;
            plugin.getLogger().warning("Сайт недоступен (" + lastError + "), использую кэш v" + m.version());
        }
        manifest = m;
        Map<String, Installed> installed = scanPlugins(runtime);
        Map<String, PluginState> result = new LinkedHashMap<>();

        for (PluginManifest.Entry e : m.plugins()) {
            String key = e.name().toLowerCase(Locale.ROOT);
            Installed inst = installed.get(key);
            String iv = inst == null ? null : inst.version();
            String ifile = inst == null ? null : inst.file().getFileName().toString();
            if (!cfg.manages(e.name())) {
                result.put(key, new PluginState(e, iv, ifile, Status.SKIPPED, null));
                continue;
            }
            // Уже скачана именно эта сборка
            Pending p = pending.get(key);
            if (p != null && e.sha256().equals(p.sha256())) {
                result.put(key, new PluginState(e, iv, ifile, Status.PENDING, null));
                continue;
            }
            Status st;
            if (inst == null) {
                st = Status.MISSING;
            } else if (e.sha256().equals(sha256(inst.file()))) {
                st = Status.UP_TO_DATE;
            } else if (!cfg.allowDowngrade() && PluginManifest.compareVersions(e.version(), iv) < 0) {
                st = Status.NEWER;
            } else {
                st = Status.OUTDATED;
            }
            if (st == Status.UP_TO_DATE || st == Status.NEWER) {
                dropPending(key); // старая скачанная версия больше не нужна
                result.put(key, new PluginState(e, iv, ifile, st, null));
                continue;
            }
            boolean want = st == Status.OUTDATED || cfg.installMissing();
            if (!want) {
                result.put(key, new PluginState(e, iv, ifile, st, null));
                continue;
            }
            String what = e.name() + " " + (iv == null ? "(новый)" : iv) + " → " + e.version();
            if (!doDownload) {
                available.add(what);
                result.put(key, new PluginState(e, iv, ifile, st, null));
                continue;
            }
            // Не качаем сразу: сначала собираем всю пачку, потом качаем её целиком
            jobs.add(new Job(e, key, iv, ifile, what));
            result.put(key, new PluginState(e, iv, ifile, st, null));
        }
        downloadAll(cfg, jobs, result, downloaded, failed);

        // Плагины, которые нужно удалить
        for (PluginManifest.RemoveRule r : m.remove()) {
            for (Installed inst : installed.values()) {
                String file = inst.file().getFileName().toString();
                boolean match = (!r.name().isEmpty() && r.name().equalsIgnoreCase(inst.name()))
                        || (!r.file().isEmpty() && r.file().equals(file));
                if (!match || !cfg.manages(inst.name()) || inst.name().equalsIgnoreCase(plugin.getName())) continue;
                String key = inst.name().toLowerCase(Locale.ROOT);
                if (pending.containsKey(key) && pending.get(key).newFile() == null) continue;
                if (!doDownload) {
                    available.add(inst.name() + " — удалить");
                    continue;
                }
                pending.put(key, new Pending(inst.name(), inst.version(), null, file, null));
                removals.add(inst.name());
                log("УДАЛЕНИЕ запланировано: " + inst.name() + " (" + file + ")");
            }
        }

        states = result;
        savePending();
        lastCheck = Instant.now();
        if (!fromCache) lastError = null;
        return new Report(true, fromCache ? lastError : null, m.version(), fromCache,
                downloaded, available, failed, removals);
    }

    /**
     * Скачать всю пачку параллельно (download.parallel потоков).
     * В pending всё попадает разом — только когда вся пачка докачалась (удачно или с ошибкой).
     */
    private void downloadAll(SyncConfig cfg, List<Job> jobs, Map<String, PluginState> result,
                             List<String> downloaded, List<String> failed) {
        if (jobs.isEmpty()) return;
        int threads = Math.max(1, Math.min(cfg.parallelDownloads(), jobs.size()));
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "PluginSync-download");
            t.setDaemon(true);
            return t;
        });
        log("ПАКЕТ: скачиваю " + jobs.size() + " шт. (" + threads + " одновременно)");
        Map<Job, CompletableFuture<Void>> futures = new LinkedHashMap<>();
        for (Job j : jobs) {
            futures.put(j, CompletableFuture.runAsync(() -> {
                try {
                    download(cfg, j.entry());
                } catch (Exception ex) {
                    throw new CompletionException(ex);
                }
            }, pool));
        }
        Map<String, Pending> ready = new LinkedHashMap<>();
        try {
            for (Map.Entry<Job, CompletableFuture<Void>> f : futures.entrySet()) {
                Job j = f.getKey();
                PluginManifest.Entry e = j.entry();
                try {
                    f.getValue().join();
                    ready.put(j.key(), new Pending(e.name(), e.version(), e.file(), j.installedFile(), e.sha256()));
                    downloaded.add(j.what());
                    log("СКАЧАН " + j.what() + " (" + e.file() + ")");
                    result.put(j.key(), new PluginState(e, j.installedVersion(), j.installedFile(), Status.PENDING, null));
                } catch (CompletionException | java.util.concurrent.CancellationException ce) {
                    Throwable ex = ce.getCause() == null ? ce : ce.getCause();
                    String err = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    failed.add(e.name() + ": " + err);
                    log("ОШИБКА " + j.what() + ": " + err);
                    plugin.getLogger().warning("Не удалось скачать " + e.name() + ": " + err);
                    result.put(j.key(), new PluginState(e, j.installedVersion(), j.installedFile(), Status.ERROR, err));
                }
            }
        } finally {
            pool.shutdownNow();
        }
        // Вся пачка разом: старые скачанные версии заменяем новыми
        for (Map.Entry<String, Pending> r : ready.entrySet()) {
            Pending old = pending.remove(r.getKey());
            if (old != null && old.newFile() != null && !old.newFile().equals(r.getValue().newFile())) {
                try {
                    Files.deleteIfExists(pendingDir.resolve(old.newFile()));
                } catch (IOException ignored) { }
            }
            pending.put(r.getKey(), r.getValue());
        }
        if (!ready.isEmpty()) {
            log("ПАКЕТ скачан: " + ready.size() + " из " + jobs.size());
        }
    }

    // ------------------------------------------------------------------ сеть

    private PluginManifest fetchManifest(SyncConfig cfg) throws Exception {
        if (cfg.manifestUrl() == null) throw new IllegalStateException("manifest.url не задан или не HTTPS");
        if (cfg.publicKey().isEmpty()) throw new IllegalStateException("manifest.public-key не задан");
        byte[] body = get(cfg, cfg.manifestUrl());
        byte[] sigRaw = get(cfg, URI.create(cfg.manifestUrl() + ".sig"));
        byte[] sig = Base64.getDecoder().decode(new String(sigRaw, StandardCharsets.UTF_8).trim());
        if (!verify(body, sig, cfg.publicKey())) throw new SecurityException("подпись plugins.json не совпала");
        PluginManifest m = PluginManifest.parse(new String(body, StandardCharsets.UTF_8));
        // Кэш на случай недоступности сайта (вместе с подписью — при чтении проверим снова)
        try {
            Files.createDirectories(cacheFile.getParent());
            writeAtomic(cacheFile, body);
            writeAtomic(cacheSig, sigRaw);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить кэш: " + e.getMessage());
        }
        return m;
    }

    private PluginManifest loadCache(SyncConfig cfg) {
        try {
            if (!Files.exists(cacheFile) || !Files.exists(cacheSig) || cfg.publicKey().isEmpty()) return null;
            byte[] body = Files.readAllBytes(cacheFile);
            byte[] sig = Base64.getDecoder().decode(Files.readString(cacheSig, StandardCharsets.UTF_8).trim());
            if (!verify(body, sig, cfg.publicKey())) return null;
            return PluginManifest.parse(new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] get(SyncConfig cfg, URI uri) throws Exception {
        HttpResponse<byte[]> r = http.send(request(cfg, uri), HttpResponse.BodyHandlers.ofByteArray());
        if (!"https".equalsIgnoreCase(r.uri().getScheme())) throw new SecurityException("ответ пришёл не по HTTPS");
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " для " + uri);
        return r.body();
    }

    private HttpRequest request(SyncConfig cfg, URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(cfg.timeoutSeconds()))
                .header("User-Agent", "PluginSync/" + plugin.getPluginMeta().getVersion())
                .header("Cache-Control", "no-cache")
                .GET().build();
    }

    /** Скачать jar во временный файл, проверить размер и SHA-256, затем атомарно переместить в pending/. */
    private void download(SyncConfig cfg, PluginManifest.Entry e) throws Exception {
        URI uri = URI.create(e.url());
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new SecurityException("URL не HTTPS");
        if (!cfg.domainAllowed(uri.getHost())) throw new SecurityException("домен не в белом списке: " + uri.getHost());
        if (e.size() > cfg.maxSizeBytes()) throw new IOException("файл больше max-size-mb");
        Files.createDirectories(pendingDir);
        Path part = pendingDir.resolve(e.file() + ".part");
        Path target = pendingDir.resolve(e.file());
        try {
            HttpResponse<InputStream> r = http.send(request(cfg, uri), HttpResponse.BodyHandlers.ofInputStream());
            if (!"https".equalsIgnoreCase(r.uri().getScheme()) || !cfg.domainAllowed(r.uri().getHost())) {
                r.body().close();
                throw new SecurityException("редирект на запрещённый адрес: " + r.uri());
            }
            if (r.statusCode() != 200) {
                r.body().close();
                throw new IOException("HTTP " + r.statusCode());
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = r.body(); var out = Files.newOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > cfg.maxSizeBytes()) throw new IOException("файл больше max-size-mb");
                    md.update(buf, 0, n);
                    out.write(buf, 0, n);
                }
            }
            if (e.size() > 0 && total != e.size()) throw new IOException("размер " + total + " вместо " + e.size());
            String sha = HexFormat.of().formatHex(md.digest());
            if (!sha.equals(e.sha256())) throw new SecurityException("SHA-256 не совпал");
            move(part, target);
        } finally {
            Files.deleteIfExists(part); // откат: недокачанный файл не остаётся
        }
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

    // ------------------------------------------------------------------ диск

    /** Ищет все jar в plugins/ и читает из них name/version (plugin.yml или paper-plugin.yml). */
    private Map<String, Installed> scanPlugins(Map<String, String> runtime) {
        Map<String, Installed> out = new HashMap<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : ds) {
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    ZipEntry en = zip.getEntry("paper-plugin.yml");
                    if (en == null) en = zip.getEntry("plugin.yml");
                    if (en == null) continue;
                    YamlConfiguration y;
                    try (var reader = new InputStreamReader(zip.getInputStream(en), StandardCharsets.UTF_8)) {
                        y = YamlConfiguration.loadConfiguration(reader);
                    }
                    String name = y.getString("name");
                    if (name == null || name.isBlank()) continue;
                    String key = name.toLowerCase(Locale.ROOT);
                    // Версия работающего плагина точнее, чем из файла (файл мог уже смениться)
                    String version = runtime.getOrDefault(key, y.getString("version", "?"));
                    out.put(key, new Installed(name, version, jar));
                } catch (Exception ignored) {
                    // повреждённый или чужой jar — пропускаем
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось прочитать папку plugins: " + e.getMessage());
        }
        return out;
    }

    private static String sha256(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private void dropPending(String key) {
        Pending old = pending.remove(key);
        if (old != null && old.newFile() != null) {
            try {
                Files.deleteIfExists(pendingDir.resolve(old.newFile()));
            } catch (IOException ignored) { }
        }
    }

    private synchronized void savePending() {
        try {
            Files.createDirectories(pendingFile.getParent());
            writeAtomic(pendingFile, gson.toJson(new ArrayList<>(pending.values())).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить pending.json: " + e.getMessage());
        }
    }

    /** Удалить все скачанные, но ещё не поставленные обновления. */
    public CompletableFuture<Integer> discardPending() {
        return CompletableFuture.supplyAsync(() -> {
            int n = pending.size();
            for (String key : new ArrayList<>(pending.keySet())) dropPending(key);
            savePending();
            log("ОТМЕНА ожидающих обновлений: " + n);
            return n;
        }, worker);
    }

    private static void writeAtomic(Path target, byte[] data) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, data);
        move(tmp, target);
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private synchronized void log(String line) {
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, "[" + Instant.now() + "] " + line + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    // ------------------------------------------------------------------ применение при выключении

    /**
     * Ставит ВСЕ скачанные jar в plugins/ одной пачкой. Вызывается из onDisable при остановке сервера.
     * 1) Проверяем всю пачку (файлы на месте, SHA-256 совпадает). Хоть один битый — не ставим ничего.
     * 2) Старые jar копируем в plugins/PluginSync/backup/<время>/.
     * 3) Меняем файлы. Linux: старый jar удаляется, новый кладётся со своим именем.
     *    Windows (файл заблокирован): новый jar идёт в папку update/ под именем старого — Paper заменит сам.
     * 4) Ошибка посередине — откат всей пачки, сервер запустится со старыми версиями.
     */
    public void applyPending() {
        worker.shutdown();
        // Если как раз идёт скачивание пачки — дождаться её целиком, а не ставить половину
        int wait = Math.min(120, Math.max(10, plugin.syncConfig().timeoutSeconds() * 2));
        try {
            if (!worker.awaitTermination(wait, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Скачивание не успело закончиться — ставлю только то, что было скачано целиком раньше");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (pending.isEmpty()) return;
        List<Pending> batch = new ArrayList<>(pending.values());
        batch.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        // 1. Проверка всей пачки до того, как что-то трогать
        List<String> bad = new ArrayList<>();
        for (Pending p : batch) {
            if (p.newFile() == null) continue;
            Path staged = pendingDir.resolve(p.newFile());
            if (!Files.exists(staged) || (p.sha256() != null && !p.sha256().equals(sha256(staged)))) bad.add(p.name());
        }
        if (!bad.isEmpty()) {
            for (String n : bad) dropPending(n.toLowerCase(Locale.ROOT));
            savePending();
            String m = "Пакет обновлений НЕ поставлен: повреждены или пропали " + String.join(", ", bad)
                    + ". После запуска они скачаются заново, и вся пачка встанет при следующем перезапуске.";
            plugin.getLogger().warning(m);
            log("ПАКЕТ ОТЛОЖЕН: " + m);
            return;
        }

        // 2–3. Резервная копия и замена
        Path updateDir = Bukkit.getUpdateFolderFile().toPath();
        Path backupRoot = plugin.getDataFolder().toPath().resolve("backup");
        Path backup = backupRoot.resolve(String.valueOf(System.currentTimeMillis()));
        List<Undo> undo = new ArrayList<>();
        List<String> done = new ArrayList<>();
        try {
            Files.createDirectories(backup);
            for (Pending p : batch) {
                Path old = p.oldFile() == null ? null : pluginsDir.resolve(p.oldFile());
                Path saved = null;
                if (old != null && Files.exists(old)) {
                    saved = backup.resolve(p.oldFile());
                    Files.copy(old, saved, StandardCopyOption.REPLACE_EXISTING);
                }
                if (p.newFile() == null) {
                    // Удаление плагина
                    if (old != null) Files.deleteIfExists(old);
                    undo.add(new Undo(null, null, old, saved));
                    done.add(p.name() + " (удалён)");
                    continue;
                }
                Path staged = pendingDir.resolve(p.newFile());
                Path target = pluginsDir.resolve(p.newFile());
                try {
                    if (old != null && !old.equals(target)) Files.deleteIfExists(old);
                    move(staged, target);
                    undo.add(new Undo(target, staged, old, saved));
                    done.add(p.name() + " " + p.version());
                } catch (IOException locked) {
                    // Файл занят (Windows) — отдаём замену самому Paper через папку update/
                    if (!Files.exists(staged)) throw locked;
                    Files.createDirectories(updateDir);
                    String name = old != null && Files.exists(old) ? p.oldFile() : p.newFile();
                    Path upd = updateDir.resolve(name);
                    move(staged, upd);
                    undo.add(new Undo(upd, staged, null, null));
                    done.add(p.name() + " " + p.version() + " (через update/)");
                }
            }
        } catch (Exception e) {
            // 4. Откат всей пачки в обратном порядке
            for (int i = undo.size() - 1; i >= 0; i--) {
                Undo u = undo.get(i);
                try {
                    if (u.placed() != null && Files.exists(u.placed())) {
                        if (u.staged() != null) move(u.placed(), u.staged());
                        else Files.deleteIfExists(u.placed());
                    }
                    if (u.saved() != null && u.old() != null && !Files.exists(u.old())) {
                        Files.copy(u.saved(), u.old(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ignored) { }
            }
            plugin.getLogger().warning("Не удалось поставить пачку обновлений (" + e.getMessage()
                    + "), всё откатено — сервер запустится со старыми версиями");
            log("ПАКЕТ ОТКАЧЕН: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }
        pending.clear();
        savePending();
        pruneBackups(backupRoot, 3);
        log("ПАКЕТ УСТАНОВЛЕН (" + done.size() + "): " + String.join(", ", done));
        plugin.getLogger().info("Пачка обновлений поставлена (" + done.size() + "): " + String.join(", ", done)
                + ". Заработает после запуска сервера.");
    }

    /** Оставить только последние keep резервных копий. */
    private static void pruneBackups(Path root, int keep) {
        try (var s = Files.list(root)) {
            List<Path> dirs = s.filter(Files::isDirectory).sorted().toList();
            for (int i = 0; i < dirs.size() - keep; i++) {
                try (var w = Files.walk(dirs.get(i))) {
                    w.sorted(java.util.Comparator.reverseOrder()).forEach(x -> {
                        try {
                            Files.deleteIfExists(x);
                        } catch (IOException ignored) { }
                    });
                }
            }
        } catch (IOException ignored) { }
    }

    /** Остановить фоновый поток без применения (например, при отключении плагина без остановки сервера). */
    public void shutdownWorker() {
        worker.shutdownNow();
    }
}
