package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Тема оформления: пресет + необязательные цвета поверх. Неизменяемая.
 * Пример: {@code PixelTheme.DARK.accent(0x55FFFF).title(0xFFFF55)}.
 */
public final class PixelTheme {

    public static final PixelTheme DARK = new PixelTheme("dark");
    public static final PixelTheme STONE = new PixelTheme("stone");
    public static final PixelTheme SUCCESS = new PixelTheme("success");
    public static final PixelTheme ERROR = new PixelTheme("error");
    public static final PixelTheme WARNING = new PixelTheme("warning");
    public static final PixelTheme INFO = new PixelTheme("info");
    public static final PixelTheme GOLD = new PixelTheme("gold");
    public static final PixelTheme ENDER = new PixelTheme("ender");
    public static final PixelTheme NETHER = new PixelTheme("nether");

    private final String preset;
    private final Map<String, String> colors;
    private final Boolean shadow;

    private PixelTheme(String preset) {
        this(preset, Map.of(), null);
    }

    private PixelTheme(String preset, Map<String, String> colors, Boolean shadow) {
        this.preset = preset;
        this.colors = colors;
        this.shadow = shadow;
    }

    /** Пресет по имени: dark, stone, success, error, warning, info, gold, ender, nether. */
    public static PixelTheme preset(String name) {
        return new PixelTheme(name == null ? "dark" : name.trim().toLowerCase(Locale.ROOT));
    }

    public String presetName() {
        return preset;
    }

    // Цвета: 0xRRGGBB или 0xAARRGGBB, либо строка "#RRGGBB"
    public PixelTheme accent(int c) { return with("accent", c); }
    public PixelTheme accent(String hex) { return with("accent", hex); }
    public PixelTheme bg(int c) { return with("bg", c); }
    public PixelTheme outline(int c) { return with("outline", c); }
    public PixelTheme light(int c) { return with("light", c); }
    public PixelTheme dark(int c) { return with("dark", c); }
    public PixelTheme title(int c) { return with("title", c); }
    public PixelTheme text(int c) { return with("text", c); }

    public PixelTheme shadow(boolean v) {
        return new PixelTheme(preset, colors, v);
    }

    private PixelTheme with(String key, int c) {
        String hex = (c >>> 24) == 0 ? String.format("#%06X", c & 0xFFFFFF) : String.format("#%08X", c);
        return with(key, hex);
    }

    private PixelTheme with(String key, String hex) {
        if (hex == null || !hex.startsWith("#")) throw new IllegalArgumentException("Цвет должен быть вида #RRGGBB: " + hex);
        Map<String, String> m = new LinkedHashMap<>(colors);
        m.put(key, hex);
        return new PixelTheme(preset, m, shadow);
    }

    public JsonElement toJson() {
        if (colors.isEmpty() && shadow == null) return new JsonPrimitive(preset);
        JsonObject o = new JsonObject();
        o.addProperty("preset", preset);
        colors.forEach(o::addProperty);
        if (shadow != null) o.addProperty("shadow", shadow);
        return o;
    }

    /** Цвет для запасного варианта в чате (игроки без мода). */
    public NamedTextColor chatColor() {
        return switch (preset) {
            case "success", "green" -> NamedTextColor.GREEN;
            case "error", "red", "nether" -> NamedTextColor.RED;
            case "warning", "warn", "yellow" -> NamedTextColor.YELLOW;
            case "info", "blue" -> NamedTextColor.AQUA;
            case "gold" -> NamedTextColor.GOLD;
            case "ender", "purple" -> NamedTextColor.LIGHT_PURPLE;
            case "stone", "light" -> NamedTextColor.GRAY;
            default -> NamedTextColor.GREEN;
        };
    }

    /** Цвет боссбара для запасного варианта плашки. */
    public BossBar.Color barColor() {
        return switch (preset) {
            case "error", "red", "nether" -> BossBar.Color.RED;
            case "warning", "warn", "yellow", "gold" -> BossBar.Color.YELLOW;
            case "info", "blue" -> BossBar.Color.BLUE;
            case "ender", "purple" -> BossBar.Color.PURPLE;
            case "stone", "light" -> BossBar.Color.WHITE;
            default -> BossBar.Color.GREEN;
        };
    }
}
