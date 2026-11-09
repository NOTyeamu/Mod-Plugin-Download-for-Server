package my.yeamu.pluginsync;

import my.yeamu.pixelui.paper.api.PixelIcon;
import my.yeamu.pixelui.paper.api.PixelNotification;
import my.yeamu.pixelui.paper.api.PixelPosition;
import my.yeamu.pixelui.paper.api.PixelTheme;
import my.yeamu.pixelui.paper.api.PixelUI;
import org.bukkit.entity.Player;

/**
 * Мост к PixelUI. Этот класс загружается ТОЛЬКО если плагин PixelUI включён,
 * поэтому без PixelUI PluginSync работает как обычно (только чат).
 */
final class PixelHook {

    private PixelHook() { }

    /** Показать пиксельное уведомление. kind: info | success | warning | error. */
    static boolean notify(Player p, String kind, String title, String text, long durationMs) {
        if (!PixelUI.hasMod(p)) return false; // без клиентского мода — пусть будет чат
        String icon = switch (kind) {
            case "success" -> PixelIcon.CHECK;
            case "warning" -> PixelIcon.CLOCK;
            case "error" -> PixelIcon.CROSS;
            default -> PixelIcon.INFO;
        };
        PixelUI.notify(p, PixelNotification.builder()
                .id("pluginsync")
                .title(title)
                .text(text)
                .icon(icon)
                .theme(PixelTheme.preset(kind))
                .position(PixelPosition.TOP_RIGHT)
                .duration(durationMs)
                .build());
        return true;
    }
}
