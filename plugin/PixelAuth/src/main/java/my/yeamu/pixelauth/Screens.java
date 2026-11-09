package my.yeamu.pixelauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Всё, что видит игрок. Если на сервере есть PixelUI и у игрока стоит мод — пиксельные окна
 * (через {@link PixelScreens}). Иначе — обычный чат, титры и боссбар, как в AuthMe.
 * Этот класс не ссылается на классы PixelUI напрямую, поэтому работает и без него.
 */
final class Screens {

    private final PixelAuthPlugin plugin;
    private final boolean pixelAvailable;

    Screens(PixelAuthPlugin plugin) {
        this.plugin = plugin;
        boolean ok = false;
        if (Bukkit.getPluginManager().isPluginEnabled("PixelUI")) {
            try {
                // нужен PixelUI-Paper 1.1.3+ (в нём появилось окно ввода)
                Class.forName("my.yeamu.pixelui.paper.api.PixelInput", false, getClass().getClassLoader());
                ok = true;
            } catch (Throwable t) {
                plugin.getLogger().warning("PixelUI слишком старый (нужен 1.1.3+) — вход будет через чат.");
            }
        }
        pixelAvailable = ok;
    }

    boolean pixelAvailable() {
        return pixelAvailable;
    }

    /** У игрока стоит мод PixelUI и он уже поздоровался. */
    boolean pixel(Player p) {
        if (!pixelAvailable) return false;
        try {
            return PixelScreens.hasMod(p);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Показать экран входа/регистрации. error — текст ошибки или null. */
    void show(Player p, AuthManager.State st, String error) {
        boolean registered = st.account != null;
        boolean reset = registered && plugin.auth().resetReady(st);
        if (pixel(p)) {
            try {
                PixelScreens.auth(plugin, p, reset ? "reset" : registered ? "login" : "register", error,
                        plugin.auth().secondsLeft(st));
                return;
            } catch (Throwable t) {
                plugin.getLogger().log(Level.FINE, "PixelUI не смог открыть окно", t);
            }
        }
        if (reset) {
            p.sendMessage(Msg.color(plugin.msg().raw("prefix") + plugin.msg().raw("reset-approved-chat"))
                    .clickEvent(ClickEvent.suggestCommand("/register ")));
        } else {
            chatPrompt(p, registered, true);
        }
        if (error != null && !error.isEmpty()) p.sendMessage(Msg.color(plugin.msg().raw("prefix") + error));
    }

    /** Подсказка в чате (клик подставляет команду). title — ещё и титр по центру экрана. */
    void chatPrompt(Player p, boolean registered, boolean title) {
        Msg m = plugin.msg();
        String cmd = registered ? "/login " : "/register ";
        Component line = Msg.color(m.raw("prefix") + m.raw(registered ? "login-chat" : "register-chat"))
                .append(Component.space())
                .append(Msg.color(m.raw("click-hint")))
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Msg.color(m.raw("click-hint"))));
        p.sendMessage(line);
        if (title) {
            p.showTitle(Title.title(
                    m.comp(registered ? "login-title" : "register-title"),
                    m.comp(registered ? "login-subtitle" : "register-subtitle"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(plugin.settings().timeoutSeconds), Duration.ofMillis(300))));
            if (pixelAvailable) p.sendMessage(m.comp("mod-hint"));
            if (registered && plugin.settings().resetEnabled) {
                p.sendMessage(Msg.color(m.raw("reset-chat")).clickEvent(ClickEvent.runCommand("/forgotpassword"))
                        .hoverEvent(HoverEvent.showText(Component.text("/forgotpassword"))));
            }
        }
    }

    /** Ошибка: красная строка в открытом окне или сообщение в чат. */
    void error(Player p, String inputId, String raw) {
        if (pixel(p)) {
            try {
                if (PixelScreens.hasInput(p, inputId)) {
                    PixelScreens.error(p, inputId, raw);
                    return;
                }
            } catch (Throwable ignored) {
                // упадём в чат
            }
        }
        p.sendMessage(Msg.color(plugin.msg().raw("prefix") + raw));
    }

    /** Зелёная строка в открытом окне (поля не очищаются) или сообщение в чат. */
    void info(Player p, String inputId, String raw) {
        if (pixel(p)) {
            try {
                if (PixelScreens.hasInput(p, inputId)) {
                    PixelScreens.info(p, inputId, raw);
                    return;
                }
            } catch (Throwable ignored) {
                // упадём в чат
            }
        }
        p.sendMessage(Msg.color(plugin.msg().raw("prefix") + raw));
    }

    /** Новый запрос сброса: админам онлайн — строка с кнопками [Одобрить] / [Отклонить], в консоль — запись. */
    void notifyAdmins(String name, String ip, String lastIp) {
        Msg m = plugin.msg();
        boolean same = ip.equals(lastIp);
        String note = same ? m.raw("reset-admin-same-ip")
                : m.raw("reset-admin-other-ip", "old", lastIp == null ? "-" : lastIp, "ip", ip);
        String accept = "/pixelauth reset-accept " + name, deny = "/pixelauth reset-deny " + name;
        Component line = Msg.color(m.raw("prefix") + m.raw("reset-admin", "name", name, "ipnote", note))
                .append(Component.space())
                .append(Msg.color(m.raw("reset-admin-accept")).clickEvent(ClickEvent.runCommand(accept))
                        .hoverEvent(HoverEvent.showText(Component.text(accept))))
                .append(Component.space())
                .append(Msg.color(m.raw("reset-admin-deny")).clickEvent(ClickEvent.runCommand(deny))
                        .hoverEvent(HoverEvent.showText(Component.text(deny))));
        for (Player a : Bukkit.getOnlinePlayers()) {
            if (!a.hasPermission("pixelauth.resets") || !plugin.auth().isAuthed(a)) continue;
            a.sendMessage(line);
            if (pixel(a)) {
                try {
                    PixelScreens.adminNotify(a, name);
                } catch (Throwable ignored) {
                    // чата достаточно
                }
            }
        }
        plugin.getLogger().info("Запрос сброса пароля: " + name + " (IP " + ip
                + (same ? ", совпадает с последним входом" : ", последний вход был с " + (lastIp == null ? "-" : lastIp))
                + "). Одобрить: " + accept + ", отклонить: " + deny);
    }

    void closeAuth(Player p) {
        close(p, AuthManager.AUTH_ID);
        p.clearTitle();
    }

    void closeChange(Player p) {
        close(p, AuthManager.CHANGE_ID);
    }

    private void close(Player p, String id) {
        if (!pixelAvailable) return;
        try {
            PixelScreens.close(p, id);
        } catch (Throwable ignored) {
            // нечего закрывать
        }
    }

    boolean openChange(Player p) {
        if (!pixel(p)) return false;
        try {
            return PixelScreens.change(plugin, p);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Успех: пиксельное уведомление или титр + чат. textKey может быть пустым. */
    void success(Player p, String titleKey, String textKey) {
        Msg m = plugin.msg();
        String title = m.raw(titleKey, "name", p.getName());
        String text = textKey.isEmpty() ? "" : m.raw(textKey, "name", p.getName());
        if (pixel(p)) {
            try {
                PixelScreens.notify(p, title, text);
                return;
            } catch (Throwable ignored) {
                // упадём в чат
            }
        }
        if (!text.isEmpty()) {
            p.showTitle(Title.title(Msg.color(title), Msg.color(text),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400))));
        }
        p.sendMessage(Msg.color(m.raw("prefix") + title + (text.isEmpty() ? "" : " " + text)));
    }
}
