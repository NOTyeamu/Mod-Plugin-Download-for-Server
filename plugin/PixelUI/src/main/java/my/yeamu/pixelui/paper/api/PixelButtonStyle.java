package my.yeamu.pixelui.paper.api;

import java.util.Locale;

/** Стиль (цвет) кнопки меню. Любой свой цвет — через ButtonBuilder.color(0xRRGGBB). */
public enum PixelButtonStyle {
    DEFAULT, PRIMARY, SUCCESS, DANGER, INFO, GOLD;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
