package my.yeamu.pluginsync;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

/** Перезапуск сервера с обратным отсчётом (всё — в главном потоке, через планировщик). */
public final class RestartManager {

    /** На каких секундах предупреждать игроков. */
    private static final Set<Integer> ANNOUNCE = Set.of(300, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1);

    private final PluginSyncPlugin plugin;
    private BukkitTask task;
    private int left;
    /** Последняя проверка перед перезапуском уже запущена. */
    private boolean finalCheck;
    private int waited;

    public RestartManager(PluginSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return task != null;
    }

    public int secondsLeft() {
        return left;
    }

    /** Запустить отсчёт. Если на сервере никого нет — перезапуск через пару секунд. */
    public void start(int seconds, String reason) {
        cancel(false);
        finalCheck = false;
        waited = 0;
        left = Bukkit.getOnlinePlayers().isEmpty() ? Math.min(seconds, 3) : seconds;
        plugin.getLogger().info("Перезапуск через " + left + " с: " + reason);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    private void tick() {
        if (left <= 0) {
            // Перед самым перезапуском — ещё одна проверка, чтобы в пачку попало всё свежее с сайта
            if (!finalCheck && plugin.syncConfig().mode() != SyncConfig.Mode.OFF) {
                finalCheck = true;
                plugin.updates().check(false);
                return;
            }
            // Ждём, пока докачается вся пачка (не дольше 2 минут)
            if (plugin.updates().isBusy() && waited++ < 120) return;
            task.cancel();
            task = null;
            doRestart();
            return;
        }
        if (ANNOUNCE.contains(left) || left % 60 == 0) {
            String text = "Сервер перезапустится через " + left + " с — ставим обновления";
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.notify(p, "warning", "Перезапуск", text, "&6[PluginSync] &e" + text);
            }
        }
        left--;
    }

    public boolean cancel(boolean announce) {
        if (task == null) return false;
        task.cancel();
        task = null;
        if (announce) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.notify(p, "success", "Перезапуск отменён", "Играем дальше!", "&6[PluginSync] &aПерезапуск отменён");
            }
        }
        return true;
    }

    private void doRestart() {
        String method = plugin.syncConfig().restartMethod();
        plugin.getLogger().info("Перезапуск для установки обновлений (" + method + ")");
        if ("stop".equals(method)) {
            Bukkit.shutdown();
        } else {
            // Нужен restart-script в spigot.yml (или автоперезапуск хостинга). Иначе это просто остановка.
            Bukkit.spigot().restart();
        }
    }
}
