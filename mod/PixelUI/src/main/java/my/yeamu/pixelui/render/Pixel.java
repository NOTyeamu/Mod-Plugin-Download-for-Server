package my.yeamu.pixelui.render;

import my.yeamu.pixelui.api.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.util.HashMap;
import java.util.Map;

/**
 * Пиксельная отрисовка: панели со срезанными углами и фаской, кнопки с «толщиной»,
 * иконки из символов и предметы. Всё рисуется прямоугольниками без текстур — всегда чётко.
 */
public final class Pixel {

    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    private Pixel() {}

    // ------------------------------------------------------------------ цвета и анимация

    public static int alpha(int argb, float a) {
        float k = Math.max(0f, Math.min(1f, a));
        int al = Math.round(((argb >>> 24) & 0xFF) * k);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Округление сдвига до 2 пикселей — движение «ступеньками», как в игре. */
    public static int snap(float v) {
        return Math.round(v / 2f) * 2;
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    public static float easeOut(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static float easeIn(float t) {
        float u = clamp01(t);
        return u * u * u;
    }

    /** Лёгкий «перелёт» в конце — уведомление забегает чуть дальше и возвращается. */
    public static float easeOutBack(float t) {
        float x = clamp01(t) - 1f;
        float c = 1.4f;
        return 1f + (c + 1f) * x * x * x + c * x * x;
    }

    // ------------------------------------------------------------------ текст

    /** Текст с кодами §/& → Text. */
    public static Text text(String s) {
        if (s == null || s.isEmpty()) return Text.empty();
        return Text.literal(s.replaceAll("&([0-9a-fk-orA-FK-OR])", "\u00a7$1"));
    }

    // ------------------------------------------------------------------ звук

    public static void sound(String id, float pitch) {
        if (id == null || id.isBlank()) return;
        Identifier i = Identifier.tryParse(id.trim());
        if (i == null) return;
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.ui(SoundEvent.of(i), pitch <= 0 ? 1f : pitch));
    }

    // ------------------------------------------------------------------ формы

    /** Панель: контур со срезанными углами, светлая фаска сверху-слева, тёмная снизу-справа. */
    public static void panel(DrawContext c, int x, int y, int w, int h, Theme t, float a) {
        int o = alpha(t.outline(), a);
        c.fill(x + 1, y, x + w - 1, y + 1, o);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, o);
        c.fill(x, y + 1, x + 1, y + h - 1, o);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, o);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(t.bg(), a));
        c.fill(x + 1, y + 1, x + w - 2, y + 2, alpha(t.light(), a));
        c.fill(x + 1, y + 2, x + 2, y + h - 2, alpha(t.light(), a));
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, alpha(t.dark(), a));
        c.fill(x + w - 2, y + 1, x + w - 1, y + h - 2, alpha(t.dark(), a));
    }

    /** Горизонтальный разделитель «бороздка» (тёмная + светлая линия). */
    public static void separator(DrawContext c, int x1, int x2, int y, Theme t, float a) {
        c.fill(x1, y, x2, y + 1, alpha(t.dark(), a));
        c.fill(x1, y + 1, x2, y + 2, alpha(t.light(), a));
    }

    /** Кнопка как в Minecraft: контур, светлая грань сверху, тёмная толщина снизу, белая рамка при наведении. */
    public static void button(DrawContext c, TextRenderer tr, int x, int y, int w, int h, Text label, String icon,
                              int base, int accent, boolean hover, boolean active, float a) {
        int fill = !active ? 0xFF4A4A4A : hover ? mix(base, 0xFFFFFFFF, 0.18f) : base;
        c.fill(x, y, x + w, y + h, alpha(hover && active ? 0xFFFFFFFF : 0xFF000000, a));
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(fill, a));
        c.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(mix(fill, 0xFFFFFFFF, 0.35f), a));
        c.fill(x + 1, y + 2, x + 2, y + h - 3, alpha(mix(fill, 0xFFFFFFFF, 0.2f), a));
        c.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, alpha(mix(fill, 0xFF000000, 0.45f), a));
        if (a < 0.1f) return;
        boolean hasIcon = hasIcon(icon);
        int room = w - 8 - (hasIcon ? 12 : 0);
        StringVisitable trimmed = tr.trimToWidth(label, room);
        int tw = tr.getWidth(trimmed);
        int total = tw + (hasIcon ? 12 : 0);
        int sx = x + (w - total) / 2;
        if (hasIcon) {
            icon(c, icon, sx, y + (h - 10) / 2, 1, accent, a);
            sx += 12;
        }
        int tc = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        c.drawText(tr, Language.getInstance().reorder(trimmed), sx, y + (h - 10) / 2, alpha(tc, a), true);
    }

    // ------------------------------------------------------------------ иконки

    public static boolean hasIcon(String spec) {
        if (spec == null || spec.isBlank() || spec.equalsIgnoreCase("none")) return false;
        return spec.startsWith("item:") ? item(spec) != null : PixelIcons.get(spec) != null;
    }

    private static ItemStack item(String spec) {
        return ITEMS.computeIfAbsent(spec, s -> {
            Identifier id = Identifier.tryParse(s.substring(5).trim());
            if (id == null) return ItemStack.EMPTY;
            Item it = Registries.ITEM.get(id);
            return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
        }).isEmpty() ? null : ITEMS.get(spec);
    }

    /**
     * Рисует иконку: имя из {@link PixelIcons} (8×8, каждый пиксель — scale×scale)
     * или предмет "item:minecraft:diamond" (16×16).
     */
    public static void icon(DrawContext c, String spec, int x, int y, int scale, int accent, float a) {
        if (!hasIcon(spec)) return;
        if (spec.startsWith("item:")) {
            if (a < 0.5f) return; // предметы не умеют прозрачность — просто прячем в начале/конце анимации
            ItemStack st = item(spec);
            if (st != null) c.drawItem(st, x + (scale * 8 - 16) / 2, y + (scale * 8 - 16) / 2);
            return;
        }
        String[] rows = PixelIcons.get(spec);
        for (int row = 0; row < rows.length; row++) {
            String r = rows[row];
            for (int col = 0; col < r.length(); col++) {
                int color = PixelIcons.color(r.charAt(col), accent);
                if (color == 0) continue;
                int px = x + col * scale, py = y + row * scale;
                c.fill(px, py, px + scale, py + scale, alpha(color, a));
            }
        }
    }
}
