package my.yeamu.modsync.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Пиксельная отрисовка в стиле Minecraft: панели со срезанными углами и фаской,
 * «настоящие» кнопки с толщиной снизу и пиксельные иконки 8×8.
 * Всё рисуется прямоугольниками — никаких текстур, поэтому картинка всегда чёткая.
 */
public final class Pixel {

    private Pixel() {}

    // ------------------------------------------------------------------ иконки 8×8
    // k — контур, w — белый, g — зелёный, r — красный, y — жёлтый, o — оранжевый, b — синий, . — пусто

    public static final String[] CHECK = {
            "......kk",
            ".....kgk",
            "....kggk",
            "k..kggk.",
            "kgkggk..",
            "kgggk...",
            ".kgk....",
            "..k....."};

    public static final String[] CROSS = {
            "kk....kk",
            "krk..krk",
            ".krkkrk.",
            "..krrk..",
            "..krrk..",
            ".krkkrk.",
            "krk..krk",
            "kk....kk"};

    public static final String[] WARN = {
            "...kk...",
            "..kyyk..",
            ".kykkyk.",
            ".kykkyk.",
            "kyykkyyk",
            "kyyyyyyk",
            "kyykkyyk",
            "kkkkkkkk"};

    /** Песочные часы — 2 кадра для анимации ожидания. */
    public static final String[][] HOURGLASS = {{
            "kkkkkkkk",
            ".kyyyyk.",
            "..kyyk..",
            "...kk...",
            "...kk...",
            "..k..k..",
            ".k.yy.k.",
            "kkkkkkkk"}, {
            "kkkkkkkk",
            ".k....k.",
            "..kyyk..",
            "...kk...",
            "...kk...",
            "..kyyk..",
            ".kyyyyk.",
            "kkkkkkkk"}};

    private static int paletteColor(char ch) {
        return switch (ch) {
            case 'k' -> 0xFF000000;
            case 'w' -> 0xFFFFFFFF;
            case 'g' -> 0xFF5BE34A;
            case 'r' -> 0xFFFF4B4B;
            case 'y' -> 0xFFFFD83D;
            case 'o' -> 0xFFE58A1F;
            case 'b' -> 0xFF3FA9FF;
            default -> 0;
        };
    }

    /** Рисует иконку 8×8, каждый «пиксель» — квадрат scale×scale. */
    public static void icon(DrawContext c, String[] rows, int x, int y, int scale, float a) {
        for (int row = 0; row < rows.length; row++) {
            String r = rows[row];
            for (int col = 0; col < r.length(); col++) {
                int color = paletteColor(r.charAt(col));
                if (color == 0) continue;
                int px = x + col * scale, py = y + row * scale;
                c.fill(px, py, px + scale, py + scale, alpha(color, a));
            }
        }
    }

    // ------------------------------------------------------------------ цвета

    public static int alpha(int argb, float a) {
        float k = Math.max(0f, Math.min(1f, a));
        int al = Math.round(((argb >>> 24) & 0xFF) * k);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** Смешивает два цвета: t=0 — первый, t=1 — второй. */
    public static int mix(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Округление до чётного пикселя — движение получается «ступеньками», как в игре. */
    public static int snap(float v) {
        return Math.round(v / 2f) * 2;
    }

    public static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    // ------------------------------------------------------------------ панели и кнопки

    /** Панель: чёрный контур со срезанными углами, светлая фаска сверху-слева и тёмная снизу-справа. */
    public static void panel(DrawContext c, int x, int y, int w, int h, int bg, int light, int dark, float a) {
        int o = alpha(0xFF000000, a);
        c.fill(x + 1, y, x + w - 1, y + 1, o);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, o);
        c.fill(x, y + 1, x + 1, y + h - 1, o);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, o);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(bg, a));
        c.fill(x + 1, y + 1, x + w - 2, y + 2, alpha(light, a));
        c.fill(x + 1, y + 2, x + 2, y + h - 2, alpha(light, a));
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, alpha(dark, a));
        c.fill(x + w - 2, y + 1, x + w - 1, y + h - 2, alpha(dark, a));
    }

    /**
     * Кнопка как в Minecraft: контур, светлая грань сверху, тёмная «толщина» снизу.
     * При наведении — белая рамка и светлее заливка.
     */
    public static void button(DrawContext c, TextRenderer tr, int x, int y, int w, int h,
                              String label, int base, boolean hover, boolean active, float a) {
        int fill = !active ? 0xFF4A4A4A : hover ? mix(base, 0xFFFFFFFF, 0.18f) : base;
        c.fill(x, y, x + w, y + h, alpha(hover && active ? 0xFFFFFFFF : 0xFF000000, a));
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(fill, a));
        c.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(mix(fill, 0xFFFFFFFF, 0.35f), a));
        c.fill(x + 1, y + 2, x + 2, y + h - 3, alpha(mix(fill, 0xFFFFFFFF, 0.2f), a));
        c.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, alpha(mix(fill, 0xFF000000, 0.45f), a));
        if (a < 0.1f) return;
        String s = tr.trimToWidth(label, w - 6);
        int tc = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        c.drawTextWithShadow(tr, s, x + (w - tr.getWidth(s)) / 2, y + (h - 10) / 2, alpha(tc, a));
    }
}
