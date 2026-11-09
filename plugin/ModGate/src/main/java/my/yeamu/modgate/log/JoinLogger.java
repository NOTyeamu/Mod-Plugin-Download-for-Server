package my.yeamu.modgate.log;

import my.yeamu.modgate.ModGatePlugin;
import my.yeamu.modgate.gate.HandshakeData;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Журнал входов plugins/ModGate/joins.log.
 * Запись идёт в отдельном потоке, чтобы не тормозить главный поток диском.
 */
public final class JoinLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ModGatePlugin plugin;
    private final Path file;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ModGate-JoinLog");
        t.setDaemon(true);
        return t;
    });
    private BufferedWriter writer; // используется только потоком io

    public JoinLogger(ModGatePlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("joins.log");
    }

    /** Добавляет строку в журнал. Данные собираем сейчас, пишем — в фоне. */
    public void log(Player player, String result, String reason, HandshakeData hs) {
        if (!plugin.gateConfig().logEnabled()) return;
        String line = String.format("%s | %-8s | %-16s | %s | %s | mod=%s manifest=%s hash=%s game=%s | ip=%s",
                LocalDateTime.now().format(TS), result, player.getName(), player.getUniqueId(), reason,
                hs != null ? hs.modVersion() : "-",
                hs != null ? hs.manifestVersion() : "-",
                hs != null ? shortHash(hs.modsHash()) : "-",
                hs != null ? hs.gameVersion() : "-",
                player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "?");
        plugin.getLogger().info("[вход] " + line.substring(22));
        io.execute(() -> write(line));
    }

    private static String shortHash(String h) {
        return h == null ? "-" : (h.length() > 12 ? h.substring(0, 12) : h);
    }

    private void write(String line) {
        try {
            if (writer == null) {
                Files.createDirectories(file.getParent());
                writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось записать joins.log: " + e.getMessage());
        }
    }

    /** Закрыть файл (откроется заново при следующей записи). */
    public void reopen() {
        io.execute(this::closeWriter);
    }

    private void closeWriter() {
        try {
            if (writer != null) writer.close();
        } catch (IOException ignored) {
        }
        writer = null;
    }

    public void close() {
        io.execute(this::closeWriter);
        io.shutdown();
        try {
            io.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
