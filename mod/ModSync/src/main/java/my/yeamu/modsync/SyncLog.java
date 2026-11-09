package my.yeamu.modsync;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Лог мода: пишет и в общий лог игры, и в отдельный файл logs/modsync.log.
 */
public final class SyncLog {

    private static final Logger LOG = LoggerFactory.getLogger("ModSync");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static BufferedWriter writer;

    private SyncLog() {}

    public static void info(String msg) { LOG.info(msg); write("INFO", msg); }
    public static void warn(String msg) { LOG.warn(msg); write("WARN", msg); }

    public static void error(String msg, Throwable t) {
        LOG.error(msg, t);
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        write("ERROR", msg + (t != null ? "\n" + sw : ""));
    }

    private static synchronized void write(String level, String msg) {
        try {
            if (writer == null) {
                Path file = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("modsync.log");
                Files.createDirectories(file.getParent());
                // Не даём логу разрастаться бесконечно
                if (Files.exists(file) && Files.size(file) > 2L * 1024 * 1024) {
                    Files.move(file, file.resolveSibling("modsync.old.log"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            writer.write("[" + LocalDateTime.now().format(TS) + "] [" + level + "] [" + Thread.currentThread().getName() + "] " + msg);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOG.warn("Не удалось записать modsync.log: {}", e.getMessage());
        }
    }
}
