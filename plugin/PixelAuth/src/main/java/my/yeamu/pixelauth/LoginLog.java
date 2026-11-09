package my.yeamu.pixelauth;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/** Лог входов в plugins/PixelAuth/logins.log. Пишет в фоне, пароли никогда не попадают. */
final class LoginLog {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File file;
    private final Executor executor;
    private final Logger logger;
    private volatile boolean enabled;

    LoginLog(File folder, Executor executor, Logger logger, boolean enabled) {
        this.file = new File(folder, "logins.log");
        this.executor = executor;
        this.logger = logger;
        this.enabled = enabled;
    }

    void setEnabled(boolean v) {
        enabled = v;
    }

    /** @param event например LOGIN, REGISTER, SESSION, FAIL, KICK */
    void write(String name, String ip, String event, String details) {
        if (!enabled) return;
        String line = LocalDateTime.now().format(F) + " | " + name + " | " + ip + " | " + event
                + (details == null || details.isEmpty() ? "" : " | " + details) + System.lineSeparator();
        try {
            executor.execute(() -> {
                try {
                    Files.writeString(file.toPath(), line, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    logger.warning("Не удалось записать logins.log: " + e.getMessage());
                }
            });
        } catch (RuntimeException ignored) {
            // поток уже остановлен (выключение сервера)
        }
    }
}
