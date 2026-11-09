package my.yeamu.modsync.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Мини-процесс замены файлов. Запускается при выходе из игры отдельной JVM:
 *   java -cp modsync-helper.jar my.yeamu.modsync.helper.PendingApplier <папка игры> <pid игры>
 * Ждёт завершения игры, затем выполняет операции из .modsync/pending.txt.
 * Внимание: здесь нельзя использовать классы Minecraft/Fabric — только чистую Java.
 */
public final class PendingApplier {

    public static final String PENDING_FILE = "pending.txt";

    private PendingApplier() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            log("Использование: PendingApplier <gameDir> <pid>");
            return;
        }
        Path gameDir = Path.of(args[0]);
        Path pending = gameDir.resolve(".modsync").resolve(PENDING_FILE);
        long pid = Long.parseLong(args[1]);
        log("Старт, жду завершения игры (pid " + pid + ")");

        // Ждём, пока игра закроется (не более 2 минут)
        Optional<ProcessHandle> game = ProcessHandle.of(pid);
        if (game.isPresent()) {
            try {
                game.get().onExit().get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                log("Игра не закрылась за 2 минуты, пробую всё равно");
            }
        }
        sleep(1000); // ОС нужно время, чтобы снять блокировки файлов

        try {
            List<String> left = applyAll(pending, 30, PendingApplier::log);
            if (left.isEmpty()) {
                Files.deleteIfExists(pending);
                log("Все операции выполнены");
            } else {
                Files.write(pending, left, StandardCharsets.UTF_8);
                log("Не удалось выполнить " + left.size() + " операций, повторю при следующем запуске игры");
            }
        } catch (IOException e) {
            log("Ошибка: " + e);
        }
    }

    /**
     * Выполняет операции из файла. Возвращает строки, которые выполнить не удалось.
     * @param attempts сколько раз пробовать каждую операцию (с паузой 500 мс)
     */
    public static List<String> applyAll(Path pending, int attempts, Consumer<String> logger) throws IOException {
        List<String> left = new ArrayList<>();
        if (!Files.exists(pending)) return left;
        for (String line : Files.readAllLines(pending, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            String[] p = line.split("\t");
            boolean ok = false;
            for (int i = 0; i < attempts && !ok; i++) {
                try {
                    if ("REPLACE".equals(p[0]) && p.length == 3) {
                        Path src = Path.of(p[1]);
                        Path dst = Path.of(p[2]);
                        if (!Files.exists(src)) { ok = true; break; } // уже применено
                        move(src, dst);
                        logger.accept("Заменён " + dst.getFileName());
                    } else if ("DELETE".equals(p[0]) && p.length == 2) {
                        Files.deleteIfExists(Path.of(p[1]));
                        logger.accept("Удалён " + Path.of(p[1]).getFileName());
                    } else {
                        logger.accept("Пропущена непонятная строка: " + line);
                    }
                    ok = true;
                } catch (IOException e) {
                    if (i + 1 < attempts) sleep(500);
                    else logger.accept("Не получилось: " + line.replace('\t', ' ') + " — " + e.getMessage());
                }
            }
            if (!ok) left.add(line);
        }
        return left;
    }

    private static void move(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalDateTime.now() + "] " + msg);
    }
}
