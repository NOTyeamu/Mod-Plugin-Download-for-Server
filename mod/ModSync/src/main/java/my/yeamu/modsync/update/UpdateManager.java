package my.yeamu.modsync.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import my.yeamu.modsync.ModSyncConfig;
import my.yeamu.modsync.SyncLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Проверка и установка обновлений модов.
 * Вся работа идёт в отдельном потоке, экран только читает volatile-поля.
 *
 * Порядок: скачать манифест и подпись → проверить Ed25519 → составить план →
 * скачать всё во временную папку с проверкой SHA-256 → одной транзакцией
 * заменить файлы в mods/ (с резервной копией и откатом при ошибке).
 */
public final class UpdateManager {

    /** Этап работы — показывается на экране. */
    public enum Phase {
        IDLE("Ожидание"),
        FETCHING("Получаю список модов с сайта…"),
        VERIFYING("Проверяю подпись манифеста…"),
        PLANNING("Сравниваю моды…"),
        DOWNLOADING("Скачиваю моды…"),
        APPLYING("Устанавливаю моды…"),
        UP_TO_DATE("Все моды актуальны!"),
        RESTART_REQUIRED("Моды обновлены — нужен перезапуск игры"),
        OFFLINE("Сайт недоступен"),
        SIGNATURE_BAD("Подпись манифеста не совпала"),
        FAILED("Ошибка обновления");

        public final String title;
        Phase(String title) { this.title = title; }

        public boolean active() {
            return this == FETCHING || this == VERIFYING || this == PLANNING || this == DOWNLOADING || this == APPLYING;
        }
    }

    private static final UpdateManager INSTANCE = new UpdateManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    public static UpdateManager get() { return INSTANCE; }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ModSync-Updater");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER) // редиректы обрабатываем сами, проверяя белый список
            .connectTimeout(Duration.ofSeconds(ModSyncConfig.CONNECT_TIMEOUT))
            .build();

    // ---- состояние для экрана
    private volatile Phase phase = Phase.IDLE;
    private volatile String detail = "";
    private volatile String currentFile = "";
    private volatile long bytesDone;
    private volatile long bytesTotal;
    private volatile int filesDone;
    private volatile int filesTotal;
    private volatile int changedCount;
    private volatile boolean hadPending;
    private volatile ManifestData lastManifest;

    private final Path gameDir = FabricLoader.getInstance().getGameDir();
    private final Path modsDir = gameDir.resolve("mods");
    private final Path baseDir = gameDir.resolve(".modsync");
    private final Path tmpDir = baseDir.resolve("tmp");
    private final Path stateFile = baseDir.resolve("state.json");
    private final Path installedFile = baseDir.resolve("installed.json");

    private UpdateManager() {}

    // ------------------------------------------------------------------ публичное API

    /** Запускает проверку в фоне (если уже идёт — ничего не делает). */
    public void startCheck() {
        if (!running.compareAndSet(false, true)) return;
        phase = Phase.FETCHING;
        detail = "";
        worker.execute(() -> {
            try {
                runCheck();
            } catch (Throwable t) {
                SyncLog.error("Непредвиденная ошибка обновления", t);
                phase = Phase.FAILED;
                detail = t.getClass().getSimpleName() + ": " + t.getMessage();
            } finally {
                running.set(false);
                cleanupTmp();
            }
        });
    }

    public boolean shouldShowOnStartup() {
        Phase p = phase;
        return p.active() || p == Phase.RESTART_REQUIRED || p == Phase.OFFLINE
                || p == Phase.SIGNATURE_BAD || p == Phase.FAILED;
    }

    public Phase phase() { return phase; }
    public String detail() { return detail; }
    public String currentFile() { return currentFile; }
    public long bytesDone() { return bytesDone; }
    public long bytesTotal() { return bytesTotal; }
    public int filesDone() { return filesDone; }
    public int filesTotal() { return filesTotal; }
    public int changedCount() { return changedCount; }
    public boolean hadPending() { return hadPending; }
    public ManifestData lastManifest() { return lastManifest; }

    public static String modVersion() {
        return FabricLoader.getInstance().getModContainer("modsync")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");
    }

    public static String gameVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    /** Версия манифеста, которой полностью соответствует папка mods/ (0 — неизвестно). */
    public int installedManifestVersion() {
        try {
            if (!Files.exists(stateFile)) return 0;
            JsonObject o = JsonParser.parseString(Files.readString(stateFile)).getAsJsonObject();
            return o.has("manifestVersion") ? o.get("manifestVersion").getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Опциональные моды (не участвуют в хеше) из последнего проверенного манифеста. */
    public Set<String> optionalShas() {
        ManifestData m = lastManifest;
        if (m != null) return m.optional();
        try {
            if (!Files.exists(stateFile)) return Set.of();
            JsonObject o = JsonParser.parseString(Files.readString(stateFile)).getAsJsonObject();
            Set<String> s = new HashSet<>();
            if (o.has("optional")) o.getAsJsonArray("optional").forEach(e -> s.add(e.getAsString()));
            return s;
        } catch (Exception e) {
            return Set.of();
        }
    }

    // ------------------------------------------------------------------ основной процесс

    private void runCheck() throws Exception {
        SyncLog.info("Проверка обновлений: " + ModSyncConfig.MANIFEST_URL);
        bytesDone = bytesTotal = 0;
        filesDone = filesTotal = changedCount = 0;
        currentFile = "";
        hadPending = false;

        // 1. Манифест и подпись
        byte[] manifestBytes;
        byte[] sigBytes;
        try {
            manifestBytes = fetchSmall(ModSyncConfig.MANIFEST_URL);
            sigBytes = fetchSmall(ModSyncConfig.MANIFEST_URL + ".sig");
        } catch (Exception e) {
            SyncLog.warn("Сайт недоступен: " + e);
            phase = Phase.OFFLINE;
            detail = "Не удалось связаться с сайтом. Можно играть с уже установленными модами, "
                    + "но сервер может не пустить, если сборка устарела.";
            return;
        }

        // 2. Подпись Ed25519 — если не сошлась, ничего не скачиваем
        phase = Phase.VERIFYING;
        if (!verifySignature(manifestBytes, sigBytes)) {
            SyncLog.warn("Подпись манифеста НЕ совпала — обновление отменено");
            phase = Phase.SIGNATURE_BAD;
            detail = "Манифест мог быть подменён. Ничего не скачано. Сообщите администратору сервера.";
            return;
        }

        ManifestData manifest = ManifestData.parse(new String(manifestBytes, StandardCharsets.UTF_8));
        lastManifest = manifest;
        SyncLog.info("Манифест v" + manifest.version() + " проверен, модов: " + manifest.mods().size());
        if (!manifest.minecraft().isEmpty() && !manifest.minecraft().equals(gameVersion())) {
            SyncLog.warn("Манифест для Minecraft " + manifest.minecraft() + ", а запущена " + gameVersion());
        }
        for (ManifestData.ModEntry m : manifest.mods()) validateUrl(m.url());

        // 3. План изменений
        phase = Phase.PLANNING;
        Files.createDirectories(modsDir);
        List<ModsHasher.ModFile> present = ModsHasher.scan(modsDir);
        Map<String, String> installed = readInstalled();
        Plan plan = plan(manifest, present, installed);

        if (plan.downloads.isEmpty() && plan.deletes.isEmpty()) {
            writeInstalled(plan.newInstalled);
            if (!PendingOps.hasPending()) saveState(manifest);
            phase = PendingOps.hasPending() ? Phase.RESTART_REQUIRED : Phase.UP_TO_DATE;
            hadPending = PendingOps.hasPending();
            detail = "Сборка v" + manifest.version() + ", модов: " + manifest.mods().size();
            SyncLog.info("Всё актуально");
            return;
        }

        // 4. Скачивание во временную папку
        phase = Phase.DOWNLOADING;
        filesTotal = plan.downloads.size();
        bytesTotal = plan.downloads.stream().mapToLong(ManifestData.ModEntry::size).sum();
        Files.createDirectories(tmpDir);
        Map<ManifestData.ModEntry, Path> ready = new HashMap<>();
        for (ManifestData.ModEntry m : plan.downloads) {
            currentFile = m.name() + " " + m.version();
            try {
                ready.put(m, download(m));
            } catch (Exception e) {
                SyncLog.error("Не удалось скачать " + m.file(), e);
                phase = Phase.FAILED;
                detail = "Не удалось скачать " + m.name() + ": " + e.getMessage() + ". Ничего не изменено.";
                return;
            }
            filesDone++;
        }

        // 5. Транзакция замены файлов
        phase = Phase.APPLYING;
        currentFile = "";
        Transaction tx = new Transaction();
        try {
            for (Path del : plan.deletes) tx.delete(del);
            for (ManifestData.ModEntry m : plan.downloads) tx.install(ready.get(m), modsDir.resolve(m.file()));
            tx.commit();
        } catch (Exception e) {
            SyncLog.error("Ошибка установки, откатываю изменения", e);
            tx.rollback();
            phase = Phase.FAILED;
            detail = "Ошибка при установке: " + e.getMessage() + ". Все изменения отменены.";
            return;
        }

        writeInstalled(plan.newInstalled);
        hadPending = PendingOps.hasPending();
        if (!hadPending) saveState(manifest);
        changedCount = plan.downloads.size() + plan.deletes.size();
        phase = Phase.RESTART_REQUIRED;
        detail = "Изменено файлов: " + changedCount + (hadPending ? " (часть заменится при выходе из игры)" : "");
        SyncLog.info("Обновление завершено, " + detail);
    }

    // ------------------------------------------------------------------ план

    private record Plan(List<ManifestData.ModEntry> downloads, List<Path> deletes, Map<String, String> newInstalled) {}

    private Plan plan(ManifestData manifest, List<ModsHasher.ModFile> present, Map<String, String> installed) {
        Map<String, Path> bySha = new HashMap<>();
        for (ModsHasher.ModFile f : present) bySha.putIfAbsent(f.sha256(), f.path());
        Set<String> requiredShas = new HashSet<>();
        Set<String> requiredFiles = new HashSet<>();
        for (ManifestData.ModEntry m : manifest.mods()) {
            requiredShas.add(m.sha256());
            requiredFiles.add(m.file().toLowerCase(Locale.ROOT));
        }

        List<ManifestData.ModEntry> downloads = new ArrayList<>();
        Set<Path> deletes = new HashSet<>();
        Map<String, String> newInstalled = new HashMap<>();

        for (ManifestData.ModEntry m : manifest.mods()) {
            Path have = bySha.get(m.sha256());
            if (have != null) {
                newInstalled.put(m.id(), have.getFileName().toString());
            } else {
                downloads.add(m);
                newInstalled.put(m.id(), m.file());
            }
            // Старая версия этого же мода, которую ставили мы, под другим именем
            String old = installed.get(m.id());
            if (old != null && !old.equalsIgnoreCase(newInstalled.get(m.id()))) {
                Path oldPath = modsDir.resolve(old).normalize();
                if (oldPath.getParent().equals(modsDir.normalize()) && Files.exists(oldPath)
                        && !requiredFiles.contains(old.toLowerCase(Locale.ROOT))) {
                    String oldSha = shaOf(present, oldPath);
                    if (oldSha == null || !requiredShas.contains(oldSha)) deletes.add(oldPath);
                }
            }
        }

        // Явный список удаления из манифеста
        for (ManifestData.RemoveEntry r : manifest.remove()) {
            for (ModsHasher.ModFile f : present) {
                boolean byName = r.file() != null && f.path().getFileName().toString().equalsIgnoreCase(r.file());
                boolean bySh = r.sha256() != null && r.sha256().equals(f.sha256());
                if ((byName || bySh) && !requiredShas.contains(f.sha256())) deletes.add(f.path());
            }
        }
        List<Path> del = new ArrayList<>(deletes);
        // Файл, который всё равно перезапишем скачанным, удалять отдельно не нужно
        del.removeIf(p -> requiredFiles.contains(p.getFileName().toString().toLowerCase(Locale.ROOT)));
        return new Plan(downloads, del, newInstalled);
    }

    private static String shaOf(List<ModsHasher.ModFile> present, Path p) {
        for (ModsHasher.ModFile f : present) if (f.path().normalize().equals(p)) return f.sha256();
        return null;
    }

    // ------------------------------------------------------------------ сеть

    private void validateUrl(String url) {
        URI u = URI.create(url);
        if (!"https".equalsIgnoreCase(u.getScheme())) throw new IllegalArgumentException("не HTTPS: " + url);
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        if (!ModSyncConfig.DOWNLOAD_WHITELIST.contains(host)) {
            throw new IllegalArgumentException("домен не в белом списке: " + host);
        }
    }

    /** GET с ручной обработкой редиректов: каждый адрес проверяется по белому списку. */
    private HttpResponse<InputStream> open(String url) throws IOException, InterruptedException {
        String current = url;
        for (int i = 0; i < 6; i++) {
            validateUrl(current);
            HttpRequest req = HttpRequest.newBuilder(URI.create(current))
                    .timeout(Duration.ofSeconds(ModSyncConfig.REQUEST_TIMEOUT))
                    .header("User-Agent", "ModSync/" + modVersion())
                    .header("Cache-Control", "no-cache")
                    .GET().build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            int code = resp.statusCode();
            if (code >= 300 && code < 400) {
                String loc = resp.headers().firstValue("Location").orElseThrow(() -> new IOException("редирект без адреса"));
                resp.body().close();
                current = URI.create(current).resolve(loc).toString();
                continue;
            }
            if (code != 200) {
                resp.body().close();
                throw new IOException("HTTP " + code + " для " + current);
            }
            return resp;
        }
        throw new IOException("слишком много редиректов");
    }

    private byte[] fetchSmall(String url) throws IOException, InterruptedException {
        // Манифест и подпись берём только с домена манифеста
        try (InputStream in = open(url).body()) {
            byte[] data = in.readNBytes(4 * 1024 * 1024 + 1);
            if (data.length > 4 * 1024 * 1024) throw new IOException("файл слишком большой");
            return data;
        }
    }

    private boolean verifySignature(byte[] data, byte[] sigFile) {
        try {
            PublicKey key = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(ModSyncConfig.PUBLIC_KEY)));
            byte[] sig = Base64.getDecoder().decode(new String(sigFile, StandardCharsets.UTF_8).trim());
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(key);
            s.update(data);
            return s.verify(sig);
        } catch (Exception e) {
            SyncLog.error("Ошибка проверки подписи", e);
            return false;
        }
    }

    /** Скачивает мод во временный файл, проверяя размер и SHA-256. */
    private Path download(ManifestData.ModEntry m) throws Exception {
        if (m.size() > ModSyncConfig.MAX_MOD_SIZE) throw new IOException("слишком большой файл");
        Path part = tmpDir.resolve(m.file() + ".part");
        SyncLog.info("Скачиваю " + m.file() + " (" + m.size() + " байт) из " + m.url());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long written = 0;
        try (InputStream in = open(m.url()).body(); OutputStream out = Files.newOutputStream(part)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                written += n;
                if (written > m.size()) throw new IOException("файл больше заявленного размера");
                md.update(buf, 0, n);
                out.write(buf, 0, n);
                bytesDone += n;
            }
        } catch (Exception e) {
            Files.deleteIfExists(part);
            throw e;
        }
        if (written != m.size()) {
            Files.deleteIfExists(part);
            throw new IOException("размер не совпал: " + written + " вместо " + m.size());
        }
        String sha = HexFormat.of().formatHex(md.digest());
        if (!sha.equals(m.sha256())) {
            Files.deleteIfExists(part);
            throw new IOException("SHA-256 не совпал");
        }
        return part;
    }

    // ------------------------------------------------------------------ транзакция

    /**
     * Замена файлов с резервной копией. При любой ошибке (кроме блокировки файла)
     * всё уже сделанное откатывается. Заблокированные файлы откладываются до выхода из игры.
     */
    private final class Transaction {
        private record Done(Path target, Path backup) {}

        private final Path backupDir = baseDir.resolve("backup")
                .resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        private final List<Done> done = new ArrayList<>();
        private final List<String[]> pending = new ArrayList<>(); // {тип, src, dst}
        private final Path pendingDir = baseDir.resolve("pending");

        void delete(Path target) throws IOException {
            Files.createDirectories(backupDir);
            Path backup = backupDir.resolve(target.getFileName().toString());
            try {
                move(target, backup);
                done.add(new Done(target, backup));
                SyncLog.info("Удалён " + target.getFileName());
            } catch (IOException e) {
                if (!isLocked(e)) throw e;
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                pending.add(new String[]{"DELETE", null, target.toString()});
            }
        }

        void install(Path ready, Path target) throws IOException {
            Path backup = null;
            if (Files.exists(target)) {
                Files.createDirectories(backupDir);
                backup = backupDir.resolve(target.getFileName().toString());
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                move(ready, target); // атомарная замена
                done.add(new Done(target, backup));
                SyncLog.info("Установлен " + target.getFileName());
            } catch (IOException e) {
                if (!isLocked(e)) throw e;
                Files.createDirectories(pendingDir);
                Path staged = pendingDir.resolve(target.getFileName().toString());
                move(ready, staged);
                pending.add(new String[]{"REPLACE", staged.toString(), target.toString()});
            }
        }

        void commit() throws IOException {
            for (String[] p : pending) {
                if ("REPLACE".equals(p[0])) PendingOps.addReplace(Path.of(p[1]), Path.of(p[2]));
                else PendingOps.addDelete(Path.of(p[2]));
            }
            pruneBackups();
        }

        void rollback() {
            for (int i = done.size() - 1; i >= 0; i--) {
                Done d = done.get(i);
                try {
                    if (d.backup() != null && Files.exists(d.backup())) {
                        Files.copy(d.backup(), d.target(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(d.target());
                    }
                    SyncLog.info("Откат: " + d.target().getFileName());
                } catch (IOException e) {
                    SyncLog.error("Откат не удался для " + d.target(), e);
                }
            }
            for (String[] p : pending) {
                if (p[1] != null) {
                    try { Files.deleteIfExists(Path.of(p[1])); } catch (IOException ignored) { }
                }
            }
        }
    }

    private static void move(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Файл занят другим процессом (типично для Windows и загруженных jar). */
    private static boolean isLocked(IOException e) {
        if (e instanceof NoSuchFileException) return false;
        if (e instanceof AccessDeniedException) return true;
        return WINDOWS && e instanceof FileSystemException;
    }

    /** Оставляем только 3 последние резервные копии. */
    private void pruneBackups() {
        Path dir = baseDir.resolve("backup");
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> all = s.sorted(Comparator.reverseOrder()).toList();
            for (int i = 3; i < all.size(); i++) deleteTree(all.get(i));
        } catch (IOException ignored) {
        }
    }

    private void cleanupTmp() {
        deleteTree(tmpDir);
    }

    private static void deleteTree(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(x -> {
                try { Files.deleteIfExists(x); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) {
        }
    }

    // ------------------------------------------------------------------ состояние

    private Map<String, String> readInstalled() {
        try {
            if (!Files.exists(installedFile)) return new HashMap<>();
            JsonObject o = JsonParser.parseString(Files.readString(installedFile)).getAsJsonObject();
            Map<String, String> map = new HashMap<>();
            for (String k : o.keySet()) map.put(k, o.get(k).getAsString());
            return map;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void writeInstalled(Map<String, String> map) {
        try {
            Files.createDirectories(baseDir);
            Files.writeString(installedFile, GSON.toJson(map));
        } catch (IOException e) {
            SyncLog.error("Не удалось сохранить installed.json", e);
        }
    }

    private void saveState(ManifestData m) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("manifestVersion", m.version());
            o.addProperty("checkedAt", LocalDateTime.now().toString());
            com.google.gson.JsonArray opt = new com.google.gson.JsonArray();
            m.optional().forEach(opt::add);
            o.add("optional", opt);
            Files.createDirectories(baseDir);
            Files.writeString(stateFile, GSON.toJson(o));
        } catch (IOException e) {
            SyncLog.error("Не удалось сохранить state.json", e);
        }
    }
}
