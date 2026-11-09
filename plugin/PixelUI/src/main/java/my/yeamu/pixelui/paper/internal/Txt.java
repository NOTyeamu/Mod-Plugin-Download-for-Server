package my.yeamu.pixelui.paper.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * Текст: поддерживаются коды &a/&l и MiniMessage (<green>, <bold>...).
 * Клиенту уходит строка с §-кодами. HEX-цвета округляются до 16 стандартных:
 * ванильный рендер текста не понимает §x.
 */
public final class Txt {

    private static final Pattern MINI = Pattern.compile("<[a-zA-Z#/!][^<>]*>");
    private static final Pattern AMP = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private Txt() {}

    public static String section(String s) {
        if (s == null || s.isEmpty()) return "";
        if (MINI.matcher(s).find()) {
            try {
                return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(s));
            } catch (Exception ignored) {
                // не MiniMessage — обрабатываем как обычный текст
            }
        }
        return AMP.matcher(s).replaceAll("\u00a7$1");
    }

    public static Component component(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(section(s));
    }

    public static String plain(String s) {
        return PlainTextComponentSerializer.plainText().serialize(component(s));
    }
}
