package my.yeamu.modsync;

import my.yeamu.modsync.gui.GateOverlay;
import my.yeamu.modsync.gui.StatusBadge;
import my.yeamu.modsync.gui.SyncScreen;
import my.yeamu.modsync.net.HandshakeSender;
import my.yeamu.modsync.update.PendingOps;
import my.yeamu.modsync.update.UpdateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;

/**
 * Точка входа клиентского мода ModSync.
 */
public final class ModSyncClient implements ClientModInitializer {

    /** Показать окно синхронизации, как только откроется главное меню. */
    private static boolean showOnTitle = true;

    @Override
    public void onInitializeClient() {
        SyncLog.info("ModSync " + UpdateManager.modVersion() + " запускается");

        // Если прошлый мини-процесс не успел всё заменить — пробуем ещё раз
        PendingOps.retryLeftovers();

        // Проверка обновлений сразу при запуске игры, в фоновом потоке
        UpdateManager.get().startCheck();

        HandshakeSender.register();

        // Проверка сборки ModGate — пиксельная плашка поверх экрана вместо сообщений в чате
        HudElementRegistry.addLast(Identifier.of("modsync", "gate"), GateOverlay::render);
        ClientTickEvents.END_CLIENT_TICK.register(GateOverlay::tick);

        // Плашка-статус ModSync в главном меню (нажать — проверить обновления).
        // Место выбирается само на первом кадре, когда другие моды уже добавили свои кнопки, —
        // так плашка не прячется за чужими кнопками.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                Screens.getButtons(screen).add(new StatusBadge(screen, () -> {
                    UpdateManager.get().startCheck();
                    client.setScreen(new SyncScreen(screen));
                }));
            }
        });

        // Первое открытие главного меню: если идёт загрузка или есть что сказать — показываем окно
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!showOnTitle) return;
            if (client.currentScreen instanceof TitleScreen title) {
                showOnTitle = false;
                if (UpdateManager.get().shouldShowOnStartup()) {
                    client.setScreen(new SyncScreen(title));
                }
            }
        });

        // При выходе из игры запускаем мини-процесс для заблокированных файлов
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PendingOps.launchHelperIfNeeded());
        Runtime.getRuntime().addShutdownHook(new Thread(PendingOps::launchHelperIfNeeded, "ModSync-Shutdown"));
    }
}
