package my.yeamu.pixelauth;

import my.yeamu.pixelui.paper.api.event.PixelClientReadyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Регистрируется только при наличии PixelUI: мод игрока подключился — показываем пиксельное окно. */
final class PixelHookListener implements Listener {

    private final PixelAuthPlugin plugin;

    PixelHookListener(PixelAuthPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onReady(PixelClientReadyEvent e) {
        plugin.auth().clientReady(e.getPlayer());
    }
}
