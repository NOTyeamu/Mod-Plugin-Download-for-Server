package my.yeamu.modsync.gui;

import my.yeamu.modsync.update.UpdateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Экран «Нужен перезапуск игры» после обновления модов.
 */
public class RestartScreen extends Screen {

    private final Screen parent;

    public RestartScreen(Screen parent) {
        super(Text.literal("Нужен перезапуск игры"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 40;
        addDrawableChild(ButtonWidget.builder(Text.literal("Выйти из игры"), b -> this.client.scheduleStop())
                .dimensions(cx - 154, y, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Позже"), b -> close())
                .dimensions(cx + 4, y, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e§lНужен перезапуск игры"), cx, top, 0xFFFFFFFF);
        UpdateManager um = UpdateManager.get();
        String text = "Моды сервера обновлены. Перезапустите игру, чтобы изменения вступили в силу. "
                + um.detail()
                + (um.hadPending() ? " Заблокированные файлы будут заменены автоматически после закрытия игры." : "");
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(text), Math.min(320, this.width - 40));
        int y = top + 20;
        for (OrderedText l : lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, l, cx, y, 0xFFDDDDDD);
            y += 11;
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
