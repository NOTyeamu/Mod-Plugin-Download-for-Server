package my.yeamu.modsync.update;

import my.yeamu.modsync.SyncLog;
import my.yeamu.modsync.helper.PendingApplier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Отложенные операции над файлами, которые нельзя было заменить, пока игра запущена
 * (на Windows загруженные jar заблокированы). Хранятся в .modsync/pending.txt
 * в простом текстовом формате, чтобы мини-процесс мог читать их без библиотек:
 *   REPLACE\t<готовый файл>\t<куда положить>
 *   DELETE\t<что удалить>
 */
public final class PendingOps {

    private static final AtomicBoolean LAUNCHED = new AtomicBoolean(false);

    private PendingOps() {}

    public static Path baseDir() {
        return FabricLoader.getInstance().getGameDir().resolve(".modsync");
    }

    public static Path file() {
        return baseDir().resolve(PendingApplier.PENDING_FILE);
    }

    public static synchronized void addReplace(Path ready, Path target) throws IOException {
        append("REPLACE\t" + ready.toAbsolutePath() + "\t" + target.toAbsolutePath());
    }

    public static synchronized void addDelete(Path target) throws IOException {
        append("DELETE\t" + target.toAbsolutePath());
    }

    private static void append(String line) throws IOException {
        Files.createDirectories(baseDir());
        Files.writeString(file(), line + System.lineSeparator(), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        SyncLog.info("Отложено до выхода из игры: " + line.replace('\t', ' '));
    }

    public static synchronized boolean hasPending() {
        try {
            return Files.exists(file()) && Files.size(file()) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized void clear() {
        try {
            Files.deleteIfExists(file());
        } catch (IOException ignored) {
        }
    }

    /** При запуске игры: то, что не успел сделать мини-процесс, делаем сами (если файлы уже не заняты). */
    public static synchronized void retryLeftovers() {
        if (!hasPending()) return;
        SyncLog.info("Найдены незавершённые отложенные операции, пробую применить");
        try {
            List<String> left = PendingApplier.applyAll(file(), 1, msg -> SyncLog.info("[pending] " + msg));
            if (left.isEmpty()) clear();
            else Files.write(file(), left, StandardCharsets.UTF_8);
        } catch (IOException e) {
            SyncLog.error("Ошибка при повторе отложенных операций", e);
        }
    }

    /**
     * Запускает отдельный мини-процесс, который дождётся закрытия игры и заменит файлы.
     * Наш jar копируется во временное место, чтобы процесс мог заменить и сам ModSync.
     */
    public static void launchHelperIfNeeded() {
        if (!hasPending() || !LAUNCHED.compareAndSet(false, true)) return;
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath();
            Optional<Path> origin = FabricLoader.getInstance().getModContainer("modsync")
                    .flatMap(c -> c.getOrigin().getPaths().stream().findFirst());
            if (origin.isEmpty()) {
                SyncLog.warn("Не удалось найти jar ModSync — отложенные операции выполнятся при следующем запуске");
                return;
            }
            Path classpath = origin.get();
            if (Files.isRegularFile(classpath)) {
                Path helperDir = baseDir().resolve("helper");
                Files.createDirectories(helperDir);
                Path copy = helperDir.resolve("modsync-helper.jar");
                Files.copy(classpath, copy, StandardCopyOption.REPLACE_EXISTING);
                classpath = copy;
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExecutable());
            cmd.add("-Xmx64m");
            cmd.add("-cp");
            cmd.add(classpath.toAbsolutePath().toString());
            cmd.add(PendingApplier.class.getName());
            cmd.add(gameDir.toString());
            cmd.add(String.valueOf(ProcessHandle.current().pid()));

            File log = baseDir().resolve("helper.log").toFile();
            new ProcessBuilder(cmd)
                    .directory(gameDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                    .start();
            SyncLog.info("Запущен мини-процесс замены файлов: " + String.join(" ", cmd));
        } catch (Exception e) {
            SyncLog.error("Не удалось запустить мини-процесс замены файлов", e);
        }
    }

    /** Путь к java текущей игры (на Windows — javaw, чтобы не мигало консольное окно). */
    private static String javaExecutable() {
        String cmd = ProcessHandle.current().info().command().orElse(null);
        if (cmd == null || !cmd.toLowerCase().contains("java")) {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            cmd = Path.of(System.getProperty("java.home"), "bin", win ? "javaw.exe" : "java").toString();
        }
        if (cmd.toLowerCase().endsWith("java.exe")) {
            Path javaw = Path.of(cmd).resolveSibling("javaw.exe");
            if (Files.exists(javaw)) cmd = javaw.toString();
        }
        return cmd;
    }
}
