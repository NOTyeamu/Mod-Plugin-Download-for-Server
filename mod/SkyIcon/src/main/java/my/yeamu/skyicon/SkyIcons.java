package my.yeamu.skyicon;

/**
 * Иконки солнца и луны 24×24 — каждая буква это один пиксель своего цвета.
 * Нарисованы с тенью (свет сверху-слева), бликами и тёмным контуром, чтобы читались на любом небе.
 */
public final class SkyIcons {

    public static final int SIZE = 24;

    // Солнце: диск с бликом, оранжевый край, 8 лучей
    public static final String[] SUN = {
            "...........RR...........",
            "...........rr...........",
            "...........rr...........",
            "...........rr...........",
            "....R......kk......R....",
            ".....r..kkkookkk..r.....",
            ".......koooYYoook.......",
            "......koowwWwwwook......",
            ".....koowWWwwwYYook.....",
            ".....kowWWwwwYYYYok.....",
            ".....kowWwwwYYYYYok.....",
            "RrrrkoYWwwwYYYYYyyokrrrR",
            "RrrrkoYwwwYYYYYyyyokrrrR",
            ".....kowwYYYYYyyyok.....",
            ".....kowYYYYYyyyyok.....",
            ".....kooYYYYyyyyook.....",
            "......kooYYyyyyook......",
            ".......koooyyoook.......",
            ".....r..kkkookkk..r.....",
            "....R......kk......R....",
            "...........rr...........",
            "...........rr...........",
            "...........rr...........",
            "...........RR...........",
    };

    // Луна: серп с кратерами и звёзды-искорки
    public static final String[] MOON = {
            "........................",
            "........................",
            ".....................s..",
            ".......kk...........sWs.",
            ".....kkbbk...........s..",
            "....kbbWbk..............",
            "...kbWWbk...............",
            "...kbWWbk........s......",
            "..kbWWWbk........S......",
            "..kbWWWbk......sSWSs....",
            ".kbWWCCbk........S......",
            ".kbWWCcbk........s......",
            ".kbWWmmmbk..............",
            ".kbWWmmmbk..............",
            ".kbWmCmmMbk..........s..",
            "..kbCcmCCMbkkk....k.sWs.",
            "..kbmmCCccMbbbkkkkbk.s..",
            "...kbmMcccMnnnbbbbk.....",
            "...kbMMMMMnnnnnnnbk.....",
            "....kbbMMMnnCcnbbk......",
            ".....kkbbbbbbbbkk.......",
            ".......kkkkkkkk.........",
            "........................",
            "........................",
    };

    private SkyIcons() {}

    /** Цвет пикселя (ARGB), 0 — прозрачный. */
    public static int color(char ch, boolean night) {
        if (night) {
            return switch (ch) {
                case 'k' -> 0xFF141A33; // контур
                case 'b' -> 0xFF7E8CB8; // край серпа
                case 'W' -> 0xFFFFFFFF; // блик
                case 'm' -> 0xFFE8EEFF;
                case 'M' -> 0xFFC9D3F2;
                case 'n' -> 0xFFA3B0D9; // тень
                case 'c' -> 0xFF8E9BC6; // кратер
                case 'C' -> 0xFF6C78A6; // кратер в тени
                case 's' -> 0xFF8FD8FF; // звезда
                case 'S' -> 0xFFD9F4FF;
                default -> 0;
            };
        }
        return switch (ch) {
            case 'k' -> 0xFF2A1400; // контур
            case 'o' -> 0xFFC85A00; // край диска
            case 'O' -> 0xFFEF8A10;
            case 'y' -> 0xFFFFB521;
            case 'Y' -> 0xFFFFD43A;
            case 'w' -> 0xFFFFEE88;
            case 'W' -> 0xFFFFFFFF; // блик
            case 'r' -> 0xFFFFC23A; // луч
            case 'R' -> 0xFFFF8A1A; // кончик луча
            default -> 0;
        };
    }
}
