package my.yeamu.pixelui.api;

import my.yeamu.pixelui.hud.PixelHud;
import my.yeamu.pixelui.net.PixelNet;
import my.yeamu.pixelui.render.PixelIcons;
import my.yeamu.pixelui.screen.MenuScreen;
import net.minecraft.client.MinecraftClient;

/**
 * Публичный API для других клиентских модов. Можно вызывать из любого потока —
 * работа переносится в поток клиента.
 *
 * <pre>
 * PixelUI.notify(Notification.builder()
 *         .title("&aКвест выполнен")
 *         .text("Награда: &e50 монет")
 *         .icon("coin").theme(Theme.GOLD)
 *         .sound("minecraft:entity.player.levelup")
 *         .build());
 * </pre>
 */
public final class PixelUI {

    private PixelUI() {}

    public static void notify(Notification n) {
        run(() -> PixelHud.notify(n, false));
    }

    public static void notify(String title, String text) {
        notify(Notification.builder().title(title).text(text).build());
    }

    public static void banner(Banner b) {
        run(() -> PixelHud.banner(b, false));
    }

    public static void removeBanner(String id) {
        run(() -> PixelHud.removeBanner(id));
    }

    /** Открыть локальное меню. Нажатия приходят в listener, на сервер ничего не уходит. */
    public static void openMenu(Menu menu, Menu.Listener listener) {
        run(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof MenuScreen old) old.markReplaced();
            mc.setScreen(new MenuScreen(menu, false,
                    b -> { if (listener != null) listener.onClick(menu, b); },
                    () -> { if (listener != null) listener.onClose(menu); }));
        });
    }

    /** Закрыть открытое меню PixelUI (любое). */
    public static void closeMenu() {
        run(() -> {
            if (MinecraftClient.getInstance().currentScreen instanceof MenuScreen ms) ms.closeSilently();
        });
    }

    /** Убрать все уведомления и плашки. */
    public static void clear() {
        run(() -> PixelHud.clear(false));
    }

    /** Добавить свою пиксельную иконку (строки из символов палитры, см. PixelIcons). */
    public static void registerIcon(String name, String[] rows) {
        PixelIcons.register(name, rows);
    }

    /** Есть ли на текущем сервере плагин PixelUI. */
    public static boolean serverHasPlugin() {
        return PixelNet.serverHasPlugin();
    }

    private static void run(Runnable r) {
        MinecraftClient.getInstance().execute(r);
    }
}
