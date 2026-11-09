package my.yeamu.pixelui.api;

import java.util.Locale;

/** Место на экране для уведомлений и плашек. */
public enum Position {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    public boolean isTop() { return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT; }
    public boolean isBottom() { return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT; }
    public boolean isLeft() { return this == TOP_LEFT || this == BOTTOM_LEFT; }
    public boolean isRight() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }

    /** "top_right", "top-right", "top" (= TOP_CENTER), "bottom" (= BOTTOM_CENTER), "center". */
    public static Position parse(String s, Position def) {
        if (s == null || s.isBlank()) return def;
        String n = s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (n.equals("TOP")) return TOP_CENTER;
        if (n.equals("BOTTOM")) return BOTTOM_CENTER;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
