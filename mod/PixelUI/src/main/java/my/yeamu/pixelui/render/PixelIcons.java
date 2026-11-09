package my.yeamu.pixelui.render;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Набор пиксельных иконок 8×8, нарисованных символами.
 * Палитра: k — контур, w — белый, y — жёлтый, o — оранжевый, r — красный, g — зелёный,
 * b — синий, n — коричневый, s — серый, a — цвет акцента темы, d/l — акцент темнее/светлее.
 * Свои иконки можно добавить через {@link #register(String, String[])}.
 */
public final class PixelIcons {

    private static final Map<String, String[]> ICONS = new HashMap<>();

    static {
        register("info", new String[]{
                ".kkkkkk.",
                "kaawwaak",
                "kaaaaaak",
                "kaawwaak",
                "kaawwaak",
                "kaawwaak",
                "kdawwadk",
                ".kkkkkk."});
        register("check", new String[]{
                "......kk",
                ".....kak",
                "....kalk",
                "k..kalk.",
                "kakalk..",
                "kaaak...",
                ".kdk....",
                "..k....."});
        register("cross", new String[]{
                "kk....kk",
                "kak..kak",
                ".kakkak.",
                "..kaak..",
                "..kaak..",
                ".kakkak.",
                "kdk..kdk",
                "kk....kk"});
        register("warn", new String[]{
                "...kk...",
                "..kyyk..",
                ".kykkyk.",
                ".kykkyk.",
                "kyykkyyk",
                "kyyyyyyk",
                "kyykkyyk",
                "kkkkkkkk"});
        register("star", new String[]{
                "...kk...",
                "..kyyk..",
                "kkkywkkk",
                "kyyyyyyk",
                ".kyyyyk.",
                ".kyooyk.",
                "kyokkoyk",
                "kkk..kkk"});
        register("heart", new String[]{
                ".kk..kk.",
                "krrkkrrk",
                "krwrrrrk",
                "krrrrrrk",
                ".krrrrk.",
                "..krrk..",
                "...kk...",
                "........"});
        register("coin", new String[]{
                "..kkkk..",
                ".kyyyyk.",
                "kyyooyyk",
                "kyoyywyk",
                "kyoyyyyk",
                "kyyooyyk",
                ".kyyyyk.",
                "..kkkk.."});
        register("bell", new String[]{
                "...kk...",
                "..kyyk..",
                ".kywyyk.",
                ".kyyyyk.",
                ".kyyyyk.",
                "kyyyyyyk",
                "kkkkkkkk",
                "...kk..."});
        register("gift", new String[]{
                "..k..k..",
                ".krkkrk.",
                "kkkkkkkk",
                "kaarraak",
                "kkkrrkkk",
                "kaarraak",
                "kdarradk",
                "kkkkkkkk"});
        register("sword", new String[]{
                "......kk",
                ".....kwk",
                "....kwk.",
                "k..kwk..",
                "kkkwk...",
                ".knk....",
                "knkk....",
                "kk......"});
        register("shield", new String[]{
                "kkkkkkkk",
                "kaaalaak",
                "kaaalaak",
                "kllllllk",
                "kaaalaak",
                ".kaalak.",
                "..kdak..",
                "...kk..."});
        register("clock", new String[]{
                ".kkkkkk.",
                "kwwwwwwk",
                "kwwkwwwk",
                "kwwkwwwk",
                "kwwkkkwk",
                "kwwwwwwk",
                "kswwwwsk",
                ".kkkkkk."});
    }

    private PixelIcons() {}

    /** Регистрирует свою иконку 8×8 (или любого размера до 16×16 при scale 1). */
    public static void register(String name, String[] rows) {
        ICONS.put(name.toLowerCase(Locale.ROOT), rows);
    }

    public static String[] get(String name) {
        return name == null ? null : ICONS.get(name.toLowerCase(Locale.ROOT));
    }

    public static int color(char ch, int accent) {
        return switch (ch) {
            case 'k' -> 0xFF000000;
            case 'w' -> 0xFFFFFFFF;
            case 'y' -> 0xFFFFD83D;
            case 'o' -> 0xFFE58A1F;
            case 'r' -> 0xFFFF4B4B;
            case 'g' -> 0xFF5BE34A;
            case 'b' -> 0xFF3FA9FF;
            case 'n' -> 0xFF8A5A2B;
            case 's' -> 0xFF8B8B8B;
            case 'a' -> accent | 0xFF000000;
            case 'd' -> Pixel.mix(accent | 0xFF000000, 0xFF000000, 0.35f);
            case 'l' -> Pixel.mix(accent | 0xFF000000, 0xFFFFFFFF, 0.45f);
            default -> 0;
        };
    }
}
