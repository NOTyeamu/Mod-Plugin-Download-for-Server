package my.yeamu.pixelui;

import my.yeamu.pixelui.hud.PixelHud;
import my.yeamu.pixelui.net.PixelNet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа PixelUI. Регистрирует слой HUD (уведомления и плашки) и сетевой канал.
 * Другие моды вызывают my.yeamu.pixelui.api.PixelUI, плагины — через PixelUI-Paper.
 */
public final class PixelUIClient implements ClientModInitializer {

    public static final Logger LOG = LoggerFactory.getLogger("PixelUI");

    public static String version() {
        return FabricLoader.getInstance().getModContainer("pixelui")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0");
    }

    @Override
    public void onInitializeClient() {
        PixelNet.register();
        // Поверх всего остального HUD
        HudElementRegistry.addLast(Identifier.of("pixelui", "overlay"), PixelHud::render);
        LOG.info("PixelUI {} готов", version());
    }
}
