package my.yeamu.modsync.gui;

import my.yeamu.modsync.update.UpdateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Окно синхронизации модов: этап, текущий файл, полоса прогресса.
 * Сама загрузка идёт в фоне — окно можно скрыть кнопкой.
 */
public class SyncScreen extends Screen {

    private final Screen parent;
    private UpdateManager.Phase shownPhase;
    /** Плавная полоса прогресса: отображаемое значение догоняет реальное. */
    private float smooth;

    public SyncScreen(Screen parent) {
        super(Text.literal("ModSync"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        UpdateManager um = UpdateManager.get();
        shownPhase = um.phase();
        int cx = this.width / 2;
        int y = this.height / 2 + 50;

        switch (shownPhase) {
            case OFFLINE, SIGNATURE_BAD, FAILED -> {
                addDrawableChild(ButtonWidget.builder(Text.literal("Повторить"), b -> um.startCheck())
                        .dimensions(cx - 154, y, 150, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("Играть с тем, что есть"), b -> close())
                        .dimensions(cx + 4, y, 150, 20).build());
            }
            case UP_TO_DATE, IDLE -> addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                    .dimensions(cx - 75, y, 150, 20).build());
            default -> addDrawableChild(ButtonWidget.builder(Text.literal("Скрыть (загрузка продолжится)"), b -> close())
                    .dimensions(cx - 100, y, 200, 20).build());
        }
    }

    @Override
    public void tick() {
        UpdateManager.Phase now = UpdateManager.get().phase();
        if (now == UpdateManager.Phase.RESTART_REQUIRED) {
            this.client.setScreen(new RestartScreen(parent));
            return;
        }
        if (now != shownPhase) clearAndInit(); // сменился этап — перестраиваем кнопки
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        UpdateManager um = UpdateManager.get();
        UpdateManager.Phase phase = um.phase();
        int cx = this.width / 2;
        int top = this.height / 2 - 60;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§aModSync §7" + UpdateManager.modVersion()), cx, top, 0xFFFFFFFF);
        int color = switch (phase) {
            case OFFLINE, FAILED -> 0xFFFFAA00;
            case SIGNATURE_BAD -> 0xFFFF5555;
            case UP_TO_DATE -> 0xFF55FF55;
            default -> 0xFFFFFFFF;
        };
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(phase.title), cx, top + 18, color);

        if (phase.active()) {
            float target = um.bytesTotal() > 0 ? (float) um.bytesDone() / um.bytesTotal() : 0f;
            if (phase == UpdateManager.Phase.APPLYING) target = 1f;
            smooth += (target - smooth) * Math.min(1f, delta * 0.25f + 0.05f);

            int w = Math.min(300, this.width - 40);
            int x0 = cx - w / 2;
            int y0 = top + 40;
            // Рамка и полоса в стиле ванильной загрузки
            context.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + 12, 0xFF000000);
            context.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 11, 0xFF8B8B8B);
            context.fill(x0, y0, x0 + w, y0 + 10, 0xFF373737);
            context.fill(x0, y0, x0 + (int) (w * Math.max(0f, Math.min(1f, smooth))), y0 + 10, 0xFF55C93F);

            String line = phase == UpdateManager.Phase.DOWNLOADING
                    ? um.filesDone() + "/" + um.filesTotal() + " • " + mb(um.bytesDone()) + " / " + mb(um.bytesTotal())
                    : "";
            if (!line.isEmpty()) context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), cx, y0 + 18, 0xFFAAAAAA);
            if (!um.currentFile().isEmpty())
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(um.currentFile()), cx, y0 + 30, 0xFFDDDDDD);
        } else if (!um.detail().isEmpty()) {
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(um.detail()), Math.min(320, this.width - 40));
            int y = top + 40;
            for (OrderedText l : lines) {
                context.drawCenteredTextWithShadow(this.textRenderer, l, cx, y, 0xFFDDDDDD);
                y += 11;
            }
        }
    }

    private static String mb(long bytes) {
        return String.format("%.1f МБ", bytes / 1024.0 / 1024.0);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
