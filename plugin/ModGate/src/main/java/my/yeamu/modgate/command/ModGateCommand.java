package my.yeamu.modgate.command;

import my.yeamu.modgate.ModGatePlugin;
import my.yeamu.modgate.gate.HandshakeData;
import my.yeamu.modgate.gate.PlayerState;
import my.yeamu.modgate.manifest.Manifest;
import my.yeamu.modgate.manifest.ManifestService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * /modgate status [игрок] | list | reload | kick-outdated
 */
public final class ModGateCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final List<String> SUBS = List.of("status", "list", "reload", "kick-outdated");

    private final ModGatePlugin plugin;

    public ModGateCommand(ModGatePlugin plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String mini) {
        s.sendMessage(MM.deserialize(plugin.gateConfig().messages().prefix() + mini));
    }

    private static String esc(String s) {
        return MM.escapeTags(s == null ? "-" : s);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            msg(sender, "<gray>Использование: <white>/modgate <status|list|reload|kick-outdated>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "kick-outdated" -> {
                int n = plugin.gateManager().kickOutdated();
                msg(sender, "<yellow>Кикнуто игроков с устаревшей сборкой: <white>" + n);
            }
            default -> msg(sender, "<red>Неизвестная подкоманда.");
        }
        return true;
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Без ника — состояние самого плагина
            ManifestService ms = plugin.manifestService();
            Manifest m = ms.current();
            msg(sender, "<gray>Режим: <white>" + plugin.gateConfig().mode().name().toLowerCase(Locale.ROOT));
            if (m == null) {
                msg(sender, "<red>Манифест не загружен." + (ms.lastError() != null ? " Ошибка: " + esc(ms.lastError()) : ""));
            } else {
                msg(sender, "<gray>Манифест: <white>v" + m.version() + "</white>, ModSync <white>" + esc(m.modVersion())
                        + "</white> (мин. " + esc(m.minModVersion()) + "), модов: <white>" + m.mods().size()
                        + (ms.isFromCache() ? " <yellow>(из кэша)" : ""));
                msg(sender, "<gray>Ожидаемый хеш: <white>" + m.expectedHash().substring(0, 16) + "…");
            }
            if (ms.lastError() != null && m != null) msg(sender, "<yellow>Последняя ошибка загрузки: " + esc(ms.lastError()));
            msg(sender, "<gray>Подробно об игроке: <white>/modgate status <ник>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg(sender, "<red>Игрок не в сети.");
            return;
        }
        PlayerState st = plugin.gateManager().state(target.getUniqueId());
        if (st == null) {
            msg(sender, "<red>Нет данных об игроке.");
            return;
        }
        msg(sender, "<gold>" + esc(st.name()) + "</gold> <gray>— статус: " + statusColor(st) + st.status()
                + (st.reason() != null ? " <gray>(" + st.reason() + ")" : ""));
        HandshakeData hs = st.handshake();
        if (hs == null) {
            msg(sender, "<gray>Рукопожатие не получено — мод ModSync не установлен или не ответил.");
            return;
        }
        Manifest m = plugin.manifestService().current();
        msg(sender, "<gray>ModSync: <white>" + esc(hs.modVersion()) + "</white>, манифест: <white>v" + hs.manifestVersion()
                + "</white>, игра: <white>" + esc(hs.gameVersion()));
        msg(sender, "<gray>Хеш модов: <white>" + esc(shortHash(hs.modsHash()))
                + (m != null ? (m.expectedHash().equalsIgnoreCase(hs.modsHash()) ? " <green>✔ совпадает" : " <red>✘ не совпадает") : ""));
        if (!hs.files().isEmpty()) {
            msg(sender, "<gray>Установлено файлов: <white>" + hs.files().size());
            Set<String> have = new HashSet<>();
            for (HandshakeData.ModFile f : hs.files()) {
                have.add(f.sha256().toLowerCase(Locale.ROOT));
                boolean known = m == null || m.containsSha(f.sha256());
                sender.sendMessage(MM.deserialize((known ? " <green>✔ " : " <red>✘ ") + "<white>" + esc(f.fileName())
                        + (known ? "" : " <red>(не из списка)")));
            }
            if (m != null) {
                for (Manifest.ModEntry e : m.mods()) {
                    if (!have.contains(e.sha256())) {
                        sender.sendMessage(MM.deserialize(" <yellow>⚠ не хватает: <white>" + esc(e.name()) + " " + esc(e.version())));
                    }
                }
            }
        }
    }

    private void list(CommandSender sender) {
        var states = plugin.gateManager().states();
        if (states.isEmpty()) {
            msg(sender, "<gray>Никого нет в сети.");
            return;
        }
        msg(sender, "<gray>Игроки (" + states.size() + "):");
        for (PlayerState st : states) {
            HandshakeData hs = st.handshake();
            sender.sendMessage(MM.deserialize(" " + statusColor(st) + "● <white>" + esc(st.name()) + " <gray>— " + st.status()
                    + (st.reason() != null ? " / " + st.reason() : "")
                    + (hs != null ? " <dark_gray>[ModSync " + esc(hs.modVersion()) + ", v" + hs.manifestVersion() + "]" : "")));
        }
    }

    private void reload(CommandSender sender) {
        plugin.reloadEverything();
        msg(sender, "<gray>Конфиг перечитан, обновляю манифест…");
        plugin.manifestService().refreshThen(ok -> {
            Manifest m = plugin.manifestService().current();
            if (ok && m != null) msg(sender, "<green>Готово. Манифест v" + m.version() + ", ModSync " + esc(m.modVersion()));
            else msg(sender, "<yellow>Конфиг перечитан, но манифест обновить не удалось: "
                    + esc(plugin.manifestService().lastError() == null ? "уже обновляется" : plugin.manifestService().lastError()));
        });
    }

    private static String statusColor(PlayerState st) {
        return switch (st.status()) {
            case OK -> "<green>";
            case FAILED -> "<red>";
            case PENDING -> "<yellow>";
            default -> "<aqua>";
        };
    }

    private static String shortHash(String h) {
        return h == null ? "-" : (h.length() > 16 ? h.substring(0, 16) + "…" : h);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        }
        return out;
    }
}
