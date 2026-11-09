package my.yeamu.pixelui.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Цветовая тема пиксельного окна. Все цвета — ARGB.
 * bg — заливка, outline — контур, light/dark — фаска (свет сверху-слева, тень снизу-справа),
 * accent — акцент (полоска, прогресс, иконка), title/text — цвета заголовка и текста.
 */
public record Theme(int bg, int outline, int light, int dark, int accent, int title, int text, boolean shadow) {

    /** Тёмная (по умолчанию) — как подсказки и тосты Minecraft. */
    public static final Theme DARK = new Theme(0xF0232323, 0xFF000000, 0xFF4E4E4E, 0xFF101010, 0xFF5BE34A, 0xFFFFFFFF, 0xFFC8C8C8, true);
    /** Светлый «камень» — как окно инвентаря. */
    public static final Theme STONE = new Theme(0xFFC6C6C6, 0xFF000000, 0xFFFFFFFF, 0xFF555555, 0xFF3F76E4, 0xFF3F3F3F, 0xFF505050, false);
    public static final Theme SUCCESS = DARK.withAccent(0xFF5BE34A);
    public static final Theme ERROR = DARK.withAccent(0xFFFF4B4B);
    public static final Theme WARNING = DARK.withAccent(0xFFFFC83D);
    public static final Theme INFO = DARK.withAccent(0xFF3FA9FF);
    /** Золото — награды, донат, достижения. */
    public static final Theme GOLD = new Theme(0xF02B210A, 0xFF000000, 0xFF7A5E1C, 0xFF140E02, 0xFFFFC83D, 0xFFFFE08A, 0xFFE6D3A3, true);
    /** Энд — фиолетовая. */
    public static final Theme ENDER = new Theme(0xF01E1229, 0xFF000000, 0xFF4E3566, 0xFF0B0510, 0xFFC066FF, 0xFFF0DDFF, 0xFFCDB6E0, true);
    /** Незер — красно-оранжевая. */
    public static final Theme NETHER = new Theme(0xF02A0F0F, 0xFF000000, 0xFF6A2A22, 0xFF120404, 0xFFFF6A2B, 0xFFFFD6C2, 0xFFE0B3A3, true);

    public Theme withAccent(int accent) {
        return new Theme(bg, outline, light, dark, accent, title, text, shadow);
    }

    public static Theme preset(String name) {
        if (name == null) return DARK;
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "stone", "light" -> STONE;
            case "success", "green" -> SUCCESS;
            case "error", "red" -> ERROR;
            case "warning", "warn", "yellow" -> WARNING;
            case "info", "blue" -> INFO;
            case "gold" -> GOLD;
            case "ender", "purple" -> ENDER;
            case "nether" -> NETHER;
            default -> DARK;
        };
    }

    /**
     * Тема из JSON: строка с названием пресета или объект
     * {"preset":"dark","accent":"#FF55FF","bg":"#E0101010",...}.
     */
    public static Theme fromJson(JsonElement e) {
        if (e == null || e.isJsonNull()) return DARK;
        if (e.isJsonPrimitive()) return preset(e.getAsString());
        if (!e.isJsonObject()) return DARK;
        JsonObject o = e.getAsJsonObject();
        Theme b = preset(o.has("preset") ? o.get("preset").getAsString() : "dark");
        return new Theme(
                color(o, "bg", b.bg), color(o, "outline", b.outline), color(o, "light", b.light),
                color(o, "dark", b.dark), color(o, "accent", b.accent), color(o, "title", b.title),
                color(o, "text", b.text), o.has("shadow") ? o.get("shadow").getAsBoolean() : b.shadow);
    }

    private static int color(JsonObject o, String key, int def) {
        return o.has(key) ? parseColor(o.get(key).getAsString(), def) : def;
    }

    /** "#RRGGBB", "#AARRGGBB" или "0x...". Ошибка — значение по умолчанию. */
    public static int parseColor(String s, int def) {
        if (s == null) return def;
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        else if (h.startsWith("0x") || h.startsWith("0X")) h = h.substring(2);
        try {
            long v = Long.parseLong(h, 16);
            if (h.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
