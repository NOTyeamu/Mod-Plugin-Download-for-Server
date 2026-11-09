package my.yeamu.pixelui.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import my.yeamu.pixelui.PixelUIClient;
import my.yeamu.pixelui.api.Banner;
import my.yeamu.pixelui.api.Menu;
import my.yeamu.pixelui.api.Notification;
import my.yeamu.pixelui.hud.PixelHud;
import my.yeamu.pixelui.screen.MenuScreen;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;

/**
 * Сеть PixelUI: принимаем команды от плагина и отсылаем нажатия.
 *
 * Сервер → клиент: notify, banner, banner_remove, menu, menu_close, clear.
 * Клиент → сервер: hello {version}, click {menu, button}, close {menu}.
 */
public final class PixelNet {

    private static boolean helloSent;
    private static boolean serverReady;

    private PixelNet() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(PixelPayload.ID, PixelPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PixelPayload.ID, PixelPayload.CODEC);

        // Обработчик уже вызывается в потоке клиента
        ClientPlayNetworking.registerGlobalReceiver(PixelPayload.ID, (payload, context) -> {
            serverReady = true;
            handle(context.client(), payload.json());
        });

        // Сервер объявил, что слушает наш канал — здороваемся, чтобы плагин знал о моде
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(PixelPayload.CHANNEL)) client.execute(PixelNet::hello);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(PixelNet::hello));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            helloSent = false;
            serverReady = false;
            client.execute(() -> PixelHud.clear(true));
        });
    }

    /** true, если на текущем сервере есть плагин PixelUI. */
    public static boolean serverHasPlugin() {
        return serverReady || ClientPlayNetworking.canSend(PixelPayload.ID);
    }

    private static void hello() {
        if (helloSent || !ClientPlayNetworking.canSend(PixelPayload.ID)) return;
        helloSent = true;
        JsonObject o = new JsonObject();
        o.addProperty("type", "hello");
        o.addProperty("version", PixelUIClient.version());
        send(o);
    }

    public static void sendClick(String menu, String button) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "click");
        o.addProperty("menu", menu);
        o.addProperty("button", button);
        send(o);
    }

    public static void sendClose(String menu) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "close");
        o.addProperty("menu", menu);
        send(o);
    }

    private static void send(JsonObject o) {
        if (ClientPlayNetworking.canSend(PixelPayload.ID)) {
            ClientPlayNetworking.send(new PixelPayload(o.toString()));
        }
    }

    private static void handle(MinecraftClient client, String json) {
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            PixelUIClient.LOG.warn("PixelUI: неверный пакет от сервера: {}", e.getMessage());
            return;
        }
        String type = o.has("type") ? o.get("type").getAsString() : "";
        try {
            switch (type) {
                case "notify" -> PixelHud.notify(Notification.fromJson(o), true);
                case "banner" -> PixelHud.banner(Banner.fromJson(o), true);
                case "banner_remove" -> PixelHud.removeBanner(o.has("id") ? o.get("id").getAsString() : null);
                case "clear" -> PixelHud.clear(true);
                case "menu" -> {
                    Menu m = Menu.fromJson(o);
                    if (client.currentScreen instanceof MenuScreen old) old.markReplaced();
                    client.setScreen(new MenuScreen(m, true,
                            b -> sendClick(m.id, b.id()),
                            () -> sendClose(m.id)));
                }
                case "menu_close" -> {
                    if (client.currentScreen instanceof MenuScreen ms && ms.fromServer
                            && (!o.has("id") || ms.menu().id.equals(o.get("id").getAsString()))) {
                        ms.closeSilently();
                    }
                }
                default -> PixelUIClient.LOG.debug("PixelUI: неизвестный тип пакета {}", type);
            }
        } catch (Exception e) {
            PixelUIClient.LOG.warn("PixelUI: ошибка обработки «{}»: {}", type, e.toString());
        }
    }
}
