package my.yeamu.modsync.net;

import my.yeamu.modsync.SyncLog;
import my.yeamu.modsync.update.ModsHasher;
import my.yeamu.modsync.update.UpdateManager;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Отправка рукопожатия плагину ModGate.
 * Хеши файлов считаются в фоне, сам пакет отправляется в потоке клиента.
 */
public final class HandshakeSender {

    private static final ExecutorService HASHER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ModSync-Handshake");
        t.setDaemon(true);
        return t;
    });
    private static volatile long lastSent;

    private HandshakeSender() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerMessagePayload.ID, ServerMessagePayload.CODEC);

        // Сервер просит рукопожатие или сообщает итог
        ClientPlayNetworking.registerGlobalReceiver(ServerMessagePayload.ID, (payload, context) -> {
            if ("request".equals(payload.action())) {
                lastSent = 0; // на прямой запрос отвечаем всегда
                sendAsync(context.client());
            } else if ("result".equals(payload.action())) {
                SyncLog.info("ModGate: результат проверки — " + payload.text());
            }
        });

        // При входе на сервер
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendAsync(client));
        // Сервер объявил, что слушает наш канал
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(HandshakePayload.CHANNEL)) sendAsync(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSent = 0);
    }

    private static void sendAsync(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastSent < 2000) return; // защита от дублей
        lastSent = now;
        HASHER.execute(() -> {
            try {
                HandshakePayload payload = build();
                client.execute(() -> {
                    if (ClientPlayNetworking.canSend(HandshakePayload.ID)) {
                        ClientPlayNetworking.send(payload);
                        SyncLog.info("Рукопожатие отправлено: мод " + payload.modVersion() + ", манифест v"
                                + payload.manifestVersion() + ", хеш " + payload.modsHash().substring(0, 12));
                    } else {
                        lastSent = 0; // сервер ещё не готов — отправим по REGISTER или request
                    }
                });
            } catch (Exception e) {
                SyncLog.error("Не удалось подготовить рукопожатие", e);
            }
        });
    }

    private static HandshakePayload build() throws Exception {
        UpdateManager um = UpdateManager.get();
        List<ModsHasher.ModFile> mods = ModsHasher.scan(FabricLoader.getInstance().getGameDir().resolve("mods"));
        String hash = ModsHasher.listHash(mods, um.optionalShas());
        // Список файлов — для /modgate status. Ограничиваем, чтобы влезть в лимит пакета (32 КБ)
        List<String> files = new ArrayList<>();
        for (ModsHasher.ModFile f : mods) {
            if (files.size() >= 150) break;
            String name = f.path().getFileName().toString();
            if (name.length() > 80) name = name.substring(0, 80);
            files.add(f.sha256() + ":" + name);
        }
        return new HandshakePayload(UpdateManager.modVersion(), um.installedManifestVersion(), hash,
                UpdateManager.gameVersion(), files);
    }
}
