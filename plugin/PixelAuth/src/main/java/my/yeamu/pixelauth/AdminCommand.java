package my.yeamu.pixelauth;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * /pixelauth — админ-команды. Все обращения к базе и хеширование идут в фоне,
 * ответ отправляется из главного потока.
 */
final class AdminCommand implements TabExecutor {

    private static final List<String> SUB = List.of("register", "unregister", "changepass", "forcelogin", "info",
            "import-authme", "stats", "reload");
    private final PixelAuthPlugin plugin;

    AdminCommand(PixelAuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            help(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "register" -> {
                if (a.length < 3) { s.sendMessage("§c/pixelauth register <ник> <пароль>"); return true; }
                String name = a[1], pw = a[2];
                int iter = plugin.settings().hashIterations;
                plugin.storage().executor().execute(() -> {
                    String hash = Passwords.hash(pw, iter);
                    long now = System.currentTimeMillis();
                    Account acc = new Account(name.toLowerCase(Locale.ROOT), name, null, hash, null, now, null, 0);
                    try {
                        boolean ok = plugin.storage().insert(acc);
                        reply(s, ok ? "§aАккаунт " + name + " создан." : "§cАккаунт " + name + " уже существует.");
                        if (ok) sync(() -> plugin.auth().accountChanged(name, acc));
                    } catch (SQLException e) {
                        fail(s, e);
                    }
                });
            }
            case "unregister" -> {
                if (a.length < 2) { s.sendMessage("§c/pixelauth unregister <ник>"); return true; }
                String name = a[1];
                plugin.storage().executor().execute(() -> {
                    try {
                        boolean ok = plugin.storage().delete(name);
                        reply(s, ok ? "§aАккаунт " + name + " удалён." : "§cАккаунт " + name + " не найден.");
                        if (ok) sync(() -> plugin.auth().accountDeleted(name));
                    } catch (SQLException e) {
                        fail(s, e);
                    }
                });
            }
            case "changepass" -> {
                if (a.length < 3) { s.sendMessage("§c/pixelauth changepass <ник> <новый пароль>"); return true; }
                String name = a[1], pw = a[2];
                int iter = plugin.settings().hashIterations;
                plugin.storage().executor().execute(() -> {
                    try {
                        Account acc = plugin.storage().find(name);
                        if (acc == null) { reply(s, "§cАккаунт " + name + " не найден."); return; }
                        String hash = Passwords.hash(pw, iter);
                        plugin.storage().updateHash(name, hash);
                        Account changed = acc.withHash(hash);
                        sync(() -> plugin.auth().accountChanged(name, changed));
                        reply(s, "§aПароль " + name + " изменён.");
                    } catch (SQLException e) {
                        fail(s, e);
                    }
                });
            }
            case "forcelogin" -> {
                if (a.length < 2) { s.sendMessage("§c/pixelauth forcelogin <игрок>"); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) { s.sendMessage("§cИгрок не в сети."); return true; }
                s.sendMessage(plugin.auth().forceLogin(p) ? "§a" + p.getName() + " вошёл." : "§e" + p.getName() + " уже вошёл или ещё загружается.");
            }
            case "info" -> {
                if (a.length < 2) { s.sendMessage("§c/pixelauth info <ник>"); return true; }
                String name = a[1];
                plugin.storage().executor().execute(() -> {
                    try {
                        Account acc = plugin.storage().find(name);
                        if (acc == null) { reply(s, "§cАккаунт " + name + " не найден."); return; }
                        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                        reply(s, "§6── " + acc.realName() + " ──"
                                + "\n§7UUID: §f" + (acc.uuid() == null ? "-" : acc.uuid())
                                + "\n§7Регистрация: §f" + f.format(new Date(acc.regDate())) + " §8(" + nz(acc.regIp()) + ")"
                                + "\n§7Последний вход: §f" + (acc.lastLogin() > 0 ? f.format(new Date(acc.lastLogin())) : "-")
                                + " §8(" + nz(acc.lastIp()) + ")"
                                + "\n§7Хеш: §f" + Passwords.format(acc.hash()));
                    } catch (SQLException e) {
                        fail(s, e);
                    }
                });
            }
            case "import-authme" -> {
                String file = a.length > 1 ? a[1] : "authme.db";
                String table = a.length > 2 ? a[2] : "authme";
                s.sendMessage("§eИмпорт из AuthMe/" + file + "…");
                plugin.storage().executor().execute(() -> {
                    try {
                        AuthMeImporter.Result r = AuthMeImporter.run(plugin.getDataFolder().getParentFile(), file, table, plugin.storage());
                        reply(s, "§aГотово: прочитано " + r.read() + ", добавлено " + r.added()
                                + " (уже были: " + (r.read() - r.added()) + ")."
                                + (r.unsupported() > 0 ? "\n§e" + r.unsupported() + " аккаунтов с BCrypt/неизвестным хешем — им нужен сброс пароля." : ""));
                    } catch (SQLException e) {
                        fail(s, e);
                    }
                });
            }
            case "stats" -> plugin.storage().executor().execute(() -> {
                try {
                    int total = plugin.storage().count();
                    long online = Bukkit.getOnlinePlayers().size();
                    reply(s, "§6PixelAuth: §f" + total + "§7 аккаунтов, §f" + online + "§7 онлайн, PixelUI: "
                            + (plugin.screens().pixelAvailable() ? "§aда" : "§cнет") + "§7, база: §f" + plugin.settings().storageType);
                } catch (SQLException e) {
                    fail(s, e);
                }
            });
            case "reload" -> {
                plugin.reloadSettings();
                s.sendMessage("§aКонфиг PixelAuth перезагружен (тип базы меняется только после рестарта).");
            }
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage("§6PixelAuth " + plugin.getPluginMeta().getVersion()
                + "\n§e/pixelauth register <ник> <пароль>"
                + "\n§e/pixelauth unregister <ник>"
                + "\n§e/pixelauth changepass <ник> <пароль>"
                + "\n§e/pixelauth forcelogin <игрок>"
                + "\n§e/pixelauth info <ник>"
                + "\n§e/pixelauth import-authme [файл] [таблица]"
                + "\n§e/pixelauth stats"
                + "\n§e/pixelauth reload");
    }

    private static String nz(String v) {
        return v == null ? "-" : v;
    }

    private void reply(CommandSender s, String text) {
        sync(() -> s.sendMessage(text));
    }

    private void fail(CommandSender s, Exception e) {
        plugin.getLogger().log(Level.WARNING, "Ошибка команды /pixelauth", e);
        reply(s, "§cОшибка: " + e.getMessage());
    }

    private void sync(Runnable r) {
        if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, r);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String sub : SUB) if (sub.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(sub);
        } else if (a.length == 2 && !a[0].equalsIgnoreCase("import-authme") && !a[0].equalsIgnoreCase("stats")
                && !a[0].equalsIgnoreCase("reload")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
        }
        return out;
    }
}
