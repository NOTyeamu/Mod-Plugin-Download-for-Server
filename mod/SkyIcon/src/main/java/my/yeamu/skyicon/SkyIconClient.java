package my.yeamu.skyicon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SkyIcon: при смене дня и ночи вверху по центру на несколько секунд появляется
 * пиксельное солнце или луна. Всё считается на клиенте, сервер ничего не отправляет.
 */
public final class SkyIconClient implements ClientModInitializer {

    public static final Logger LOG = LoggerFactory.getLogger("SkyIcon");

    @Override
    public void onInitializeClient() {
        SkyConfig.load();
        HudElementRegistry.addLast(Identifier.of("skyicon", "icon"), SkyHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(SkyHud::tick);
        LOG.info("SkyIcon {} готов", FabricLoader.getInstance().getModContainer("skyicon")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0"));
    }
}
