package my.yeamu.pixelauth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** /login, /register, /logout, /endsession, /changepassword. Таб-дополнение пустое — чтобы не подсказывать ники. */
final class AuthCommands implements TabExecutor {

    private final PixelAuthPlugin plugin;

    AuthCommands(PixelAuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Команда только для игроков. Для консоли есть /pixelauth.");
            return true;
        }
        if (plugin.passive()) {
            p.sendMessage("PixelAuth в пассивном режиме (стоит AuthMe).");
            return true;
        }
        AuthManager auth = plugin.auth();
        switch (cmd.getName().toLowerCase()) {
            case "login" -> {
                if (args.length < 1) {
                    plugin.msg().send(p, "login-usage");
                    return true;
                }
                auth.attemptLogin(p, String.join(" ", args));
            }
            case "register" -> {
                if (args.length < 2) {
                    plugin.msg().send(p, "register-usage");
                    return true;
                }
                auth.attemptRegister(p, args[0], args[1]);
            }
            case "logout" -> auth.logout(p);
            case "endsession" -> auth.endSession(p);
            case "forgotpassword" -> auth.requestReset(p);
            case "changepassword" -> {
                if (args.length < 2) {
                    // без аргументов — открываем пиксельное окно, если можно
                    if (!auth.isAuthed(p)) plugin.msg().send(p, "need-auth");
                    else if (!auth.openChangeScreen(p)) plugin.msg().send(p, "changepass-usage");
                    return true;
                }
                auth.changePassword(p, args[0], args[1], null, false);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
