package my.yeamu.modsync.gui;

import my.yeamu.modsync.update.UpdateManager;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Компактная пиксельная плашка ModSync в главном меню вместо большой кнопки.
 * Показывает версию мода и состояние (актуально / проверяю / нужен перезапуск / сайт недоступен),
 * по нажатию открывает окно проверки обновлений.
 *
 * Место выбирается само на первом кадре: перебираем углы и края экрана и берём первое,
 * где плашка не пересекается ни с одной кнопкой (в том числе кнопками других модов).
 * При смене размера окна место пересчитывается.
 */
public final class StatusBadge extends ClickableWidget {

    private static final int H = 24, MARGIN = 4;
    private static final String[] STATUSES = {
            "Нажмите для проверки", "Проверяю…", "Скачиваю 99/99", "Моды актуальны",
            "Нужен перезапуск", "Сайт недоступен", "Ошибка подписи", "Ошибка обновления"
    };

    private final Screen screen;
    private final Runnable action;
    private final String title;
    private int placedForW = -1, placedForH = -1;

    public StatusBadge(Screen screen, Runnable action) {
        super(MARGIN, MARGIN, 100, H, Text.literal("ModSync"));
        this.screen = screen;
        this.action = action;
        this.title = "ModSync " + UpdateManager.modVersion();
        // Ширина фиксированная (по самой длинной надписи), чтобы плашка не «прыгала» при смене статуса
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int textW = tr.getWidth(title);
        for (String s : STATUSES) textW = Math.max(textW, tr.getWidth(s));
        setWidth(19 + textW + 6);
        setTooltip(Tooltip.of(Text.literal("ModSync — нажмите, чтобы проверить обновления модов сервера")));
    }

    // ------------------------------------------------------------------ выбор места

    private void place() {
        int sw = screen.width, sh = screen.height;
        if (sw == placedForW && sh == placedForH) return;
        placedForW = sw;
        placedForH = sh;
        int w = getWidth();
        int right = sw - w - MARGIN;
        int[][] spots = {
                {MARGIN, MARGIN},                     // левый верхний угол
                {right, MARGIN},                      // правый верхний угол
                {MARGIN, MARGIN + H + 2},             // под левым верхним
                {right, MARGIN + H + 2},              // под правым верхним
                {MARGIN, sh - 12 - H - MARGIN},       // слева снизу, над версией игры
                {right, sh - 12 - H - MARGIN},        // справа снизу, над копирайтом
                {MARGIN, (sh - H) / 2},               // левый край по центру
                {right, (sh - H) / 2},                // правый край по центру
        };
        List<ClickableWidget> others = Screens.getButtons(screen);
        for (int[] s : spots) {
            if (free(others, s[0], s[1], w)) {
                setX(s[0]);
                setY(s[1]);
                return;
            }
        }
        setX(spots[1][0]); // всё занято — правый верхний угол (в ванилле он пустой)
        setY(spots[1][1]);
    }

    private boolean free(List<ClickableWidget> others, int x, int y, int w) {
        if (x < 0 || y < 0 || x + w > screen.width || y + H > screen.height) return false;
        for (ClickableWidget o : others) {
            if (o == this || !o.visible) continue;
            boolean overlap = x - 2 < o.getX() + o.getWidth() && x + w + 2 > o.getX()
                    && y - 2 < o.getY() + o.getHeight() && y + H + 2 > o.getY();
            if (overlap) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
        place();
        float a = getAlpha(); // у главного меню есть плавное появление
        if (a <= 0.02f) return;
        UpdateManager m = UpdateManager.get();
        UpdateManager.Phase ph = m.phase();

        String status;
        int accent;
        String[] icon;
        long now = System.currentTimeMillis();
        if (ph.active()) {
            status = ph == UpdateManager.Phase.DOWNLOADING && m.filesTotal() > 0
                    ? "Скачиваю " + m.filesDone() + "/" + m.filesTotal() : "Проверяю…";
            accent = 0xFF55C8FF;
            icon = Pixel.HOURGLASS[(int) ((now / 250) % Pixel.HOURGLASS.length)];
        } else {
            switch (ph) {
                case UP_TO_DATE -> { status = "Моды актуальны"; accent = 0xFF6BE36B; icon = Pixel.CHECK; }
                case RESTART_REQUIRED -> { status = "Нужен перезапуск"; accent = 0xFFFFB028; icon = Pixel.WARN; }
                case OFFLINE -> { status = "Сайт недоступен"; accent = 0xFFFFB028; icon = Pixel.WARN; }
                case SIGNATURE_BAD -> { status = "Ошибка подписи"; accent = 0xFFFF5555; icon = Pixel.CROSS; }
                case FAILED -> { status = "Ошибка обновления"; accent = 0xFFFF5555; icon = Pixel.CROSS; }
                default -> { status = "Нажмите для проверки"; accent = 0xFFB0B0B0; icon = Pixel.WARN; }
            }
        }

        boolean hover = isHovered() || isFocused();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int bg = hover ? 0xEE34404E : 0xD8202226;
        Pixel.panel(c, x, y, w, h, bg, hover ? 0xFF7A8796 : 0xFF4A4C52, 0xFF111214, a);
        if (hover) {
            // белая пиксельная рамка при наведении, как у кнопок Minecraft
            int wc = Pixel.alpha(0xFFFFFFFF, a);
            c.fill(x + 1, y, x + w - 1, y + 1, wc);
            c.fill(x + 1, y + h - 1, x + w - 1, y + h, wc);
            c.fill(x, y + 1, x + 1, y + h - 1, wc);
            c.fill(x + w - 1, y + 1, x + w, y + h - 1, wc);
        }
        // цветная полоска статуса слева
        c.fill(x + 2, y + 2, x + 4, y + h - 2, Pixel.alpha(accent, a));
        Pixel.icon(c, icon, x + 7, y + (h - 8) / 2, 1, a);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        c.drawTextWithShadow(tr, title, x + 19, y + 4, Pixel.alpha(0xFFFFE066, a));
        c.drawTextWithShadow(tr, status, x + 19, y + 13, Pixel.alpha(accent, a));
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        action.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
