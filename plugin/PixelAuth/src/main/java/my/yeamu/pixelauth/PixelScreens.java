package my.yeamu.pixelauth;

import my.yeamu.pixelui.paper.api.PixelIcon;
import my.yeamu.pixelui.paper.api.PixelInput;
import my.yeamu.pixelui.paper.api.PixelNotification;
import my.yeamu.pixelui.paper.api.PixelTheme;
import my.yeamu.pixelui.paper.api.PixelUI;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Мост к PixelUI. Загружается только если PixelUI установлен (вызывается из {@link Screens}
 * внутри try/catch), поэтому без PixelUI плагин не падает.
 */
final class PixelScreens {

    private PixelScreens() {
    }

    static boolean hasMod(Player p) {
        return PixelUI.hasMod(p);
    }

    static boolean hasInput(Player p, String id) {
        return PixelUI.hasInput(p, id);
    }

    /** Ошибка в открытом окне; поля с паролем очищаются. */
    static void error(Player p, String id, String raw) {
        PixelUI.inputError(p, id, raw, true);
    }

    static void close(Player p, String id) {
        if (PixelUI.hasInput(p, id)) PixelUI.closeInput(p, id);
    }

    /** Окно входа или регистрации. Закрыть нельзя (только кнопка «Выйти»), таймер идёт прямо в окне. */
    static void auth(PixelAuthPlugin plugin, Player p, boolean registered, String error, int seconds) {
        Msg m = plugin.msg();
        Settings s = plugin.settings();
        int max = Math.max(s.passwordMax, 128); // старые пароли из AuthMe могут быть длиннее
        PixelInput.Builder b = PixelInput.builder(AuthManager.AUTH_ID)
                .closeable(false)
                .closeOnSubmit(false)
                .timer(seconds)
                .width(260)
                .cancel(m.raw("ui-quit-button"))
                .error(error == null ? "" : error)
                .onCancel(pl -> plugin.auth().quitFromScreen(pl))
                .onClose(pl -> plugin.auth().screenClosed(pl));
        if (registered) {
            b.title(m.raw("ui-login-title"))
                    .lines(m.rawList("ui-login-lines", "name", p.getName()))
                    .icon(PixelIcon.LOCK)
                    .theme(PixelTheme.DARK)
                    .field(PixelInput.Field.password(m.raw("ui-password")).placeholder(m.raw("ui-placeholder")).maxLength(max))
                    .submit(m.raw("ui-login-button"))
                    .submitIcon(PixelIcon.KEY)
                    .onSubmit((pl, v) -> plugin.auth().attemptLogin(pl, get(v, 0)));
        } else {
            b.title(m.raw("ui-register-title"))
                    .lines(m.rawList("ui-register-lines", "name", p.getName(), "min", s.passwordMin))
                    .icon(PixelIcon.SHIELD)
                    .theme(PixelTheme.GOLD)
                    .field(PixelInput.Field.password(m.raw("ui-password")).placeholder(m.raw("ui-placeholder")).maxLength(s.passwordMax))
                    .field(PixelInput.Field.password(m.raw("ui-repeat")).placeholder(m.raw("ui-placeholder")).maxLength(s.passwordMax))
                    .submit(m.raw("ui-register-button"))
                    .submitIcon(PixelIcon.CHECK)
                    .onSubmit((pl, v) -> plugin.auth().attemptRegister(pl, get(v, 0), get(v, 1)));
        }
        PixelUI.input(p, b.build());
    }

    /** Окно смены пароля (можно закрыть). */
    static boolean change(PixelAuthPlugin plugin, Player p) {
        Msg m = plugin.msg();
        Settings s = plugin.settings();
        PixelInput in = PixelInput.builder(AuthManager.CHANGE_ID)
                .title(m.raw("ui-change-title"))
                .icon(PixelIcon.KEY)
                .theme(PixelTheme.INFO)
                .width(260)
                .closeable(true)
                .closeOnSubmit(false)
                .field(PixelInput.Field.password(m.raw("ui-old-password")).maxLength(128))
                .field(PixelInput.Field.password(m.raw("ui-new-password")).maxLength(s.passwordMax))
                .field(PixelInput.Field.password(m.raw("ui-repeat")).maxLength(s.passwordMax))
                .submit(m.raw("ui-change-button"))
                .submitIcon(PixelIcon.CHECK)
                .cancel(m.raw("ui-cancel-button"))
                .onSubmit((pl, v) -> plugin.auth().changePassword(pl, get(v, 0), get(v, 1), get(v, 2), true))
                .build();
        return PixelUI.input(p, in);
    }

    static void notify(Player p, String title, String text) {
        PixelNotification.builder()
                .title(title)
                .text(text)
                .icon(PixelIcon.UNLOCK)
                .theme(PixelTheme.SUCCESS)
                .duration(3500)
                .send(p);
    }

    private static String get(List<String> v, int i) {
        return v != null && i < v.size() && v.get(i) != null ? v.get(i) : "";
    }
}
