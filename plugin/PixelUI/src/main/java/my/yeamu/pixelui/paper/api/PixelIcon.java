package my.yeamu.pixelui.paper.api;

import org.bukkit.Material;

/**
 * Иконки: встроенные пиксельные (рисует клиентский мод, красятся акцентом темы)
 * или любой предмет игры: {@code PixelIcon.item(Material.DIAMOND)}.
 */
public final class PixelIcon {

    public static final String NONE = "none";
    public static final String INFO = "info";
    public static final String CHECK = "check";
    public static final String CROSS = "cross";
    public static final String WARN = "warn";
    public static final String STAR = "star";
    public static final String HEART = "heart";
    public static final String COIN = "coin";
    public static final String BELL = "bell";
    public static final String GIFT = "gift";
    public static final String SWORD = "sword";
    public static final String SHIELD = "shield";
    public static final String CLOCK = "clock";

    private PixelIcon() {}

    @SuppressWarnings("deprecation")
    public static String item(Material material) {
        return "item:" + material.getKey();
    }

    /** Предмет по id, например "minecraft:diamond" или предмет другого мода. */
    public static String item(String id) {
        return "item:" + id;
    }
}
