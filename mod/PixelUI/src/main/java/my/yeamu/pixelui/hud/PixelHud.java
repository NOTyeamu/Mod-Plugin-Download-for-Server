package my.yeamu.pixelui.hud;

import my.yeamu.pixelui.api.Banner;
import my.yeamu.pixelui.api.Notification;
import my.yeamu.pixelui.api.Position;
import my.yeamu.pixelui.api.Theme;
import my.yeamu.pixelui.render.Pixel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Слой HUD: стопки уведомлений в углах/центре и плашки.
 * Анимации: выезд с лёгким перелётом, уезд + схлопывание, плавное смещение соседей.
 * Сдвиги округляются до 2 пикселей, чтобы картинка оставалась «пиксельной».
 * Все методы — только из потока клиента.
 */
public final class PixelHud {

    private static final long IN = 280, OUT = 220;
    private static final int MAX_VISIBLE = 5, MARGIN = 6, GAP = 4, MAX_W = 210;

    private static final class Toast {
        Notification n;
        final boolean server;
        long born, start, expire, closing = -1;
        float y = Float.NaN;
        boolean dirty = true, hasIcon;
        List<OrderedText> titleLines = List.of(), lines = List.of();
        int w, h, textX;

        Toast(Notification n, boolean server) { this.n = n; this.server = server; }
    }

    private static final class Bar {
        Banner b;
        final boolean server;
        long born, expire, closing = -1;
        float y = Float.NaN, shownProgress = -1;

        Bar(Banner b, boolean server) { this.b = b; this.server = server; }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final List<Bar> BARS = new ArrayList<>();
    private static long lastFrame;
    /** Сколько пикселей сверху заняли плашки в прошлом кадре (вопросы встают ниже). */
    public static int topUsed;

    private PixelHud() {}

    private static long now() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ управление

    public static void notify(Notification n, boolean server) {
        long now = now();
        if (n.id != null && !n.id.isBlank()) {
            for (Toast t : TOASTS) {
                if (n.id.equals(t.n.id) && t.closing < 0) { // тот же id — обновляем на месте
                    t.n = n;
                    t.start = now;
                    t.expire = now + n.durationMs;
                    t.dirty = true;
                    Pixel.sound(n.sound, n.pitch);
                    return;
                }
            }
        }
        Toast t = new Toast(n, server);
        t.born = t.start = now;
        t.expire = now + n.durationMs;
        TOASTS.add(t);
        while (TOASTS.size() > 40) TOASTS.remove(0);
        Pixel.sound(n.sound, n.pitch);
    }

    public static void banner(Banner b, boolean server) {
        long now = now();
        for (Bar bar : BARS) {
            if (bar.b.id.equals(b.id) && bar.closing < 0) {
                bar.b = b;
                bar.expire = b.durationMs > 0 ? now + b.durationMs : Long.MAX_VALUE;
                return;
            }
        }
        Bar bar = new Bar(b, server);
        bar.born = now;
        bar.expire = b.durationMs > 0 ? now + b.durationMs : Long.MAX_VALUE;
        BARS.add(bar);
        while (BARS.size() > 12) BARS.remove(0);
        Pixel.sound(b.sound, 1f);
    }

    /** null — убрать все плашки. */
    public static void removeBanner(String id) {
        long now = now();
        for (Bar bar : BARS) {
            if ((id == null || bar.b.id.equals(id)) && bar.closing < 0) bar.closing = now;
        }
    }

    /** onlyServer = true — убрать только то, что прислал сервер (при выходе с сервера). */
    public static void clear(boolean onlyServer) {
        long now = now();
        for (Toast t : TOASTS) if ((!onlyServer || t.server) && t.closing < 0) t.closing = now;
        for (Bar b : BARS) if ((!onlyServer || b.server) && b.closing < 0) b.closing = now;
    }

    // ------------------------------------------------------------------ отрисовка

    public static void render(DrawContext c, RenderTickCounter tickCounter) {
        long now = now();
        float dt = Math.min(0.1f, Math.max(0f, (now - lastFrame) / 1000f));
        lastFrame = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (TOASTS.isEmpty() && BARS.isEmpty()) { topUsed = 0; return; }
        if (mc.options.hudHidden) return;
        float follow = 1f - (float) Math.exp(-dt * 14f); // плавное «догоняние» целевой позиции
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        TextRenderer tr = mc.textRenderer;
        topUsed = renderBars(c, tr, sw, sh, now, follow);
        renderToasts(c, tr, sw, sh, now, follow, topUsed);
    }

    private static int renderBars(DrawContext c, TextRenderer tr, int sw, int sh, long now, float follow) {
        int topCursor = MARGIN;
        int bottomCursor = sh - 62; // над хотбаром, сердцами и опытом
        int centerCursor = sh / 2 - 40;
        List<Bar> dead = new ArrayList<>();
        for (Bar bar : BARS) {
            Banner b = bar.b;
            if (bar.closing < 0 && now >= bar.expire) bar.closing = now;
            float outP = bar.closing < 0 ? 0f : (now - bar.closing) / (float) OUT;
            if (outP >= 1f) { dead.add(bar); continue; }
            float inP = (now - bar.born) / (float) IN;
            float shown = Pixel.easeOutBack(inP) * (1f - Pixel.easeIn(outP));
            float a = bar.closing < 0 ? Pixel.clamp01(inP * 3f) : 1f - outP;

            Text label = Pixel.text(b.text);
            boolean icon = Pixel.hasIcon(b.icon);
            boolean prog = b.progress >= 0f;
            int w = Math.max(100, Math.min(tr.getWidth(label) + (icon ? 26 : 0) + 18, (int) (sw * 0.8f)));
            int h = 20 + (prog ? 4 : 0);
            float contribution = (h + GAP) * (bar.closing < 0 ? 1f : 1f - Pixel.easeOut(outP));

            float target;
            int slide;
            if (b.position == Position.TOP_CENTER) {
                target = topCursor;
                topCursor += Math.round(contribution);
                slide = -Pixel.snap((1f - shown) * (h + 10));
            } else if (b.position == Position.BOTTOM_CENTER) {
                target = bottomCursor - h;
                bottomCursor -= Math.round(contribution);
                slide = Pixel.snap((1f - shown) * (h + 10));
            } else {
                target = centerCursor;
                centerCursor += Math.round(contribution);
                slide = 0;
            }
            bar.y = Float.isNaN(bar.y) ? target : bar.y + (target - bar.y) * follow;
            float tp = Pixel.clamp01(b.progress);
            bar.shownProgress = bar.shownProgress < 0 ? tp : bar.shownProgress + (tp - bar.shownProgress) * follow;

            int x = (sw - w) / 2;
            int y = Math.round(bar.y) + slide;
            Theme th = b.theme;
            Pixel.panel(c, x, y, w, h, th, a);
            int contentX = x + 6;
            if (icon) {
                // Блок с иконкой — как «слот» слева
                c.fill(x + 2, y + 2, x + 21, y + h - 2, Pixel.alpha(Pixel.mix(th.accent(), 0xFF000000, 0.62f), a));
                c.fill(x + 21, y + 2, x + 22, y + h - 2, Pixel.alpha(th.outline(), a));
                Pixel.icon(c, b.icon, x + 4, y + 2 + (h - 4 - 16) / 2, 2, th.accent(), a);
                contentX = x + 26;
            }
            int tw = tr.getWidth(label);
            int tx = contentX + Math.max(0, (x + w - 6 - contentX - tw) / 2);
            c.drawText(tr, label, tx, y + 6, Pixel.alpha(th.title(), a), th.shadow());
            if (prog) {
                int bx1 = contentX, bx2 = x + w - 6, by = y + h - 7;
                c.fill(bx1, by, bx2, by + 3, Pixel.alpha(th.dark(), a));
                int fill = Math.round((bx2 - bx1) * bar.shownProgress);
                if (fill > 0) {
                    c.fill(bx1, by, bx1 + fill, by + 3, Pixel.alpha(th.accent(), a));
                    c.fill(bx1, by, bx1 + fill, by + 1, Pixel.alpha(Pixel.mix(th.accent(), 0xFFFFFFFF, 0.4f), a));
                }
            }
        }
        BARS.removeAll(dead);
        return topCursor - MARGIN;
    }

    private static void layout(Toast t, TextRenderer tr) {
        Notification n = t.n;
        t.hasIcon = Pixel.hasIcon(n.icon);
        t.textX = t.hasIcon ? 8 + 16 + 6 : 9;
        int maxW = n.width > 0 ? Math.max(100, Math.min(n.width, 300)) : MAX_W;
        int wrapW = maxW - t.textX - 8;
        List<OrderedText> title = n.title.isEmpty() ? List.of() : tr.wrapLines(Pixel.text(n.title), wrapW);
        if (title.size() > 2) title = title.subList(0, 2);
        List<OrderedText> lines = new ArrayList<>();
        if (!n.text.isEmpty()) {
            for (String part : n.text.replace("\\n", "\n").split("\n", -1)) {
                if (part.isEmpty()) lines.add(OrderedText.EMPTY);
                else lines.addAll(tr.wrapLines(Pixel.text(part), wrapW));
            }
        }
        if (lines.size() > 8) lines = new ArrayList<>(lines.subList(0, 8));
        int content = 0;
        for (OrderedText o : title) content = Math.max(content, tr.getWidth(o));
        for (OrderedText o : lines) content = Math.max(content, tr.getWidth(o));
        t.titleLines = title;
        t.lines = lines;
        t.w = n.width > 0 ? maxW : Math.max(110, Math.min(t.textX + content + 9, maxW));
        int h = 6 + title.size() * 10 + (!title.isEmpty() && !lines.isEmpty() ? 2 : 0) + lines.size() * 10 + 4
                + (n.progressBar ? 4 : 0);
        t.h = Math.max(h, t.hasIcon ? 30 : 20);
        t.dirty = false;
    }

    private static void renderToasts(DrawContext c, TextRenderer tr, int sw, int sh, long now, float follow, int topUsed) {
        List<Toast> dead = new ArrayList<>();
        for (Position p : Position.values()) {
            // Собираем тосты этой позиции (старые — ближе к краю экрана)
            List<Toast> list = new ArrayList<>();
            for (Toast t : TOASTS) if (t.n.position == p) list.add(t);
            if (list.isEmpty()) continue;
            for (Toast t : list) if (t.dirty) layout(t, tr);

            float cursor;
            if (p.isTop()) cursor = MARGIN + (p == Position.TOP_CENTER ? topUsed : 0);
            else if (p.isBottom()) cursor = sh - MARGIN - (p == Position.BOTTOM_CENTER ? 60 : 0);
            else {
                float total = 0;
                int k = 0;
                for (Toast t : list) if (k++ < MAX_VISIBLE) total += t.h + GAP;
                cursor = (sh - total) / 2f;
            }

            int visible = 0;
            for (Toast t : list) {
                if (visible >= MAX_VISIBLE && t.closing < 0) {
                    // В очереди: таймер не идёт, пока уведомление не показано
                    t.born = t.start = now;
                    t.expire = now + t.n.durationMs;
                    t.y = Float.NaN;
                    continue;
                }
                if (t.closing < 0 && now >= t.expire) t.closing = now;
                float outP = t.closing < 0 ? 0f : (now - t.closing) / (float) OUT;
                if (outP >= 1f) { dead.add(t); continue; }
                float inP = (now - t.born) / (float) IN;
                float shown = Pixel.easeOutBack(inP) * (1f - Pixel.easeIn(outP));
                float a = t.closing < 0 ? Pixel.clamp01(inP * 3f) : 1f - outP;
                float contribution = (t.h + GAP) * (t.closing < 0 ? 1f : 1f - Pixel.easeOut(outP));

                float target = p.isBottom() ? cursor - t.h : cursor;
                t.y = Float.isNaN(t.y) ? target : t.y + (target - t.y) * follow;

                int off = Pixel.snap((1f - shown) * (t.w + MARGIN + 6));
                int x, y = Math.round(t.y);
                if (p.isLeft()) x = MARGIN - off;
                else if (p.isRight()) x = sw - t.w - MARGIN + off;
                else {
                    x = (sw - t.w) / 2;
                    int v = Pixel.snap((1f - shown) * (t.h + 12));
                    if (p.isTop()) y -= v;
                    else if (p.isBottom()) y += v;
                }
                draw(c, tr, t, x, y, a, now);
                if (p.isBottom()) cursor -= contribution;
                else cursor += contribution;
                visible++;
            }
        }
        TOASTS.removeAll(dead);
    }

    private static void draw(DrawContext c, TextRenderer tr, Toast t, int x, int y, float a, long now) {
        Notification n = t.n;
        Theme th = n.theme;
        Pixel.panel(c, x, y, t.w, t.h, th, a);
        // Акцентная полоска слева
        c.fill(x + 2, y + 2, x + 4, y + t.h - 2, Pixel.alpha(th.accent(), a));
        int textRows = t.titleLines.size() + t.lines.size();
        if (t.hasIcon) {
            int iy = textRows <= 2 ? y + (t.h - (n.progressBar ? 4 : 0) - 16) / 2 : y + 6;
            Pixel.icon(c, n.icon, x + 8, iy, 2, th.accent(), a);
        }
        int ty = y + 6;
        if (t.titleLines.isEmpty() && t.lines.size() == 1) ty = y + (t.h - (n.progressBar ? 4 : 0) - 8) / 2;
        for (OrderedText line : t.titleLines) {
            c.drawText(tr, line, x + t.textX, ty, Pixel.alpha(th.title(), a), th.shadow());
            ty += 10;
        }
        if (!t.titleLines.isEmpty() && !t.lines.isEmpty()) ty += 2;
        for (OrderedText line : t.lines) {
            c.drawText(tr, line, x + t.textX, ty, Pixel.alpha(th.text(), a), th.shadow());
            ty += 10;
        }
        if (n.progressBar) {
            float rem = Pixel.clamp01(1f - (now - t.start) / (float) n.durationMs);
            int bx1 = x + 6, bx2 = x + t.w - 6, by = y + t.h - 6;
            c.fill(bx1, by, bx2, by + 2, Pixel.alpha(th.dark(), a));
            int fill = Pixel.snap((bx2 - bx1) * rem);
            if (fill > 0) c.fill(bx1, by, bx1 + fill, by + 2, Pixel.alpha(th.accent(), a));
        }
    }
}
