package my.yeamu.pluginsync.command;

import my.yeamu.pluginsync.PluginManifest;
import my.yeamu.pluginsync.PluginSyncPlugin;
import my.yeamu.pluginsync.UpdateService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /pluginsync status | check | update | restart [сек] | cancel | discard | reload */
public final class PluginSyncCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("status", "check", "update", "restart", "cancel", "discard", "reload");

    private final PluginSyncPlugin plugin;

    public PluginSyncCommand(PluginSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> status(s);
            case "check" -> {
                if (plugin.runCheck(s, false)) plugin.msg(s, "&7Проверяю обновления…");
                else plugin.msg(s, "&eПроверка уже идёт, подождите");
            }
            case "update" -> {
                if (plugin.runCheck(s, true)) plugin.msg(s, "&7Проверяю и скачиваю…");
                else plugin.msg(s, "&eПроверка уже идёт, подождите");
            }
            case "restart" -> {
                int sec = plugin.syncConfig().restartCountdown();
                if (args.length > 1) {
                    try {
                        sec = Math.max(0, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        plugin.msg(s, "&cЧисло секунд: /" + label + " restart 60");
                        return true;
                    }
                }
                if (!plugin.updates().hasPending()) plugin.msg(s, "&7Скачанных обновлений нет, но перезапускаю по команде");
                plugin.restarts().start(sec, "команда от " + s.getName());
                plugin.msg(s, "&aПерезапуск через " + sec + " с &7(отмена: /" + label + " cancel)");
            }
            case "cancel" -> plugin.msg(s, plugin.restarts().cancel(true)
                    ? "&aПерезапуск отменён" : "&7Перезапуск не запланирован");
            case "discard" -> plugin.updates().discardPending().thenAccept(n ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.msg(s, "&aОтменено ожидающих обновлений: " + n)));
            case "reload" -> {
                plugin.reload();
                plugin.msg(s, "&aКонфиг PluginSync перезагружен &7(режим "
                        + plugin.syncConfig().mode().name().toLowerCase(Locale.ROOT) + ")");
            }
            default -> plugin.msg(s, "&e/" + label + " <status|check|update|restart [сек]|cancel|discard|reload>");
        }
        return true;
    }

    private void status(CommandSender s) {
        UpdateService u = plugin.updates();
        PluginManifest m = u.manifest();
        Instant last = u.lastCheck();
        plugin.msg(s, "&6&l■ PluginSync &7" + plugin.getPluginMeta().getVersion()
                + " &8| &7режим &f" + plugin.syncConfig().mode().name().toLowerCase(Locale.ROOT)
                + " &8| &7список &f" + (m == null ? "—" : "v" + m.version())
                + " &8| &7проверка &f" + (last == null ? "не было" : ago(last)));
        if (u.lastError() != null) plugin.msg(s, "&cПоследняя ошибка: &f" + u.lastError());
        if (u.states().isEmpty()) {
            plugin.msg(s, "&7Нет данных — выполните &f/pluginsync check");
        }
        for (UpdateService.PluginState st : u.states().values()) {
            String installed = st.installedVersion() == null ? "&8—" : "&f" + st.installedVersion();
            plugin.msg(s, " &8• &f" + st.entry().name() + " " + installed + " &8→ &f" + st.entry().version()
                    + "  " + st.status().label + (st.error() == null ? "" : " &8(" + st.error() + ")"));
        }
        if (u.hasPending()) plugin.msg(s, "&bЖдут перезапуска: &f" + String.join(", ", u.pendingNames()));
        if (plugin.restarts().isRunning()) plugin.msg(s, "&eПерезапуск через " + plugin.restarts().secondsLeft() + " с");
    }

    private static String ago(Instant t) {
        long sec = Duration.between(t, Instant.now()).getSeconds();
        if (sec < 60) return sec + " с назад";
        if (sec < 3600) return (sec / 60) + " мин назад";
        return (sec / 3600) + " ч назад";
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : SUBS) if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(sub);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("restart")) {
            out.addAll(List.of("10", "30", "60", "300"));
        }
        return out;
    }
}
