package my.yeamu.pixelui.render;

/**
 * Второй набор встроенных иконок 8×8: мечи и штаны из разных материалов, броня, инструменты,
 * еда, предметы, блоки, мобы и элементы интерфейса. Имена — в docs/PixelUI.md.
 */
final class PixelIconsSet2 {

    private PixelIconsSet2() {}

    static void registerAll() {
        // ---- Оружие, броня и инструменты ----
        PixelIcons.register("sword_wood", new String[]{
                "......kk",
                ".....ktk",
                "....ktnk",
                "kmkktnk.",
                "kkmtnk..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_stone", new String[]{
                "......kk",
                ".....khk",
                "....khsk",
                "kmkkhsk.",
                "kkmhsk..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_iron", new String[]{
                "......kk",
                ".....kwk",
                "....kwhk",
                "kmkkwhk.",
                "kkmwhk..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_gold", new String[]{
                "......kk",
                ".....kyk",
                "....kyok",
                "kmkkyok.",
                "kkmyok..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_diamond", new String[]{
                "......kk",
                ".....kwk",
                "....kwck",
                "kmkkwck.",
                "kkmwck..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_netherite", new String[]{
                "......kk",
                ".....ksk",
                "....ksmk",
                "kmkksmk.",
                "kkmsmk..",
                ".knmk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("sword_magic", new String[]{
                "......kk",
                ".....klk",
                "....klak",
                "kdkklak.",
                "kkdlak..",
                ".kndk...",
                "knkk....",
                "kk......"});
        PixelIcons.register("pants", new String[]{
                "kkkkkkkk",
                "klaaaadk",
                "klaaaadk",
                "klakkadk",
                "klakkadk",
                "klakkadk",
                "klakkadk",
                "kkkkkkkk"});
        PixelIcons.register("leggings_leather", new String[]{
                "kkkkkkkk",
                "ktnnnnnk",
                "ktnnnnnk",
                "ktnkknnk",
                "ktnkknnk",
                "ktnkknnk",
                "ktnkknnk",
                "kkkkkkkk"});
        PixelIcons.register("leggings_chain", new String[]{
                "kkkkkkkk",
                "khssssmk",
                "khssssmk",
                "khskksmk",
                "khskksmk",
                "khskksmk",
                "khskksmk",
                "kkkkkkkk"});
        PixelIcons.register("leggings_iron", new String[]{
                "kkkkkkkk",
                "kwhhhhsk",
                "kwhhhhsk",
                "kwhkkhsk",
                "kwhkkhsk",
                "kwhkkhsk",
                "kwhkkhsk",
                "kkkkkkkk"});
        PixelIcons.register("leggings_gold", new String[]{
                "kkkkkkkk",
                "kwyyyyok",
                "kwyyyyok",
                "kwykkyok",
                "kwykkyok",
                "kwykkyok",
                "kwykkyok",
                "kkkkkkkk"});
        PixelIcons.register("leggings_diamond", new String[]{
                "kkkkkkkk",
                "kwccccbk",
                "kwccccbk",
                "kwckkcbk",
                "kwckkcbk",
                "kwckkcbk",
                "kwckkcbk",
                "kkkkkkkk"});
        PixelIcons.register("leggings_netherite", new String[]{
                "kkkkkkkk",
                "ksmmmmmk",
                "ksmmmmmk",
                "ksmkkmmk",
                "ksmkkmmk",
                "ksmkkmmk",
                "ksmkkmmk",
                "kkkkkkkk"});
        PixelIcons.register("helmet_leather", new String[]{
                "........",
                ".kkkkkk.",
                "kntnnnnk",
                "knnnnnnk",
                "knnkknnk",
                "knk..knk",
                "kkk..kkk",
                "........"});
        PixelIcons.register("chestplate_leather", new String[]{
                "kkk..kkk",
                "knkkkknk",
                "kntnnnnk",
                "kknnnnkk",
                ".knnnnk.",
                ".knnnnk.",
                ".knnnnk.",
                ".kkkkkk."});
        PixelIcons.register("boots_leather", new String[]{
                "........",
                ".kkk.kkk",
                ".knk.knk",
                ".knk.knk",
                "knnk.knn",
                "knnk.knn",
                "kkkk.kkk",
                "........"});
        PixelIcons.register("helmet_iron", new String[]{
                "........",
                ".kkkkkk.",
                "khwhhhhk",
                "khhhhhhk",
                "khhkkhhk",
                "khk..khk",
                "kkk..kkk",
                "........"});
        PixelIcons.register("chestplate_iron", new String[]{
                "kkk..kkk",
                "khkkkkhk",
                "khwhhhhk",
                "kkhhhhkk",
                ".khhhhk.",
                ".khhhhk.",
                ".khhhhk.",
                ".kkkkkk."});
        PixelIcons.register("boots_iron", new String[]{
                "........",
                ".kkk.kkk",
                ".khk.khk",
                ".khk.khk",
                "khhk.khh",
                "khhk.khh",
                "kkkk.kkk",
                "........"});
        PixelIcons.register("helmet_gold", new String[]{
                "........",
                ".kkkkkk.",
                "kywyyyyk",
                "kyyyyyyk",
                "kyykkyyk",
                "kyk..kyk",
                "kkk..kkk",
                "........"});
        PixelIcons.register("chestplate_gold", new String[]{
                "kkk..kkk",
                "kykkkkyk",
                "kywyyyyk",
                "kkyyyykk",
                ".kyyyyk.",
                ".kyyyyk.",
                ".kyyyyk.",
                ".kkkkkk."});
        PixelIcons.register("boots_gold", new String[]{
                "........",
                ".kkk.kkk",
                ".kyk.kyk",
                ".kyk.kyk",
                "kyyk.kyy",
                "kyyk.kyy",
                "kkkk.kkk",
                "........"});
        PixelIcons.register("helmet_netherite", new String[]{
                "........",
                ".kkkkkk.",
                "kmsmmmmk",
                "kmmmmmmk",
                "kmmkkmmk",
                "kmk..kmk",
                "kkk..kkk",
                "........"});
        PixelIcons.register("chestplate_netherite", new String[]{
                "kkk..kkk",
                "kmkkkkmk",
                "kmsmmmmk",
                "kkmmmmkk",
                ".kmmmmk.",
                ".kmmmmk.",
                ".kmmmmk.",
                ".kkkkkk."});
        PixelIcons.register("boots_netherite", new String[]{
                "........",
                ".kkk.kkk",
                ".kmk.kmk",
                ".kmk.kmk",
                "kmmk.kmm",
                "kmmk.kmm",
                "kkkk.kkk",
                "........"});
        PixelIcons.register("pickaxe_wood", new String[]{
                ".kkkk...",
                "kttttk..",
                ".kkkttk.",
                "...knktk",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("pickaxe_stone", new String[]{
                ".kkkk...",
                "kssssk..",
                ".kkkssk.",
                "...knksk",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("pickaxe_iron", new String[]{
                ".kkkk...",
                "khhhhk..",
                ".kkkhhk.",
                "...knkhk",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("pickaxe_gold", new String[]{
                ".kkkk...",
                "kyyyyk..",
                ".kkkyyk.",
                "...knkyk",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("pickaxe_netherite", new String[]{
                ".kkkk...",
                "kmmmmk..",
                ".kkkmmk.",
                "...knkmk",
                "..knk.kk",
                ".knk..k.",
                "knk.....",
                "kk......"});
        PixelIcons.register("axe_gold", new String[]{
                "...kkk..",
                "..kyyyk.",
                "..kyynk.",
                "...knyk.",
                "..knkkk.",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("axe_diamond", new String[]{
                "...kkk..",
                "..kccck.",
                "..kccnk.",
                "...knck.",
                "..knkkk.",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("axe_netherite", new String[]{
                "...kkk..",
                "..kmmmk.",
                "..kmmnk.",
                "...knmk.",
                "..knkkk.",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("shovel_gold", new String[]{
                ".....kkk",
                "....kyyk",
                "....kyyk",
                "...knkk.",
                "..knk...",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("shovel_diamond", new String[]{
                ".....kkk",
                "....kcck",
                "....kcck",
                "...knkk.",
                "..knk...",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("hoe", new String[]{
                "..kkkk..",
                ".khhhhk.",
                "..kkknk.",
                "....knk.",
                "...knk..",
                "..knk...",
                ".knk....",
                ".kk....."});
        PixelIcons.register("hoe_diamond", new String[]{
                "..kkkk..",
                ".kcccck.",
                "..kkknk.",
                "....knk.",
                "...knk..",
                "..knk...",
                ".knk....",
                ".kk....."});
        PixelIcons.register("crossbow", new String[]{
                ".kkkkkk.",
                "knnhhnnk",
                "kk.kk.kk",
                "..kwwk..",
                "..knnk..",
                "..knnk..",
                "..knnk..",
                "..kkkk.."});
        PixelIcons.register("mace", new String[]{
                "....kkk.",
                "...khhhk",
                "...khmhk",
                "...khhhk",
                "..knkkk.",
                ".knk....",
                "knk.....",
                "kk......"});
        PixelIcons.register("dagger", new String[]{
                "...kk...",
                "..kwhk..",
                "..kwhk..",
                "..kwhk..",
                ".kmmmmk.",
                "..knnk..",
                "..kyyk..",
                "...kk..."});
        PixelIcons.register("hammer", new String[]{
                "kkkkkkk.",
                "khhhhsk.",
                "kssssmk.",
                "kkknkkk.",
                "..knk...",
                "..knk...",
                "..knk...",
                "..kkk..."});
        PixelIcons.register("wand", new String[]{
                ".....k..",
                "....kyk.",
                "...kywyk",
                "....kyk.",
                "...knk..",
                "..knk...",
                ".knk....",
                "kk......"});
        PixelIcons.register("staff", new String[]{
                "..kkkk..",
                ".kplppk.",
                ".kppppk.",
                "..kkkk..",
                "...knk..",
                "...knk..",
                "...knk..",
                "...kkk.."});
        PixelIcons.register("swords_crossed", new String[]{
                "kk....kk",
                "kwk..kwk",
                ".kwkkwk.",
                "..kwwk..",
                ".kmkkmk.",
                "kmnkknmk",
                "knk..knk",
                "kk....kk"});

        // ---- Еда ----
        PixelIcons.register("watermelon", new String[]{
                "........",
                "kkkkkkkk",
                "krrkrrrk",
                "krrrrkrk",
                "kwrrrrwk",
                ".kgwwgk.",
                "..kggk..",
                "...kk..."});
        PixelIcons.register("pumpkin", new String[]{
                "...kk...",
                ".kkeekk.",
                "koyooyok",
                "koyooyok",
                "koyooyok",
                "koyooyok",
                ".kooook.",
                "..kkkk.."});
        PixelIcons.register("pumpkin_pie", new String[]{
                "........",
                "..kkkk..",
                ".kttttk.",
                "ktooootk",
                "ktooootk",
                "kttttttk",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("potato", new String[]{
                "........",
                "..kkkk..",
                ".kttttk.",
                "kttntttk",
                "kttttntk",
                "ktnttttk",
                ".kttttk.",
                "..kkkk.."});
        PixelIcons.register("beetroot", new String[]{
                "..kk.kk.",
                ".keekeek",
                "..kkekk.",
                ".kvvvvk.",
                "kvrrrrvk",
                "kvrrrrvk",
                ".kvrrvk.",
                "...kk..."});
        PixelIcons.register("berries", new String[]{
                "...kk...",
                "..kek...",
                ".kkkkkk.",
                "krwkkrwk",
                "krrkkrrk",
                ".kkrwkk.",
                "..krrk..",
                "...kk..."});
        PixelIcons.register("honey", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kwwwwk.",
                "kywyyyyk",
                "kyyyyyyk",
                "koyyyyok",
                ".kkkkkk."});
        PixelIcons.register("milk", new String[]{
                "........",
                ".kkkkkk.",
                "kwwwwwwk",
                "khwwwwsk",
                "khhhhhsk",
                ".khhhsk.",
                ".khsssk.",
                "..kkkk.."});
        PixelIcons.register("golden_carrot", new String[]{
                "....k.k.",
                ".....kgk",
                "....kkgk",
                "...kyyk.",
                "..kyyk..",
                ".kyyk...",
                "kyyk....",
                "kk......"});
        PixelIcons.register("stew", new String[]{
                "........",
                "........",
                "kkkkkkkk",
                "krnnnrnk",
                "kttttttk",
                ".kttttk.",
                "..kkkk..",
                "........"});

        // ---- Предметы ----
        PixelIcons.register("enchanted_book", new String[]{
                "kkkkkkk.",
                "kpppppkk",
                "kppcppkw",
                "kpppppkw",
                "kpppppkw",
                "kpppppkw",
                "kkkkkkkw",
                ".kwwwwww"});
        PixelIcons.register("string", new String[]{
                ".kkkkkk.",
                "kwwwwwwk",
                "kwkkkkk.",
                "kwwwwwwk",
                ".kkkkkwk",
                "kwwwwwwk",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("slimeball", new String[]{
                "........",
                "..kkkk..",
                ".kggggk.",
                "kgwggggk",
                "kggggggk",
                "kgggggek",
                ".keeeek.",
                "..kkkk.."});
        PixelIcons.register("gunpowder", new String[]{
                "........",
                "...kk...",
                "..kssk..",
                ".kshssk.",
                "kssmsssk",
                "kshssmsk",
                "kkkkkkkk",
                "........"});
        PixelIcons.register("blaze_rod", new String[]{
                "......kk",
                ".....kyk",
                "....kyok",
                "...kyok.",
                "..kyok..",
                ".kyok...",
                "kyok....",
                "kkk....."});
        PixelIcons.register("name_tag", new String[]{
                "........",
                "..kkkkkk",
                ".kwwwwwk",
                "kwkwsswk",
                ".kwwwwwk",
                "..kkkkkk",
                "........",
                "........"});
        PixelIcons.register("paper", new String[]{
                ".kkkkkk.",
                ".kwwwwk.",
                ".kwsswk.",
                ".kwwwwk.",
                ".kwsswk.",
                ".kwwwwk.",
                ".kwsswk.",
                ".kkkkkk."});
        PixelIcons.register("spyglass", new String[]{
                ".....kkk",
                "....kcck",
                "...koyck",
                "..koyok.",
                ".koyok..",
                "koyok...",
                "kyok....",
                "kkk....."});
        PixelIcons.register("lantern", new String[]{
                "...kk...",
                "..kmmk..",
                ".kkkkkk.",
                ".kmyymk.",
                ".kyoyyk.",
                ".kmyymk.",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("music_disc", new String[]{
                "..kkkk..",
                ".kmmmmk.",
                "kmmhmmmk",
                "kmmrrmmk",
                "kmmrrmmk",
                "kmmmhmmk",
                ".kmmmmk.",
                "..kkkk.."});
        PixelIcons.register("firework", new String[]{
                "...kk...",
                "..krrk..",
                "..krrk..",
                "..kwwk..",
                "..krrk..",
                "..kwwk..",
                "...nn...",
                "...nn..."});
        PixelIcons.register("snowball", new String[]{
                "........",
                "..kkkk..",
                ".kwwwwk.",
                "kwwhwwwk",
                "kwwwwwhk",
                "khwwwwwk",
                ".khhhhk.",
                "..kkkk.."});
        PixelIcons.register("spawn_egg", new String[]{
                "..kkkk..",
                ".kggggk.",
                ".kgeggk.",
                "kggggegk",
                "kgegggek",
                "kggggggk",
                ".keggek.",
                "..kkkk.."});
        PixelIcons.register("bottle", new String[]{
                "..kkkk..",
                "..knnk..",
                "..kwwk..",
                ".kw..ck.",
                "kw....ck",
                "kc....ck",
                "kcc..cck",
                ".kkkkkk."});
        PixelIcons.register("saddle", new String[]{
                "........",
                ".kk..kk.",
                "knnkknnk",
                "knnnnnnk",
                ".knnnnk.",
                ".kk..kk.",
                ".ks..sk.",
                ".kk..kk."});

        // ---- Блоки ----
        PixelIcons.register("iron_block", new String[]{
                "kkkkkkkk",
                "kwwwwwhk",
                "kwhhhhsk",
                "kwhwhhsk",
                "kwhhhhsk",
                "kwhhwhsk",
                "khsssssk",
                "kkkkkkkk"});
        PixelIcons.register("gold_block", new String[]{
                "kkkkkkkk",
                "kwwwwwyk",
                "kwyyyyok",
                "kwywyyok",
                "kwyyyyok",
                "kwyywyok",
                "kyoooook",
                "kkkkkkkk"});
        PixelIcons.register("diamond_block", new String[]{
                "kkkkkkkk",
                "kwwwwwck",
                "kwccccbk",
                "kwcwccbk",
                "kwccccbk",
                "kwccwcbk",
                "kcbbbbbk",
                "kkkkkkkk"});
        PixelIcons.register("emerald_block", new String[]{
                "kkkkkkkk",
                "kwwwwwgk",
                "kwggggek",
                "kwgwggek",
                "kwggggek",
                "kwggwgek",
                "kgeeeeek",
                "kkkkkkkk"});
        PixelIcons.register("redstone_block", new String[]{
                "kkkkkkkk",
                "kiiiiirk",
                "kirrrrvk",
                "kirirrvk",
                "kirrrrvk",
                "kirrirvk",
                "krvvvvvk",
                "kkkkkkkk"});
        PixelIcons.register("lapis_block", new String[]{
                "kkkkkkkk",
                "kcccccbk",
                "kcbbbbuk",
                "kcbcbbuk",
                "kcbbbbuk",
                "kcbbcbuk",
                "kbuuuuuk",
                "kkkkkkkk"});
        PixelIcons.register("netherrack", new String[]{
                "kkkkkkkk",
                "kvrrvrrk",
                "krrvrrvk",
                "kvrrrvrk",
                "krvrrrvk",
                "krrvrvrk",
                "kvrrrrvk",
                "kkkkkkkk"});
        PixelIcons.register("end_stone", new String[]{
                "kkkkkkkk",
                "kwyywyyk",
                "kyytyywk",
                "kywyyytk",
                "ktyywyyk",
                "kyyyytyk",
                "kwytyywk",
                "kkkkkkkk"});
        PixelIcons.register("glowstone", new String[]{
                "kkkkkkkk",
                "kyowyyok",
                "koyyoyyk",
                "kywoyowk",
                "kyoyyyok",
                "kowyoyyk",
                "kyyoywok",
                "kkkkkkkk"});
        PixelIcons.register("sponge", new String[]{
                "kkkkkkkk",
                "kyyoyyyk",
                "kyokyoyk",
                "kyyyykyk",
                "koyyoyyk",
                "kykyyyok",
                "kyyokyyk",
                "kkkkkkkk"});
        PixelIcons.register("beacon", new String[]{
                "kkkkkkkk",
                "kcccccck",
                "kcmmmmck",
                "kcmwcmck",
                "kcmccmck",
                "kcmmmmck",
                "khhhhhhk",
                "kkkkkkkk"});
        PixelIcons.register("anvil", new String[]{
                "........",
                "kkkkkkkk",
                "khsssssk",
                ".kkssmk.",
                "..ksmk..",
                ".kssssk.",
                "kssssmsk",
                "kkkkkkkk"});
        PixelIcons.register("bookshelf", new String[]{
                "kkkkkkkk",
                "knnnnnnk",
                "krbgykpk",
                "krbgykpk",
                "knnnnnnk",
                "kgyrbpbk",
                "kgyrbpbk",
                "kkkkkkkk"});
        PixelIcons.register("hay_bale", new String[]{
                "kkkkkkkk",
                "kyyoyyok",
                "kyoyyoyk",
                "knnnnnnk",
                "kyyoyyok",
                "koyyoyyk",
                "kyoyyoyk",
                "kkkkkkkk"});
        PixelIcons.register("magma", new String[]{
                "kkkkkkkk",
                "kvvovvvk",
                "kvoyovvk",
                "kvvovvok",
                "kovvvoyk",
                "kvvvvovk",
                "koyovvvk",
                "kkkkkkkk"});

        // ---- Мобы ----
        PixelIcons.register("rabbit", new String[]{
                ".kk..kk.",
                ".kikkik.",
                ".kwkkwk.",
                "kwwwwwwk",
                "kwkwwkwk",
                "kwwiiwwk",
                "kwwwwwwk",
                ".kkkkkk."});
        PixelIcons.register("fox", new String[]{
                "kk....kk",
                "kok..kok",
                "kooooook",
                "kokookok",
                "kwwoowwk",
                ".kwkkwk.",
                "..kwwk..",
                "...kk..."});
        PixelIcons.register("panda", new String[]{
                "kk....kk",
                "kmkkkkmk",
                "kwwwwwwk",
                "kmmwwmmk",
                "kmkwwkmk",
                "kwwkkwwk",
                "kwwwwwwk",
                ".kkkkkk."});
        PixelIcons.register("frog", new String[]{
                ".kk..kk.",
                "kkwkkwkk",
                "kggggggk",
                "kggggggk",
                "kgkkkkgk",
                "kggggggk",
                "kgk..kgk",
                "kk....kk"});
        PixelIcons.register("axolotl", new String[]{
                "........",
                "i.kkkk.i",
                "ikiiiiki",
                "ikkiikki",
                ".kiiiik.",
                ".kivvik.",
                "..kiik..",
                "...kk..."});
        PixelIcons.register("squid", new String[]{
                "..kkkk..",
                ".kuuuuk.",
                ".kwuuwk.",
                ".kkuukk.",
                ".kuuuuk.",
                "kukuukuk",
                "u.u..u.u",
                ".u.uu.u."});
        PixelIcons.register("witch", new String[]{
                "...kk...",
                "..kmpk..",
                ".kmmmpk.",
                "kkkkkkkk",
                "kttttttk",
                "ktkttktk",
                "ktttgttk",
                "kkkkkkkk"});
        PixelIcons.register("iron_golem", new String[]{
                "kkkkkkkk",
                "khehhhhk",
                "khrhhrhk",
                "khhkkhhk",
                "khhnnhhk",
                "khhnnhhk",
                "khhhhhhk",
                "kkkkkkkk"});
        PixelIcons.register("piglin", new String[]{
                "kkkkkkkk",
                "kiiiiiik",
                "kwkiikwk",
                "kiiiiiik",
                "kiivviik",
                "kiivviik",
                "kyiiiiyk",
                "kkkkkkkk"});
        PixelIcons.register("phantom", new String[]{
                "........",
                "k......k",
                "kuk..kuk",
                "kuukkuuk",
                ".kguugk.",
                "..kuuk..",
                "...kk...",
                "........"});

        // ---- Интерфейс ----
        PixelIcons.register("play", new String[]{
                "kk......",
                "kgkk....",
                "kgggkk..",
                "kgggggkk",
                "kgggggkk",
                "kgggkk..",
                "kgkk....",
                "kk......"});
        PixelIcons.register("pause", new String[]{
                "kkk..kkk",
                "kak..kak",
                "kak..kak",
                "kak..kak",
                "kak..kak",
                "kak..kak",
                "kak..kak",
                "kkk..kkk"});
        PixelIcons.register("stop", new String[]{
                "........",
                ".kkkkkk.",
                ".krrrrk.",
                ".krrrrk.",
                ".krrrrk.",
                ".krrrrk.",
                ".kkkkkk.",
                "........"});
        PixelIcons.register("checkbox", new String[]{
                "kkkkkkkk",
                "kwwwwwwk",
                "kwwwwwwk",
                "kwwwwwwk",
                "kwwwwwwk",
                "kwwwwwwk",
                "khhhhhhk",
                "kkkkkkkk"});
        PixelIcons.register("checkbox_checked", new String[]{
                "kkkkkkkk",
                "kwwwwwwk",
                "kwwwwwgk",
                "kwwwwggk",
                "kgwwggwk",
                "kggggwwk",
                "khgghhhk",
                "kkkkkkkk"});
        PixelIcons.register("heart_empty", new String[]{
                ".kk..kk.",
                "kmmkkmmk",
                "kmmmmmmk",
                "kmmmmmmk",
                ".kmmmmk.",
                "..kmmk..",
                "...kk...",
                "........"});
        PixelIcons.register("heart_half", new String[]{
                ".kk..kk.",
                "krrkkmmk",
                "krwrmmmk",
                "krrrmmmk",
                ".krrmmk.",
                "..krmk..",
                "...kk...",
                "........"});
        PixelIcons.register("star_empty", new String[]{
                "...kk...",
                "..kmmk..",
                "kkkmskkk",
                "kmmmmmmk",
                ".kmmmmk.",
                ".kmmmmk.",
                "kmmkkmmk",
                "kkk..kkk"});
        PixelIcons.register("crosshair", new String[]{
                "...kk...",
                "...ww...",
                "........",
                "kw....wk",
                "kw....wk",
                "........",
                "...ww...",
                "...kk..."});
    }
}
