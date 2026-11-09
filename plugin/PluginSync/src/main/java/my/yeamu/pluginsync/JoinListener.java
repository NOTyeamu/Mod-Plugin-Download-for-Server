package my.yeamu.pluginsync;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Напоминание админам при входе и перезапуск «когда сервер опустел». */
public final class JoinListener implements Listener {

    private final PluginSyncPlugin plugin;

    public JoinListener(PluginSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Игрок зашёл — автоперезапуск «пустого» сервера отменяем
        if (plugin.syncConfig().restartWhen() == SyncConfig.RestartWhen.EMPTY && plugin.restarts().isRunning()) {
            plugin.restarts().cancel(false);
        }
        if (!plugin.syncConfig().notifyAdminsOnJoin() || !p.hasPermission("pluginsync.admin")) return;
        if (!plugin.updates().hasPending()) return;
        // Чуть позже — чтобы клиентский PixelUI успел поздороваться
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            String list = String.join(", ", plugin.updates().pendingNames());
            plugin.notify(p, "info", "Есть обновления плагинов",
                    list + ". Поставятся при перезапуске.",
                    "&6[PluginSync] &eЖдут перезапуска: &f" + list + " &7(/pluginsync restart)");
        }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        // Во время события выходящий игрок ещё в списке — проверяем тиком позже
        Bukkit.getScheduler().runTaskLater(plugin, plugin::maybeRestart, 20L);
    }
}
