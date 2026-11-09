package my.yeamu.pixelui.render;

/**
 * Большой набор встроенных пиксельных иконок 8×8 (нарисованы вручную, без текстур).
 * Доступны любому моду и плагину по имени: {@code .icon("diamond")}.
 * Список и превью: docs/PixelUI.md, docs/pixelui-icons.svg.
 */
final class PixelIconsSet {

    private PixelIconsSet() {}

    static void registerAll() {
        // ---- Интерфейс и символы ----
        PixelIcons.register("home", new String[]{
                "...kk...",
                "..krrk..",
                ".krrrrk.",
                "krrrrrrk",
                ".kwwwwk.",
                ".kwnnwk.",
                ".kwnnwk.",
                ".kkkkkk."});
        PixelIcons.register("gear", new String[]{
                "k..kk..k",
                ".kkaakk.",
                ".kaddak.",
                "kadkkdak",
                "kadkkdak",
                ".kaddak.",
                ".kkaakk.",
                "k..kk..k"});
        PixelIcons.register("search", new String[]{
                ".kkkk...",
                "kccwck..",
                "kcccck..",
                "kcccck..",
                ".kkkkk..",
                "....knk.",
                ".....knk",
                "......kk"});
        PixelIcons.register("lock", new String[]{
                "..kkkk..",
                ".ks..sk.",
                ".ks..sk.",
                "kkkkkkkk",
                "kyyyyyyk",
                "kyykkyyk",
                "koykkyok",
                "kkkkkkkk"});
        PixelIcons.register("unlock", new String[]{
                "..kkkk..",
                ".ks..sk.",
                ".ks.....",
                "kkkkkkkk",
                "kyyyyyyk",
                "kyykkyyk",
                "koykkyok",
                "kkkkkkkk"});
        PixelIcons.register("key", new String[]{
                "..kkk...",
                ".kyyyk..",
                ".ky.yk..",
                ".kyyyk..",
                "..kyk...",
                "..kyyk..",
                "..kyk...",
                "..kkk..."});
        PixelIcons.register("user", new String[]{
                "..kkkk..",
                ".kttttk.",
                ".ktkktk.",
                ".kttttk.",
                "..kkkk..",
                ".kaaaak.",
                "kaaaaaak",
                "kkkkkkkk"});
        PixelIcons.register("users", new String[]{
                ".kk..kk.",
                "kttkkttk",
                "kttkkttk",
                ".kk..kk.",
                "kaak.kbk",
                "kaakkbbk",
                "kaakkbbk",
                "kkkkkkkk"});
        PixelIcons.register("chat", new String[]{
                ".kkkkkk.",
                "kwwwwwwk",
                "kwwwwwwk",
                "kwswswsk",
                "kwwwwwwk",
                ".kwkkkk.",
                ".kk.....",
                "........"});
        PixelIcons.register("mail", new String[]{
                "........",
                "kkkkkkkk",
                "kkwwwwkk",
                "kwkwwkwk",
                "kwwkkwwk",
                "kwwwwwwk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("plus", new String[]{
                "..kkkk..",
                "..kggk..",
                "kkkggkkk",
                "kggggggk",
                "kggggggk",
                "kkkggkkk",
                "..kggk..",
                "..kkkk.."});
        PixelIcons.register("minus", new String[]{
                "........",
                "........",
                "kkkkkkkk",
                "krrrrrrk",
                "krrrrrrk",
                "kkkkkkkk",
                "........",
                "........"});
        PixelIcons.register("arrow_up", new String[]{
                "...kk...",
                "..kaak..",
                ".kaaaak.",
                "kaaaaaak",
                "kkkaakkk",
                "..kaak..",
                "..kaak..",
                "..kkkk.."});
        PixelIcons.register("arrow_down", new String[]{
                "..kkkk..",
                "..kaak..",
                "..kaak..",
                "kkkaakkk",
                "kaaaaaak",
                ".kaaaak.",
                "..kaak..",
                "...kk..."});
        PixelIcons.register("arrow_left", new String[]{
                "...kk...",
                "..kak...",
                ".kaakkkk",
                "kaaaaaak",
                "kaaaaaak",
                ".kaakkkk",
                "..kak...",
                "...kk..."});
        PixelIcons.register("arrow_right", new String[]{
                "...kk...",
                "...kak..",
                "kkkkaak.",
                "kaaaaaak",
                "kaaaaaak",
                "kkkkaak.",
                "...kak..",
                "...kk..."});
        PixelIcons.register("refresh", new String[]{
                "..kkkkk.",
                ".kaaaaak",
                "kakkkkak",
                "kak..kkk",
                "kkk..kak",
                "kakkkkak",
                "kaaaaak.",
                ".kkkkk.."});
        PixelIcons.register("download", new String[]{
                "..kkkk..",
                "..kaak..",
                "..kaak..",
                "kkkaakkk",
                ".kaaaak.",
                "..kaak..",
                "k..kk..k",
                "kkkkkkkk"});
        PixelIcons.register("upload", new String[]{
                "...kk...",
                "..kaak..",
                ".kaaaak.",
                "kkkaakkk",
                "..kaak..",
                "..kkkk..",
                "k......k",
                "kkkkkkkk"});
        PixelIcons.register("trash", new String[]{
                "..kkkk..",
                "kkkkkkkk",
                "kmmmmmmk",
                "kkkkkkkk",
                ".khshsk.",
                ".khshsk.",
                ".khshsk.",
                ".kkkkkk."});
        PixelIcons.register("pencil", new String[]{
                "......kk",
                ".....kik",
                "....kykk",
                "...kyyk.",
                "..kyyk..",
                ".ktyk...",
                "kktk....",
                "kk......"});
        PixelIcons.register("eye", new String[]{
                "........",
                "..kkkk..",
                ".kwbbwk.",
                "kwbkkbwk",
                "kwbkkbwk",
                ".kwbbwk.",
                "..kkkk..",
                "........"});
        PixelIcons.register("menu", new String[]{
                "........",
                "wwwwwwww",
                "kkkkkkkk",
                "wwwwwwww",
                "kkkkkkkk",
                "wwwwwwww",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("question", new String[]{
                "..kkkk..",
                ".kwwwwk.",
                ".kwkkwk.",
                "..kkwwk.",
                "...kwk..",
                "...kkk..",
                "...kwk..",
                "...kkk.."});
        PixelIcons.register("exclamation", new String[]{
                "..kkkk..",
                "..kwwk..",
                "..kwwk..",
                "..kwwk..",
                "..kwwk..",
                "..kkkk..",
                "..kwwk..",
                "..kkkk.."});
        PixelIcons.register("flag", new String[]{
                "kkkkkk..",
                "ksrrrrk.",
                "ksrrrrrk",
                "ksrrrrk.",
                "kskkkk..",
                "ksk.....",
                "ksk.....",
                "kkk....."});
        PixelIcons.register("pin", new String[]{
                "..kkkk..",
                ".krrrrk.",
                "krrwwrrk",
                "krrwwrrk",
                ".krrrrk.",
                "..krrk..",
                "...kk...",
                "........"});
        PixelIcons.register("link", new String[]{
                "........",
                ".kkk.kkk",
                "kaak.kaa",
                "kak...ka",
                "kaakkkaa",
                ".kkk.kkk",
                "........",
                "........"});
        PixelIcons.register("power", new String[]{
                "...kk...",
                ".k.gg.k.",
                "kg.gg.gk",
                "kg.kk.gk",
                "kg....gk",
                "kgk..kgk",
                ".kggggk.",
                "..kkkk.."});
        PixelIcons.register("volume", new String[]{
                "....k...",
                "...kk..k",
                "kkkwk.k.",
                "kwwwk.kk",
                "kwwwk.kk",
                "kkkwk.k.",
                "...kk..k",
                "....k..."});
        PixelIcons.register("mute", new String[]{
                "....k...",
                "...kk...",
                "kkkwkr.r",
                "kwwwk.r.",
                "kwwwkr.r",
                "kkkwk...",
                "...kk...",
                "....k..."});
        PixelIcons.register("music", new String[]{
                "...kkk..",
                "...kaak.",
                "...kakak",
                "...kak.k",
                ".kkkak..",
                "kaaaak..",
                "kaaaak..",
                ".kkkk..."});
        PixelIcons.register("info_circle", new String[]{
                "..kkkk..",
                ".kbbbbk.",
                "kbbwwbbk",
                "kbbbbbbk",
                "kbbwwbbk",
                "kbbwwbbk",
                ".kbwwbk.",
                "..kkkk.."});
        PixelIcons.register("bookmark", new String[]{
                "kkkkkkkk",
                "krrrrrrk",
                "krrwrrrk",
                "krrrrrrk",
                "krrrrrrk",
                "krrkkrrk",
                "krk..krk",
                "kk....kk"});
        PixelIcons.register("calendar", new String[]{
                ".k....k.",
                "kkkkkkkk",
                "krrrrrrk",
                "kkkkkkkk",
                "kwkwkwwk",
                "kwwwwwwk",
                "kwkwkwwk",
                "kkkkkkkk"});
        PixelIcons.register("hourglass", new String[]{
                "kkkkkkkk",
                ".kwwwwk.",
                "..kyyk..",
                "...kk...",
                "...kk...",
                "..kwyk..",
                ".kyyyyk.",
                "kkkkkkkk"});
        PixelIcons.register("wifi", new String[]{
                "..kkkk..",
                ".kaaaak.",
                "kakkkkak",
                "kk.kk.kk",
                "..kaak..",
                "...kk...",
                "..kaak..",
                "...kk..."});
        PixelIcons.register("ping", new String[]{
                "........",
                "......kk",
                "......kg",
                "....kkkg",
                "....kgkg",
                "..kkkgkg",
                "..kgkgkg",
                "kkkkkkkk"});
        PixelIcons.register("server", new String[]{
                "kkkkkkkk",
                "kmmmmgmk",
                "kkkkkkkk",
                "kmmmmgmk",
                "kkkkkkkk",
                "kmmmmrmk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("globe", new String[]{
                "..kkkk..",
                ".kbggbk.",
                "kbgggbbk",
                "kbbggbbk",
                "kbbbbggk",
                "kgbbbggk",
                ".kbbbgk.",
                "..kkkk.."});
        PixelIcons.register("battery", new String[]{
                "........",
                "kkkkkkk.",
                "kgggg.kk",
                "kgggg.kk",
                "kgggg.kk",
                "kkkkkkk.",
                "........",
                "........"});
        PixelIcons.register("lightbulb", new String[]{
                "..kkkk..",
                ".kyyyyk.",
                "kyywyyyk",
                "kyyyyyyk",
                ".kyyyyk.",
                "..khhk..",
                "..kmmk..",
                "...kk..."});
        PixelIcons.register("target", new String[]{
                "..kkkk..",
                ".krrrrk.",
                "krwwwwrk",
                "krwrrwrk",
                "krwrrwrk",
                "krwwwwrk",
                ".krrrrk.",
                "..kkkk.."});
        PixelIcons.register("dice", new String[]{
                "kkkkkkkk",
                "kwwwwwwk",
                "kwkwwwwk",
                "kwwwwwwk",
                "kwwwkwwk",
                "kwwwwwwk",
                "kwwwwkwk",
                "kkkkkkkk"});
        PixelIcons.register("puzzle", new String[]{
                "..kk....",
                ".kggk...",
                "kkggkkk.",
                "kggggggk",
                "kgggggkk",
                "kggggggk",
                "kggggggk",
                "kkkkkkkk"});
        PixelIcons.register("ticket", new String[]{
                "kkkkkkkk",
                "kyyyyyyk",
                ".kyoyyk.",
                "kyyoyyyk",
                "kyyoyyyk",
                ".kyoyyk.",
                "kyyyyyyk",
                "kkkkkkkk"});
        PixelIcons.register("cart", new String[]{
                "kk......",
                ".kkkkkkk",
                ".kwwwwwk",
                ".kwwwwk.",
                ".kkkkk..",
                ".k......",
                ".kkkkkk.",
                "..kk.kk."});
        PixelIcons.register("bag", new String[]{
                "..kkkk..",
                ".k....k.",
                "kkkkkkkk",
                "knnnnnnk",
                "knnyynnk",
                "knnnnnnk",
                "knnnnnnk",
                "kkkkkkkk"});
        PixelIcons.register("money", new String[]{
                "kkkkkkkk",
                "keggggek",
                "kgggwggk",
                "kggwegek",
                "kgggwggk",
                "kegwggek",
                "kgggggek",
                "kkkkkkkk"});
        PixelIcons.register("rocket", new String[]{
                "...kk...",
                "..kwwk..",
                "..kwwk..",
                "..kbbk..",
                "..kwwk..",
                ".kkwwkk.",
                "krkwwkrk",
                "kk.oo.kk"});
        PixelIcons.register("camera", new String[]{
                "........",
                "..kkk...",
                "kkkkkkkk",
                "kmmkkmmk",
                "kmkcckmk",
                "kmkcckmk",
                "kmmkkmmk",
                "kkkkkkkk"});
        PixelIcons.register("party", new String[]{
                "k......y",
                "kk.r....",
                "kok...b.",
                "kook.g..",
                "kyook...",
                "kooyok.p",
                "kyoooyk.",
                "kkkkkkkk"});
        PixelIcons.register("sparkle", new String[]{
                "...k....",
                "..kyk...",
                ".kyyyk.k",
                "kyywyyk.",
                ".kyyyk..",
                "..kyk.k.",
                "...k.kyk",
                "......k."});
        PixelIcons.register("thumbs_up", new String[]{
                "...kk...",
                "..ktk...",
                "..ktk...",
                "kkkttkkk",
                "kaktttwk",
                "kaktttkk",
                "kakttttk",
                "kkkkkkk."});
        PixelIcons.register("thumbs_down", new String[]{
                "kkkkkkk.",
                "kakttttk",
                "kaktttkk",
                "kaktttwk",
                "kkkttkkk",
                "..ktk...",
                "..ktk...",
                "...kk..."});
        PixelIcons.register("smile", new String[]{
                "..kkkk..",
                ".kyyyyk.",
                "kykyykyk",
                "kyyyyyyk",
                "kykyykyk",
                "kyykkyyk",
                ".kyyyyk.",
                "..kkkk.."});
        PixelIcons.register("sad", new String[]{
                "..kkkk..",
                ".kyyyyk.",
                "kykyykyk",
                "kyyyyyyk",
                "kyykkyyk",
                "kykyykyk",
                ".kyyyyk.",
                "..kkkk.."});
        PixelIcons.register("angry", new String[]{
                "..kkkk..",
                ".krrrrk.",
                "krkrrkrk",
                "krrkkrrk",
                "krrrrrrk",
                "krrkkrrk",
                ".krrrrk.",
                "..kkkk.."});
        PixelIcons.register("crown", new String[]{
                "........",
                "k..kk..k",
                "kykyykyk",
                "kyyyyyyk",
                "kyryyryk",
                "kyyyyyyk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("trophy", new String[]{
                "kkkkkkkk",
                "kyywyyyk",
                ".kyyyyk.",
                ".kyyyok.",
                "..kyok..",
                "...kk...",
                "..kyyk..",
                ".kkkkkk."});
        PixelIcons.register("medal", new String[]{
                "kbk..krk",
                ".kbkkrk.",
                "..kbrk..",
                "..kkkk..",
                ".kyyyyk.",
                ".kyywyk.",
                ".kyooyk.",
                "..kkkk.."});
        PixelIcons.register("level_up", new String[]{
                "...kk...",
                "..kgek..",
                ".kggeek.",
                "kkkgekkk",
                "..kgek..",
                "kkkkkkkk",
                "kgggggek",
                "kkkkkkkk"});
        PixelIcons.register("skull", new String[]{
                ".kkkkkk.",
                "kwwwwwwk",
                "kwwwwwwk",
                "kkkwwkkk",
                "kkkwwkkk",
                "kwwkkwwk",
                ".kwwwwk.",
                ".kwkwkk."});

        // ---- Предметы ----
        PixelIcons.register("pickaxe", new String[]{
                ".kkkk...",
                "kcccck..",
                ".kkkcck.",
                "...knkck",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("axe", new String[]{
                "...kkk..",
                "..khhhk.",
                "..khhnk.",
                "...knhk.",
                "..knkkk.",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("shovel", new String[]{
                ".....kkk",
                "....khhk",
                "....khhk",
                "...knkk.",
                "..knk...",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("bow", new String[]{
                "..kk..k.",
                ".knk..w.",
                "knk...w.",
                "kn....w.",
                "kn....w.",
                "knk...w.",
                ".knk..w.",
                "..kk..k."});
        PixelIcons.register("arrow", new String[]{
                ".....kkk",
                "......kh",
                ".....khk",
                "....knk.",
                "...knk..",
                "kkknk...",
                "kwkk....",
                "kwk....."});
        PixelIcons.register("trident", new String[]{
                "kck.kck.",
                "kck.kck.",
                "kcckcck.",
                ".kccck..",
                "..kck...",
                "..kck...",
                "..kck...",
                "..kkk..."});
        PixelIcons.register("fishing_rod", new String[]{
                "......kk",
                ".....knk",
                "....knkw",
                "...knk.w",
                "..knk..w",
                ".knk...w",
                "knk...kr",
                "kk....kk"});
        PixelIcons.register("potion", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kwwwwk.",
                "kpwpppkk",
                "kpppppk.",
                "kpppppk.",
                ".kkkkk.."});
        PixelIcons.register("potion_red", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kwwwwk.",
                "krwrrrrk",
                "krrrrrrk",
                "kvrrrrvk",
                ".kkkkkk."});
        PixelIcons.register("potion_blue", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kwwwwk.",
                "kbwbbbbk",
                "kbbbbbbk",
                "kubbbbuk",
                ".kkkkkk."});
        PixelIcons.register("potion_green", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kwwwwk.",
                "kgwggggk",
                "kggggggk",
                "keggggek",
                ".kkkkkk."});
        PixelIcons.register("apple", new String[]{
                "...kk...",
                "..kgkk..",
                ".kkkkkk.",
                "krrrwrrk",
                "krrrrwrk",
                "krrrrrrk",
                ".kvrrvk.",
                "..kkkk.."});
        PixelIcons.register("golden_apple", new String[]{
                "...kk...",
                "..kgkk..",
                ".kkkkkk.",
                "kyyywyyk",
                "kyyyywyk",
                "kyyyyyyk",
                ".koyyok.",
                "..kkkk.."});
        PixelIcons.register("bread", new String[]{
                "........",
                "..kkkkk.",
                ".koooook",
                "kotototk",
                "koooookk",
                "kooooook",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("meat", new String[]{
                "....kkk.",
                "...kvrrk",
                "..kvrrrk",
                ".kvrrrrk",
                ".kvrrrk.",
                "kwkkkk..",
                "kwk.....",
                "kk......"});
        PixelIcons.register("cake", new String[]{
                "..r.r...",
                "..k.k...",
                "kkkkkkkk",
                "kwwwwwwk",
                "krwrwrwk",
                "kttttttk",
                "kttttttk",
                "kkkkkkkk"});
        PixelIcons.register("cookie", new String[]{
                "..kkkk..",
                ".kttttk.",
                "ktnttntk",
                "kttttttk",
                "kttnttnk",
                "ktnttttk",
                ".kttntk.",
                "..kkkk.."});
        PixelIcons.register("fish", new String[]{
                "........",
                "..kkkk..",
                ".kbbbbk.",
                "kbkbbbbk",
                "kbbbbbbk",
                ".kccccbk",
                "..kkkk.k",
                "........"});
        PixelIcons.register("carrot", new String[]{
                "....k.k.",
                ".....kgk",
                "....kkgk",
                "...kook.",
                "..kook..",
                ".kook...",
                "kook....",
                "kk......"});
        PixelIcons.register("diamond", new String[]{
                "........",
                ".kkkkkk.",
                "kccwcckk",
                "kcccccck",
                ".kccccbk",
                "..kccbk.",
                "...kbk..",
                "....k..."});
        PixelIcons.register("emerald", new String[]{
                "...kk...",
                "..kggk..",
                ".kgwgek.",
                "kggggeek",
                "kgggeeek",
                ".kgeeek.",
                "..keek..",
                "...kk..."});
        PixelIcons.register("gold_ingot", new String[]{
                "........",
                "........",
                "..kkkkkk",
                ".kywyyok",
                "kyyyyook",
                "kooooook",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("iron_ingot", new String[]{
                "........",
                "........",
                "..kkkkkk",
                ".kwhhhsk",
                "khhhhssk",
                "kssssssk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("redstone", new String[]{
                "........",
                "...kk...",
                "..krrk.k",
                ".krwrrkk",
                "krrrrvk.",
                ".krvvk..",
                "..kkk...",
                "........"});
        PixelIcons.register("lapis", new String[]{
                "...kk...",
                "..kbbk..",
                ".kbwbbk.",
                "kbbbbbuk",
                ".kbbbuk.",
                "..kbuk..",
                "...kk...",
                "........"});
        PixelIcons.register("coal", new String[]{
                "........",
                "..kkkk..",
                ".kmmhmk.",
                "kmmmmmmk",
                "kmkmmmmk",
                ".kmmmkk.",
                "..kkkk..",
                "........"});
        PixelIcons.register("amethyst", new String[]{
                "....k...",
                "...kpk..",
                "..kplpk.",
                "k.kpppk.",
                "kpkpppk.",
                "kppkpk..",
                ".kpppk..",
                "..kkk..."});
        PixelIcons.register("netherite", new String[]{
                "........",
                "........",
                "..kkkkkk",
                ".kmhmmmk",
                "kmmmmnmk",
                "knnnnnnk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("book", new String[]{
                "kkkkkkk.",
                "krrrrrkk",
                "krryrrkw",
                "krrrrrkw",
                "krrrrrkw",
                "krrrrrkw",
                "kkkkkkkw",
                ".kwwwwww"});
        PixelIcons.register("scroll", new String[]{
                ".kkkkkk.",
                "kttttttk",
                ".kwwwwk.",
                ".kwsswk.",
                ".kwwwwk.",
                ".kwsswk.",
                "kttttttk",
                ".kkkkkk."});
        PixelIcons.register("bucket", new String[]{
                "........",
                ".kkkkkk.",
                "kbbbbbbk",
                "khbbbbsk",
                "khhhhhsk",
                ".khhhsk.",
                ".khsssk.",
                "..kkkk.."});
        PixelIcons.register("torch", new String[]{
                "...kk...",
                "..kyok..",
                "..koyk..",
                "...kk...",
                "..knnk..",
                "..knnk..",
                "..knnk..",
                "..kkkk.."});
        PixelIcons.register("fire", new String[]{
                "...k....",
                "..kok...",
                "..kook.k",
                ".kooyoko",
                "koyyyook",
                "koywyyok",
                "kooyyook",
                ".kkkkkk."});
        PixelIcons.register("tnt", new String[]{
                "kkkkkkkk",
                "krrrrrrk",
                "kwwwwwwk",
                "kwkkkkwk",
                "kwwwwwwk",
                "krrrrrrk",
                "kvrvrvrk",
                "kkkkkkkk"});
        PixelIcons.register("bomb", new String[]{
                "......ok",
                ".....ky.",
                "..kkkk..",
                ".kmmmmk.",
                "kmhmmmmk",
                "kmmmmmmk",
                ".kmmmmk.",
                "..kkkk.."});
        PixelIcons.register("ender_pearl", new String[]{
                "..kkkk..",
                ".keggek.",
                "kegwggek",
                "kggkkgek",
                "keggggek",
                "kegggeek",
                ".keeeek.",
                "..kkkk.."});
        PixelIcons.register("ender_eye", new String[]{
                "..kkkk..",
                ".keggek.",
                "kegyygek",
                "kgykkygk",
                "kgykkygk",
                "kegyygek",
                ".keggek.",
                "..kkkk.."});
        PixelIcons.register("totem", new String[]{
                "..kkkk..",
                ".kyyyyk.",
                ".kgyygk.",
                ".kyyyyk.",
                "kkyookkk",
                "kykyykyk",
                "..kyyk..",
                "..kkkk.."});
        PixelIcons.register("egg", new String[]{
                "..kkkk..",
                ".kwwwwk.",
                ".kwhwwk.",
                "kwwwwwwk",
                "kwwwhwwk",
                "kwwwwwwk",
                ".khwwhk.",
                "..kkkk.."});
        PixelIcons.register("bone", new String[]{
                "kk....kk",
                "kwk..kwk",
                ".kwkkwk.",
                "..kwwk..",
                "..kwwk..",
                ".kwkkwk.",
                "kwk..kwk",
                "kk....kk"});
        PixelIcons.register("feather", new String[]{
                "......kk",
                ".....kwk",
                "....kwhk",
                "...kwhk.",
                "..kwhk..",
                ".kwhk...",
                ".khk....",
                "kk......"});
        PixelIcons.register("chest", new String[]{
                ".kkkkkk.",
                "knnnnnnk",
                "knnnnnnk",
                "kkkyykkk",
                "knnkknnk",
                "knnnnnnk",
                "knnnnnnk",
                "kkkkkkkk"});
        PixelIcons.register("ender_chest", new String[]{
                ".kkkkkk.",
                "kmmmmmmk",
                "kmmmmmmk",
                "kkkggkkk",
                "kmmkkmmk",
                "kmmmmmmk",
                "kmmmmmmk",
                "kkkkkkkk"});
        PixelIcons.register("helmet", new String[]{
                "........",
                ".kkkkkk.",
                "kcwcccck",
                "kcccccck",
                "kcckkcck",
                "kck..kck",
                "kkk..kkk",
                "........"});
        PixelIcons.register("chestplate", new String[]{
                "kkk..kkk",
                "kckkkkck",
                "kcwcccck",
                "kkcccckk",
                ".kcccck.",
                ".kcccck.",
                ".kcccck.",
                ".kkkkkk."});
        PixelIcons.register("boots", new String[]{
                "........",
                ".kkk.kkk",
                ".kck.kck",
                ".kck.kck",
                "kcck.kcc",
                "kcck.kcc",
                "kkkk.kkk",
                "........"});
        PixelIcons.register("elytra", new String[]{
                "kk....kk",
                "kpk..kpk",
                "kppkkppk",
                "kppkkppk",
                "kpk..kpk",
                "kpk..kpk",
                "kk....kk",
                "........"});
        PixelIcons.register("compass", new String[]{
                "..kkkk..",
                ".kshhsk.",
                "kshhhrsk",
                "kshhrhsk",
                "kshbhhsk",
                "ksbhhhsk",
                ".kshhsk.",
                "..kkkk.."});
        PixelIcons.register("map", new String[]{
                "kkkkkkkk",
                "ktgggttk",
                "kgggbbtk",
                "ktgbbbgk",
                "ktbbggtk",
                "kbbgrgtk",
                "ktgggttk",
                "kkkkkkkk"});
        PixelIcons.register("xp_orb", new String[]{
                "..kkkk..",
                ".kgyygk.",
                "kgywyygk",
                "kyyyyygk",
                "kgyyyyyk",
                "kgyyyygk",
                ".kggggk.",
                "..kkkk.."});
        PixelIcons.register("star_nether", new String[]{
                "...kk...",
                "..kwwk..",
                "kkkwwkkk",
                "kwwwwwwk",
                "kkkwwkkk",
                "..khhk..",
                "..kwwk..",
                "...kk..."});
        PixelIcons.register("shield_item", new String[]{
                "kkkkkkkk",
                "knnnnnnk",
                "knhhhhnk",
                "knhnnhnk",
                "knhhhhnk",
                ".knnnnk.",
                "..knnk..",
                "...kk..."});

        // ---- Блоки ----
        PixelIcons.register("grass_block", new String[]{
                "kkkkkkkk",
                "kgggeggk",
                "kegggegk",
                "kngengnk",
                "knnntnnk",
                "kntnnnnk",
                "knnnntnk",
                "kkkkkkkk"});
        PixelIcons.register("dirt", new String[]{
                "kkkkkkkk",
                "knnntnnk",
                "kntnnnnk",
                "knnnnntk",
                "knnntnnk",
                "ktnnnnnk",
                "knnnntnk",
                "kkkkkkkk"});
        PixelIcons.register("stone", new String[]{
                "kkkkkkkk",
                "kssshssk",
                "kshssssk",
                "ksssmssk",
                "kssssshk",
                "kmsssskk",
                "ksshsssk",
                "kkkkkkkk"});
        PixelIcons.register("cobblestone", new String[]{
                "kkkkkkkk",
                "khhmsssk",
                "kshmmhsk",
                "kmmsshmk",
                "khssmmsk",
                "ksmmhssk",
                "khsssmhk",
                "kkkkkkkk"});
        PixelIcons.register("log", new String[]{
                "kkkkkkkk",
                "kttttttk",
                "ktnnnntk",
                "ktnttntk",
                "ktnttntk",
                "ktnnnntk",
                "kttttttk",
                "kkkkkkkk"});
        PixelIcons.register("planks", new String[]{
                "kkkkkkkk",
                "ktttttnk",
                "knnnnnnk",
                "kntttttk",
                "knnnnnnk",
                "ktttnttk",
                "knnnnnnk",
                "kkkkkkkk"});
        PixelIcons.register("sand", new String[]{
                "kkkkkkkk",
                "kyyyyyyk",
                "kyywyyyk",
                "kyyyyoyk",
                "kyoyyyyk",
                "kyyyywyk",
                "kyyyyyyk",
                "kkkkkkkk"});
        PixelIcons.register("water", new String[]{
                "kkkkkkkk",
                "kbbbbbbk",
                "kbccbbbk",
                "kbbbbcck",
                "kubbbbbk",
                "kbbccbbk",
                "kbbbbbuk",
                "kkkkkkkk"});
        PixelIcons.register("lava", new String[]{
                "kkkkkkkk",
                "koooyook",
                "koyoooyk",
                "kooorrok",
                "kroooyok",
                "koyroook",
                "kooooyok",
                "kkkkkkkk"});
        PixelIcons.register("ice", new String[]{
                "kkkkkkkk",
                "kccwccck",
                "kcwccbck",
                "kwccbcck",
                "kccbccwk",
                "kcbccwck",
                "kbccwcck",
                "kkkkkkkk"});
        PixelIcons.register("snow_block", new String[]{
                "kkkkkkkk",
                "kwwwwwwk",
                "kwwhwwwk",
                "kwwwwwhk",
                "kwhwwwwk",
                "kwwwwhwk",
                "kwwwwwwk",
                "kkkkkkkk"});
        PixelIcons.register("leaves", new String[]{
                "kkkkkkkk",
                "keggegek",
                "kgegggek",
                "kgg.eggk",
                "kegggegk",
                "kg.eggek",
                "keggg.gk",
                "kkkkkkkk"});
        PixelIcons.register("glass", new String[]{
                "kkkkkkkk",
                "kc....ck",
                "k.w....k",
                "k..w...k",
                "k......k",
                "k......k",
                "kc....ck",
                "kkkkkkkk"});
        PixelIcons.register("bricks", new String[]{
                "kkkkkkkk",
                "krrrkrrk",
                "khhhhhhk",
                "krkrrrrk",
                "khhhhhhk",
                "krrrkrrk",
                "khhhhhhk",
                "kkkkkkkk"});
        PixelIcons.register("crafting_table", new String[]{
                "kkkkkkkk",
                "kntttnkk",
                "ktkktkkk",
                "knnnnnnk",
                "kntnnsnk",
                "kntnnsnk",
                "knnnnnnk",
                "kkkkkkkk"});
        PixelIcons.register("furnace", new String[]{
                "kkkkkkkk",
                "kssssssk",
                "kshsshsk",
                "kssssssk",
                "kskkkksk",
                "kskookkk",
                "kskyoksk",
                "kkkkkkkk"});
        PixelIcons.register("obsidian", new String[]{
                "kkkkkkkk",
                "kuupuuuk",
                "kuuuupuk",
                "kpuuuuuk",
                "kuuupuuk",
                "kupuuuuk",
                "kuuuuupk",
                "kkkkkkkk"});
        PixelIcons.register("portal", new String[]{
                "kkkkkkkk",
                "kplpppuk",
                "kpplpupk",
                "kpuplppk",
                "kpppulpk",
                "kplpuppk",
                "kpuppplk",
                "kkkkkkkk"});
        PixelIcons.register("diamond_ore", new String[]{
                "kkkkkkkk",
                "ksscsssk",
                "kscwcssk",
                "ksscsssk",
                "ksssscsk",
                "ksssccwk",
                "kssssssk",
                "kkkkkkkk"});
        PixelIcons.register("gold_ore", new String[]{
                "kkkkkkkk",
                "kssysskk",
                "ksywyssk",
                "kssysssk",
                "kssssysk",
                "ksssywyk",
                "kssssssk",
                "kkkkkkkk"});
        PixelIcons.register("bedrock", new String[]{
                "kkkkkkkk",
                "kmmsmmmk",
                "kmsmmhmk",
                "ksmmmmsk",
                "kmmhmsmk",
                "khmmsmmk",
                "kmsmmmsk",
                "kkkkkkkk"});
        PixelIcons.register("cactus", new String[]{
                "kkkkkkkk",
                "kgewgegk",
                "kgeggegk",
                "wgeggegw",
                "kgeggegk",
                "kgeggewk",
                "kgeggegk",
                "kkkkkkkk"});

        // ---- Мобы ----
        PixelIcons.register("creeper", new String[]{
                "kkkkkkkk",
                "kggeggek",
                "kkkggkkk",
                "kkkggkkk",
                "kggkkggk",
                "kgkkkkgk",
                "kgkeekgk",
                "kkkkkkkk"});
        PixelIcons.register("zombie", new String[]{
                "kkkkkkkk",
                "keeeeeek",
                "kggggggk",
                "kkkggkkk",
                "kggggggk",
                "kggeeggk",
                "kgggggek",
                "kkkkkkkk"});
        PixelIcons.register("skeleton", new String[]{
                "kkkkkkkk",
                "khhhhhhk",
                "khwwwwhk",
                "kkkwwkkk",
                "khwwwwhk",
                "khkkkkhk",
                "khwwwwhk",
                "kkkkkkkk"});
        PixelIcons.register("slime", new String[]{
                "kkkkkkkk",
                "kggggggk",
                "kgkkgkkk",
                "kgkkgkkk",
                "kggggggk",
                "kgggkggk",
                "kggggggk",
                "kkkkkkkk"});
        PixelIcons.register("pig", new String[]{
                "kkkkkkkk",
                "kiiiiiik",
                "kkwiiwkk",
                "kiiiiiik",
                "kiikkiik",
                "kiivviik",
                "kiiiiiik",
                "kkkkkkkk"});
        PixelIcons.register("cow", new String[]{
                "kkkkkkkk",
                "knnwwnnk",
                "kwkwwkwk",
                "knnwwnnk",
                "khhhhhhk",
                "khkhhkhk",
                "khhhhhhk",
                "kkkkkkkk"});
        PixelIcons.register("chicken", new String[]{
                "..kkkk..",
                ".kwwwwk.",
                ".kwkwkk.",
                ".kwwwooo",
                ".kwwwrk.",
                "kwwwwwk.",
                "kwwwwwk.",
                ".kkkkk.."});
        PixelIcons.register("sheep", new String[]{
                "kkkkkkkk",
                "kwwwwwwk",
                "kwttttwk",
                "kwkttkwk",
                "kwttttwk",
                "kwtiitwk",
                "kwwwwwwk",
                "kkkkkkkk"});
        PixelIcons.register("spider", new String[]{
                "k.kkkk.k",
                ".kmmmmk.",
                "kkrmmrkk",
                ".kmrrmk.",
                "kkmmmmkk",
                ".kmmmmk.",
                "k.kkkk.k",
                "........"});
        PixelIcons.register("enderman", new String[]{
                "kkkkkkkk",
                "kmmmmmmk",
                "kmmmmmmk",
                "kpkmmkpk",
                "kmmmmmmk",
                "kmmmmmmk",
                "kmmmmmmk",
                "kkkkkkkk"});
        PixelIcons.register("ghast", new String[]{
                ".kkkkkk.",
                "kwwwwwwk",
                "kwkwwkwk",
                "kwwwwwwk",
                "kwwkkwwk",
                "kwwwwwwk",
                "kkwkwkwk",
                ".kk.k.kk"});
        PixelIcons.register("blaze", new String[]{
                "kkkkkkkk",
                "kyyyyyyk",
                "kokyykok",
                "kyyyyyyk",
                "kyokkoyk",
                "kkkkkkkk",
                "oy.ko.yo",
                ".o.y..o."});
        PixelIcons.register("villager", new String[]{
                "kkkkkkkk",
                "kttttttk",
                "kgktkgkk",
                "kttnnttk",
                "ktnnnntk",
                "kttnnttk",
                "knnnnnnk",
                "kkkkkkkk"});
        PixelIcons.register("steve", new String[]{
                "kkkkkkkk",
                "knnnnnnk",
                "knttttnk",
                "ktwbbwtk",
                "kttnnttk",
                "ktnttntk",
                "ktnnnntk",
                "kkkkkkkk"});
        PixelIcons.register("bee", new String[]{
                "..k..k..",
                "...kk...",
                ".kkkkkk.",
                "kyykyyck",
                "kkyykyyk",
                "kyykyyck",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("cat", new String[]{
                "k.....k.",
                "kk...kk.",
                "kokkkok.",
                "kooooook",
                "kgkookgk",
                "koowoook",
                ".kooook.",
                "..kkkk.."});
        PixelIcons.register("wolf", new String[]{
                "kk....kk",
                "khk..khk",
                "khhkkhhk",
                "khhhhhhk",
                "khkhhkhk",
                "khhkkhhk",
                ".khhhhk.",
                "..kkkk.."});
        PixelIcons.register("ghost", new String[]{
                "..kkkk..",
                ".kwwwwk.",
                "kwkwwkwk",
                "kwkwwkwk",
                "kwwwwwwk",
                "kwwwwwwk",
                "kwkwwkwk",
                "kk.kk.kk"});

        // ---- Природа и погода ----
        PixelIcons.register("sun", new String[]{
                "o..kk..o",
                ".kkyykk.",
                ".kyyyyk.",
                "kyyywyyk",
                "kyyyyyyk",
                ".kyyyyk.",
                ".kkyykk.",
                "o..kk..o"});
        PixelIcons.register("moon", new String[]{
                "..kkkk..",
                ".kwwkk..",
                "kwwk....",
                "kwwk....",
                "kwwk....",
                "kwwwk...",
                ".kwwwkk.",
                "..kkkk.."});
        PixelIcons.register("cloud", new String[]{
                "........",
                "...kkk..",
                "..kwwwk.",
                ".kwwwwwk",
                "kwwwwwwk",
                "kwwwwhhk",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("rain", new String[]{
                "...kkk..",
                "..khhhk.",
                ".khhhhhk",
                "khhhhhhk",
                ".kkkkkk.",
                ".b..b..b",
                "b..b..b.",
                "........"});
        PixelIcons.register("snow", new String[]{
                "...k....",
                ".k.w.k..",
                "..kwk...",
                "kwwwwwk.",
                "..kwk...",
                ".k.w.k..",
                "...k....",
                "........"});
        PixelIcons.register("lightning", new String[]{
                "...kkkk.",
                "..kyyk..",
                ".kyyk...",
                "kyyyykk.",
                ".kkyyk..",
                "..kyk...",
                ".kyk....",
                ".kk....."});
        PixelIcons.register("tree", new String[]{
                "..kkkk..",
                ".kggegk.",
                "kgegggek",
                "kggggegk",
                ".kegggk.",
                "..kknk..",
                "...kn...",
                "..kkkk.."});
        PixelIcons.register("flower", new String[]{
                "...kk...",
                "..krrk..",
                ".krykrk.",
                "..krrk..",
                "...kgk..",
                ".kkkgk..",
                ".kgkgk..",
                "..kkgk.."});
        PixelIcons.register("mushroom", new String[]{
                "..kkkk..",
                ".krwrrk.",
                "krrrrwrk",
                "krwrrrrk",
                "kkkkkkkk",
                "..kwwk..",
                "..kwwk..",
                "..kkkk.."});
        PixelIcons.register("wheat", new String[]{
                ".k.k.k..",
                "kykykyk.",
                ".kykyk..",
                "..kyk...",
                "...kk...",
                "..kok...",
                "..kok...",
                "..kkk..."});
        PixelIcons.register("leaf", new String[]{
                "......kk",
                "....kkgk",
                "..kkggek",
                ".kggeggk",
                ".kgegggk",
                "kgeggkk.",
                "kekkk...",
                "kk......"});
        PixelIcons.register("drop", new String[]{
                "...kk...",
                "..kbbk..",
                "..kbbk..",
                ".kbwbbk.",
                "kbwbbbbk",
                "kbbbbbbk",
                ".kbbbbk.",
                "..kkkk.."});
    }
}
