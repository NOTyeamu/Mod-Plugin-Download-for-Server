package my.yeamu.pixelui.screen;

import my.yeamu.pixelui.api.Menu;
import my.yeamu.pixelui.api.Theme;
import my.yeamu.pixelui.render.Pixel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Экран меню PixelUI. Кнопки — обычные ButtonWidget (клавиатура, Tab, звук клика, подсказки),
 * но рисуются поверх своим пиксельным стилем. Окно плавно «вырастает» при открытии.
 */
public class MenuScreen extends Screen {

    private static final long ANIM = 220;

    public final boolean fromServer;
    private final Menu menu;
    private final Consumer<Menu.Button> onClick;
    private final Runnable onClose;
    private final long opened = System.currentTimeMillis();
    private final List<ButtonWidget> widgets = new ArrayList<>();
    private final List<Menu.Button> widgetButtons = new ArrayList<>();
    private List<OrderedText> lines = List.of();
    private List<OrderedText> title = List.of();
    /** true — не сообщать о закрытии (закрыли кнопкой, сервером или заменили новым меню). */
    private boolean silent;
    private int px, py, pw, ph;

    public MenuScreen(Menu menu, boolean fromServer, Consumer<Menu.Button> onClick, Runnable onClose) {
        super(Pixel.text(menu.title));
        this.menu = menu;
        this.fromServer = fromServer;
        this.onClick = onClick;
        this.onClose = onClose;
    }

    public Menu menu() {
        return menu;
    }

    public void markReplaced() {
        silent = true;
    }

    public void closeSilently() {
        silent = true;
        if (client != null && client.currentScreen == this) client.setScreen(null);
    }

    @Override
    protected void init() {
        widgets.clear();
        widgetButtons.clear();
        pw = Math.min(menu.width, width - 16);
        boolean icon = Pixel.hasIcon(menu.icon);
        int titleX = icon ? 30 : 10;
        title = menu.title.isEmpty() ? List.of() : textRenderer.wrapLines(Pixel.text(menu.title), pw - titleX - 10);
        if (title.size() > 2) title = title.subList(0, 2);
        List<OrderedText> l = new ArrayList<>();
        for (String s : menu.lines) {
            if (s.isEmpty()) l.add(OrderedText.EMPTY);
            else l.addAll(textRenderer.wrapLines(Pixel.text(s), pw - 20));
        }
        lines = l;

        int cols = Math.min(menu.columns, Math.max(1, menu.buttons.size()));
        int rows = (menu.buttons.size() + cols - 1) / cols;
        int header = Math.max(24, 8 + title.size() * 10 + 6);
        int body = lines.isEmpty() ? 0 : 6 + lines.size() * 10;
        ph = header + body + 8 + rows * 24 + 4;
        // Слишком высокое меню урезаем по тексту
        while (ph > height - 12 && !lines.isEmpty()) {
            lines = lines.subList(0, lines.size() - 1);
            body = lines.isEmpty() ? 0 : 6 + lines.size() * 10;
            ph = header + body + 8 + rows * 24 + 4;
        }
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        int gap = 4;
        int bw = (pw - 20 - gap * (cols - 1)) / cols;
        int gridY = py + header + body + 8;
        for (int i = 0; i < menu.buttons.size(); i++) {
            Menu.Button b = menu.buttons.get(i);
            int row = i / cols, col = i % cols;
            int inRow = Math.min(cols, menu.buttons.size() - row * cols);
            int shift = (cols - inRow) * (bw + gap) / 2; // неполный последний ряд — по центру
            int x = px + 10 + shift + col * (bw + gap);
            int y = gridY + row * 24;
            ButtonWidget w = ButtonWidget.builder(Pixel.text(b.label()), btn -> click(b))
                    .dimensions(x, y, bw, 20).build();
            w.active = b.enabled();
            if (b.tooltip() != null && !b.tooltip().isEmpty()) w.setTooltip(Tooltip.of(Pixel.text(b.tooltip())));
            addDrawableChild(w);
            widgets.add(w);
            widgetButtons.add(b);
        }
    }

    private void click(Menu.Button b) {
        onClick.accept(b);
        if (b.close()) {
            silent = true;
            close();
        }
    }

    private float anim() {
        return Pixel.easeOut((System.currentTimeMillis() - opened) / (float) ANIM);
    }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        float t = anim();
        c.fill(0, 0, width, height, Pixel.alpha(0x90000000, t));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta); // фон + стандартные кнопки (их перекроем)
        float t = anim();
        Theme th = menu.theme;
        // «Вырастание» ступеньками по 2 пикселя
        int dy = Pixel.snap((1f - t) * 16);
        Pixel.panel(c, px, py + dy, pw, ph, th, Math.max(t, 0.01f));
        if (t < 0.6f) return;
        float a = (t - 0.6f) / 0.4f;

        int header = Math.max(24, 8 + title.size() * 10 + 6);
        boolean icon = Pixel.hasIcon(menu.icon);
        int titleX = px + (icon ? 30 : 10);
        if (icon) Pixel.icon(c, menu.icon, px + 8, py + dy + (header - 16) / 2, 2, th.accent(), a);
        int ty = py + dy + (header - title.size() * 10) / 2 + 1;
        for (OrderedText line : title) {
            c.drawText(textRenderer, line, titleX, ty, Pixel.alpha(th.title(), a), th.shadow());
            ty += 10;
        }
        Pixel.separator(c, px + 4, px + pw - 4, py + dy + header - 2, th, a);

        int ly = py + dy + header + 6;
        for (OrderedText line : lines) {
            c.drawText(textRenderer, line, px + 10, ly, Pixel.alpha(th.text(), a), th.shadow());
            ly += 10;
        }

        for (int i = 0; i < widgets.size(); i++) {
            ButtonWidget w = widgets.get(i);
            Menu.Button b = widgetButtons.get(i);
            Pixel.button(c, textRenderer, w.getX(), w.getY() + dy, w.getWidth(), w.getHeight(), Pixel.text(b.label()),
                    b.icon(), styleColor(b.style(), th), th.accent(), w.isSelected(), w.active, a);
        }
    }

    private static int styleColor(String style, Theme th) {
        if (style == null) style = "default";
        if (style.startsWith("#")) return Theme.parseColor(style, 0xFF5A5A5A);
        return switch (style.toLowerCase(Locale.ROOT)) {
            case "primary" -> Pixel.mix(th.accent(), 0xFF000000, 0.3f);
            case "success" -> 0xFF3C8527;
            case "danger" -> 0xFFB3312C;
            case "info" -> 0xFF2F6FB5;
            case "gold" -> 0xFFC9971C;
            default -> th == Theme.STONE ? 0xFF8B8B8B : 0xFF5A5A5A;
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return menu.closeable;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (!silent) {
            silent = true;
            onClose.run();
        }
    }
}
