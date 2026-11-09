package my.yeamu.modsync.gui;

import my.yeamu.modsync.update.UpdateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Пиксельное окно «Проверка сборки не пройдена»: причина, отсчёт до отключения и кнопки.
 * Кнопки — обычные ButtonWidget (клики, клавиатура, звук), но поверх рисуется свой пиксельный вид.
 */
public class GateScreen extends Screen {

    private static final int W = 270;

    private final long opened = System.currentTimeMillis();
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private List<OrderedText> lines = List.of();
    private int px, py, ph;

    public GateScreen() {
        super(Text.literal("ModSync — проверка сборки"));
    }

    @Override
    protected void init() {
        buttons.clear();
        colors.clear();
        labels.clear();
        lines = textRenderer.wrapLines(StringVisitable.plain(GateOverlay.text()), W - 24);
        ph = 32 + lines.size() * 10 + 22 + 30;
        px = (width - W) / 2;
        py = (height - ph) / 2;

        int by = py + ph - 30;
        int bw = (W - 24 - 8) / 3;
        add(px + 12, by, bw, "Обновить моды", 0xFF3C8527, () -> {
            UpdateManager.get().startCheck();
            if (client != null) client.setScreen(new SyncScreen(this));
        });
        add(px + 12 + bw + 4, by, bw, "Открыть сайт", 0xFF2F6FB5,
                () -> ConfirmLinkScreen.open(this, GateOverlay.url()));
        add(px + 12 + (bw + 4) * 2, by, bw, GateOverlay.isWarn() ? "Понятно" : "Скрыть", 0xFF6B6B6B, this::close);
    }

    private void add(int x, int y, int w, String label, int color, Runnable action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> action.run()).dimensions(x, y, w, 20).build();
        addDrawableChild(b);
        buttons.add(b);
        colors.add(color);
        labels.add(label);
    }

    private float alpha() {
        return Math.min(1f, (System.currentTimeMillis() - opened) / 180f);
    }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        // Без размытия: просто затемнение и пиксельная панель
        c.fill(0, 0, width, height, Pixel.alpha(0xB0000000, alpha()));
        float a = alpha();
        Pixel.panel(c, px, py, W, ph, 0xF42A2A2A, 0xFF555555, 0xFF141414, a);
        c.fill(px + 4, py + 25, px + W - 4, py + 26, Pixel.alpha(0xFF141414, a));
        c.fill(px + 4, py + 26, px + W - 4, py + 27, Pixel.alpha(0xFF4A4A4A, a));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        float a = alpha();
        if (a < 0.1f) return;
        boolean warn = GateOverlay.isWarn();

        Pixel.icon(c, warn ? Pixel.WARN : Pixel.CROSS, px + 9, py + 5, 2, a);
        c.drawTextWithShadow(textRenderer, warn ? "Сборка не совпадает" : "Проверка сборки не пройдена",
                px + 32, py + 9, Pixel.alpha(warn ? 0xFFFFD83D : 0xFFFF6B6B, a));

        int y = py + 33;
        for (OrderedText line : lines) {
            c.drawText(textRenderer, line, px + 12, y, Pixel.alpha(0xFFE0E0E0, a), true);
            y += 10;
        }
        y += 6;
        int left = GateOverlay.secondsLeft();
        String info = warn ? "Играть можно, но лучше обновить моды."
                : left >= 0 ? "Отключение через " + left + " с" : "Скоро сервер отключит вас.";
        c.drawTextWithShadow(textRenderer, info, px + 12, y, Pixel.alpha(0xFFFFC83D, a));

        // Свои пиксельные кнопки поверх стандартных
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget b = buttons.get(i);
            Pixel.button(c, textRenderer, b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                    labels.get(i), colors.get(i), b.isSelected(), b.active, a);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
