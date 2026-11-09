package my.yeamu.modsync.gui;

import my.yeamu.modsync.SyncLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Проверка сборки ModGate прямо на экране — вместо сообщений в чате.
 * Пиксельная плашка сверху по центру:
 *  — «Проверка сборки…» пока ждём ответ сервера;
 *  — «Сборка в порядке» на пару секунд, если всё хорошо (и больше ничего);
 *  — красная плашка с отсчётом до отключения + окно с причиной, если проверка не пройдена.
 * Все методы вызываются в потоке клиента.
 */
public final class GateOverlay {

    public enum State { NONE, CHECKING, OK, FAIL }

    private static final long ANIM = 240, CHECK_TIMEOUT = 15_000, OK_SHOW = 2_400, WARN_SHOW = 10_000;

    private static State state = State.NONE;
    private static long since;
    private static String reason = "";
    private static String mode = "strict";
    private static String url = "https://yeamu.my.id";
    private static String text = "";
    private static long deadline;
    private static boolean screenShown;

    private GateOverlay() {}

    private static long now() { return System.currentTimeMillis(); }

    /** Сервер с ModGate объявил канал — начинаем показывать «проверку». */
    public static void onChecking() {
        if (state != State.NONE) return;
        state = State.CHECKING;
        since = now();
    }

    /** Итог от плагина: "ok" или код причины. */
    public static void onResult(String result) {
        if ("ok".equalsIgnoreCase(result)) {
            boolean wasFail = state == State.FAIL;
            state = State.OK;
            since = now();
            screenShown = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (wasFail && mc.currentScreen instanceof GateScreen) mc.setScreen(null);
            return;
        }
        // Подробности придут отдельным сообщением "gate"; это запасной вариант для старых плагинов
        if (state != State.FAIL) {
            fail(result, "strict", 0, url, defaultText(result));
        }
    }

    /** Подробности отказа: причина, режим, секунды до кика, сайт и текст причины (через перевод строки). */
    public static void onGate(String payload) {
        String[] p = payload.split("\n", 5);
        String r = p.length > 0 ? p[0] : "no_mod";
        String m = p.length > 1 ? p[1] : "strict";
        int sec = 0;
        try { sec = p.length > 2 ? Integer.parseInt(p[2].trim()) : 0; } catch (NumberFormatException ignored) {}
        String u = p.length > 3 && p[3].startsWith("https://") ? p[3] : url;
        String t = p.length > 4 && !p[4].isBlank() ? p[4] : defaultText(r);
        fail(r, m, sec, u, t);
    }

    private static void fail(String r, String m, int seconds, String u, String t) {
        boolean fresh = state != State.FAIL;
        state = State.FAIL;
        if (fresh) since = now();
        reason = r;
        mode = m;
        url = u;
        text = t;
        deadline = seconds > 0 ? now() + seconds * 1000L : 0;
        SyncLog.info("ModGate: проверка не пройдена — " + r + " (" + t + ")");
    }

    public static void reset() {
        state = State.NONE;
        screenShown = false;
        deadline = 0;
    }

    private static String defaultText(String r) {
        return switch (r == null ? "" : r) {
            case "outdated_mod" -> "ModSync устарел — обновите его.";
            case "outdated_manifest" -> "Сборка модов устарела — нажмите «Обновить моды».";
            case "foreign_mods" -> "Не хватает модов сборки или они не той версии.";
            case "bad_handshake" -> "Клиент прислал повреждённые данные.";
            default -> "Мод ModSync не найден.";
        };
    }

    // ------------------------------------------------------------------ данные для GateScreen

    public static String text() { return text; }
    public static String url() { return url; }
    public static boolean isWarn() { return "warn".equalsIgnoreCase(mode); }
    public static boolean isFailed() { return state == State.FAIL; }

    /** Сколько секунд осталось до отключения (-1 — неизвестно). */
    public static int secondsLeft() {
        if (deadline <= 0) return -1;
        return (int) Math.max(0, (deadline - now() + 999) / 1000);
    }

    // ------------------------------------------------------------------ тик и отрисовка

    /** Открывает окно с причиной один раз, когда игрок не в другом меню. */
    public static void tick(MinecraftClient mc) {
        if (state == State.FAIL && !screenShown && mc.player != null && mc.currentScreen == null) {
            screenShown = true;
            mc.setScreen(new GateScreen());
        }
    }

    public static void render(DrawContext c, RenderTickCounter tick) {
        if (state == State.NONE) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        long t = now() - since;
        String label;
        String[] icon;
        int accent;
        float out = 0f;
        switch (state) {
            case CHECKING -> {
                if (t > CHECK_TIMEOUT) { state = State.NONE; return; }
                label = "ModSync · проверка сборки" + "...".substring(0, (int) (t / 350 % 4));
                icon = Pixel.HOURGLASS[(int) (t / 500 % 2)];
                accent = 0xFFFFC83D;
            }
            case OK -> {
                if (t > OK_SHOW) { state = State.NONE; return; }
                label = "Сборка в порядке — приятной игры!";
                icon = Pixel.CHECK;
                accent = 0xFF5BE34A;
                if (t > OK_SHOW - ANIM) out = (t - (OK_SHOW - ANIM)) / (float) ANIM;
            }
            default -> {
                if (isWarn()) {
                    if (t > WARN_SHOW) { state = State.NONE; return; }
                    if (t > WARN_SHOW - ANIM) out = (t - (WARN_SHOW - ANIM)) / (float) ANIM;
                    label = "Сборка не совпадает — лучше обновиться";
                    icon = Pixel.WARN;
                    accent = 0xFFFFC83D;
                } else {
                    int left = secondsLeft();
                    label = left >= 0 ? "Проверка не пройдена · отключение через " + left + " с" : "Проверка не пройдена";
                    icon = Pixel.CROSS;
                    // В последние 5 секунд плашка мигает
                    accent = left >= 0 && left <= 5 && (now() / 250) % 2 == 0 ? 0xFFFFFFFF : 0xFFFF4B4B;
                }
            }
        }

        float shown = Pixel.easeOut(t / (float) ANIM) * (1f - Pixel.easeOut(out));
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(state == State.CHECKING ? "ModSync · проверка сборки..." : label) + 34;
        int h = 22;
        int x = (c.getScaledWindowWidth() - w) / 2;
        int y = 6 - Pixel.snap((1f - shown) * (h + 10));

        Pixel.panel(c, x, y, w, h, 0xF0262626, 0xFF4E4E4E, 0xFF111111, 1f);
        c.fill(x + 2, y + 2, x + 4, y + h - 2, accent); // цветная полоска слева
        Pixel.icon(c, icon, x + 7, y + 3, 2, 1f);
        c.drawTextWithShadow(tr, label, x + 28, y + 7, 0xFFFFFFFF);
    }
}
