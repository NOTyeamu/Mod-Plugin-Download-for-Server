package my.yeamu.pixelauth.api;

import my.yeamu.pixelauth.PixelAuthPlugin;
import org.bukkit.entity.Player;

/**
 * Простое API для других плагинов (добавьте PixelAuth в softdepend).
 * <pre>
 * if (!PixelAuthAPI.isAuthenticated(player)) return; // ещё не вошёл
 * </pre>
 * Вызывать из главного потока. События: {@link PixelAuthLoginEvent}, {@link PixelAuthLogoutEvent}.
 */
public final class PixelAuthAPI {

    private PixelAuthAPI() {
    }

    /** Игрок вошёл (или проверка для него не требуется: NPC, пассивный режим). */
    public static boolean isAuthenticated(Player player) {
        PixelAuthPlugin p = PixelAuthPlugin.instance();
        return p == null || p.auth() == null || p.auth().isAuthed(player);
    }

    /** Зарегистрирован ли игрок, сидящий сейчас на сервере. */
    public static boolean isRegistered(Player player) {
        PixelAuthPlugin p = PixelAuthPlugin.instance();
        return p != null && p.auth() != null && p.auth().isRegisteredCached(player);
    }

    /** Пустить без пароля. @return false, если уже вошёл или аккаунт ещё загружается */
    public static boolean forceLogin(Player player) {
        PixelAuthPlugin p = PixelAuthPlugin.instance();
        return p != null && p.auth() != null && !p.passive() && p.auth().forceLogin(player);
    }

    /** Выкинуть из аккаунта (как /logout). */
    public static void logout(Player player) {
        PixelAuthPlugin p = PixelAuthPlugin.instance();
        if (p != null && p.auth() != null && !p.passive()) p.auth().logout(player);
    }
}
