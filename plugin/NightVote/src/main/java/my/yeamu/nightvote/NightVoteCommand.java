package my.yeamu.nightvote;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /nightvote [start|cancel|status|reload] */
final class NightVoteCommand implements TabExecutor {

    private final NightVotePlugin plugin;
    private final VoteManager votes;

    NightVoteCommand(NightVotePlugin plugin, VoteManager votes) {
        this.plugin = plugin;
        this.votes = votes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "start" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                if (!(sender instanceof Player p)) {
                    plugin.send(sender, "only-player", "&cКоманда только для игроков.");
                    return true;
                }
                if (!p.hasPermission("nightvote.start")) {
                    plugin.send(p, "no-permission", "&cНет прав.");
                    return true;
                }
                if (!plugin.getConfig().getBoolean("trigger.command", true)) {
                    plugin.send(p, "command-disabled", "&cЗапуск командой отключён.");
                    return true;
                }
                votes.start(p.getWorld(), p, true);
            }
            case "cancel" -> {
                if (!sender.hasPermission("nightvote.admin")) {
                    plugin.send(sender, "no-permission", "&cНет прав.");
                    return true;
                }
                int n = votes.cancelAll();
                plugin.send(sender, "cancelled", "&eОтменено голосований: {n}", "n", n);
            }
            case "status" -> {
                List<String> lines = votes.status();
                if (lines.isEmpty()) plugin.send(sender, "status-none", "&7Сейчас голосования нет.");
                for (String l : lines) sender.sendMessage(NightVotePlugin.color(l));
            }
            case "reload" -> {
                if (!sender.hasPermission("nightvote.admin")) {
                    plugin.send(sender, "no-permission", "&cНет прав.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.send(sender, "reloaded", "&aКонфиг перезагружен.");
            }
            default -> plugin.send(sender, "usage", "&7/nightvote [start|status|cancel|reload]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> all = new ArrayList<>(List.of("start", "status"));
            if (sender.hasPermission("nightvote.admin")) all.addAll(List.of("cancel", "reload"));
            for (String s : all) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
