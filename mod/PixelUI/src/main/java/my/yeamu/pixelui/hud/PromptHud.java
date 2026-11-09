package my.yeamu.pixelui.hud;

import my.yeamu.pixelui.api.Position;
import my.yeamu.pixelui.api.Prompt;
import my.yeamu.pixelui.api.PromptAnswer;
import my.yeamu.pixelui.api.Theme;
import my.yeamu.pixelui.render.Pixel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Вопросы-плашки «Да (Y) / Нет (N)».
 * Показываем один вопрос за раз, остальные в очереди (счётчик «+2» в углу).
 * Клавиши читаем напрямую через GLFW и только когда не открыт ни один экран (чат, инвентарь),
 * чтобы набор текста в чате не отвечал на вопрос. Удерживаемая до показа клавиша не считается.
 * Все методы — только из потока клиента.
 */
public final class PromptHud {

    private static final long IN = 300, OUT = 240, PRESS_FLASH = 220, ARM_DELAY = 250;

    private static final class Entry {
        Prompt p;
        final boolean server;
        final Consumer<PromptAnswer> callback;
        long shown = -1, closing = -1, answeredAt = -1;
        PromptAnswer answer;
        float y = Float.NaN;

        Entry(Prompt p, boolean server, Consumer<PromptAnswer> callback) {
            this.p = p;
            this.server = server;
            this.callback = callback;
        }
    }

    private static final Deque<Entry> QUEUE = new ArrayDeque<>();
    /** Уходящие (анимация исчезновения). */
    private static final List<Entry> LEAVING = new ArrayList<>();
    private static boolean yesWasDown = true, noWasDown = true;
    private static long lastFrame;

    private PromptHud() {}

    private static long now() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ управление

    /** Показать вопрос. Тот же id заменяет старый вопрос (старый получает CANCELLED). */
    public static void show(Prompt p, boolean server, Consumer<PromptAnswer> callback) {
        for (Iterator<Entry> it = QUEUE.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (e.server == server && e.p.id.equals(p.id)) {
                if (e == QUEUE.peekFirst() && e.shown >= 0) {
                    // Активный вопрос обновляем на месте, таймер начинается заново
                    fire(e, PromptAnswer.CANCELLED);
                    Entry n = new Entry(p, server, callback);
                    n.shown = now();
                    n.y = e.y;
                    it.remove();
                    QUEUE.addFirst(n);
                    Pixel.sound(p.sound, 1f);
                    return;
                }
                fire(e, PromptAnswer.CANCELLED);
                it.remove();
                break;
            }
        }
        QUEUE.addLast(new Entry(p, server, callback));
        while (QUEUE.size() > 16) {
            Entry old = QUEUE.pollLast();
            fire(old, PromptAnswer.CANCELLED);
        }
    }

    /** Отменить вопрос по id (null — все вопросы этого источника). */
    public static void cancel(String id, boolean server, boolean notify) {
        for (Iterator<Entry> it = QUEUE.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (e.server != server || (id != null && !e.p.id.equals(id))) continue;
            it.remove();
            if (notify) fire(e, PromptAnswer.CANCELLED);
            else e.answer = PromptAnswer.CANCELLED;
            if (e.shown >= 0) { e.closing = now(); LEAVING.add(e); }
        }
    }

    /** onlyServer = true — убрать только серверные (без ответа серверу). */
    public static void clear(boolean onlyServer) {
        cancel(null, true, false);
        if (!onlyServer) cancel(null, false, true);
    }

    public static boolean active() {
        return !QUEUE.isEmpty();
    }

    private static void fire(Entry e, PromptAnswer a) {
        if (e.answer != null) return;
        e.answer = a;
        try {
            if (e.callback != null) e.callback.accept(a);
        } catch (Exception ex) {
            my.yeamu.pixelui.PixelUIClient.LOG.warn("PixelUI: ошибка в обработчике вопроса {}: {}", e.p.id, ex.toString());
        }
    }

    private static void answer(Entry e, PromptAnswer a) {
        QUEUE.remove(e);
        fire(e, a);
        long now = now();
        e.answeredAt = now;
        // После нажатия клавиша коротко «подсвечивается», потом плашка уезжает
        e.closing = a == PromptAnswer.YES || a == PromptAnswer.NO ? now + PRESS_FLASH : now;
        LEAVING.add(e);
        if (a == PromptAnswer.YES) Pixel.sound("minecraft:ui.button.click", 1.2f);
        else if (a == PromptAnswer.NO) Pixel.sound("minecraft:ui.button.click", 0.8f);
    }

    // ------------------------------------------------------------------ клавиши (каждый тик)

    public static void tick(MinecraftClient mc) {
        Entry e = QUEUE.peekFirst();
        if (e == null || e.shown < 0) {
            yesWasDown = noWasDown = true; // пока вопроса нет — требуем отпускания клавиш
            return;
        }
        long now = now();
        if (e.p.timeoutMs > 0 && now - e.shown >= e.p.timeoutMs) {
            answer(e, PromptAnswer.TIMEOUT);
            return;
        }
        boolean canPress = mc.currentScreen == null && mc.isWindowFocused() && mc.getWindow() != null;
        boolean yesDown = canPress && down(mc, e.p.yesKey);
        boolean noDown = canPress && down(mc, e.p.noKey);
        boolean armed = now - e.shown >= ARM_DELAY;
        if (armed && yesDown && !yesWasDown) answer(e, PromptAnswer.YES);
        else if (armed && noDown && !noWasDown) answer(e, PromptAnswer.NO);
        yesWasDown = yesDown;
        noWasDown = noDown;
    }

    private static boolean down(MinecraftClient mc, char key) {
        // Коды GLFW для A–Z и 0–9 совпадают с ASCII
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    // ------------------------------------------------------------------ отрисовка

    public static void render(DrawContext c, RenderTickCounter tickCounter) {
        long now = now();
        float dt = Math.min(0.1f, Math.max(0f, (now - lastFrame) / 1000f));
        lastFrame = now;
        if (QUEUE.isEmpty() && LEAVING.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Entry head = QUEUE.peekFirst();
        if (head != null && head.shown < 0) {
            head.shown = now;
            Pixel.sound(head.p.sound, 1f);
        }
        if (mc.options.hudHidden) return;
        float follow = 1f - (float) Math.exp(-dt * 14f);
        TextRenderer tr = mc.textRenderer;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        List<Entry> dead = new ArrayList<>();
        for (Entry e : LEAVING) {
            float outP = e.closing > now ? 0f : (now - e.closing) / (float) OUT;
            if (outP >= 1f) { dead.add(e); continue; }
            draw(c, tr, e, sw, sh, now, follow, outP, 0);
        }
        LEAVING.removeAll(dead);
        if (head != null) draw(c, tr, head, sw, sh, now, follow, 0f, QUEUE.size() - 1);
    }

    private static void draw(DrawContext c, TextRenderer tr, Entry e, int sw, int sh, long now,
                             float follow, float outP, int queued) {
        Prompt p = e.p;
        Theme th = p.theme;
        float inP = (now - e.shown) / (float) IN;
        float shown = Pixel.easeOutBack(inP) * (1f - Pixel.easeIn(outP));
        float a = outP > 0 ? 1f - outP : Pixel.clamp01(inP * 3f);

        // --- разметка
        boolean icon = Pixel.hasIcon(p.icon);
        int textX = icon ? 30 : 9;
        Text yesL = Pixel.text(p.yesLabel), noL = Pixel.text(p.noLabel);
        int keysW = 16 + 4 + tr.getWidth(yesL) + 14 + 16 + 4 + tr.getWidth(noL);
        int maxW = p.width > 0 ? p.width : 300;
        int wrap = maxW - textX - 9;
        List<OrderedText> q = p.question.isEmpty() ? List.of() : tr.wrapLines(Pixel.text(p.question), wrap);
        if (q.size() > 3) q = q.subList(0, 3);
        List<OrderedText> lines = new ArrayList<>();
        if (!p.text.isEmpty()) {
            for (String part : p.text.replace("\\n", "\n").split("\n", -1)) {
                if (part.isEmpty()) lines.add(OrderedText.EMPTY);
                else lines.addAll(tr.wrapLines(Pixel.text(part), wrap));
            }
        }
        if (lines.size() > 5) lines = new ArrayList<>(lines.subList(0, 5));
        int content = keysW;
        for (OrderedText o : q) content = Math.max(content, tr.getWidth(o));
        for (OrderedText o : lines) content = Math.max(content, tr.getWidth(o));
        int w = p.width > 0 ? p.width : Math.max(170, Math.min(textX + content + 9, maxW));
        int textH = q.size() * 10 + (!q.isEmpty() && !lines.isEmpty() ? 2 : 0) + lines.size() * 10;
        int h = 7 + textH + 6 + 18 + 6 + (p.timerBar && p.timeoutMs > 0 ? 4 : 0);

        // --- позиция (верх — под плашками PixelHud)
        float target;
        int slide;
        if (p.position == Position.TOP_CENTER) {
            target = 6 + PixelHud.topUsed;
            slide = -Pixel.snap((1f - shown) * (h + 12));
        } else if (p.position == Position.BOTTOM_CENTER) {
            target = sh - 66 - h;
            slide = Pixel.snap((1f - shown) * (h + 12));
        } else {
            target = (sh - h) / 2f - 30;
            slide = 0;
        }
        e.y = Float.isNaN(e.y) ? target : e.y + (target - e.y) * follow;
        int x = (sw - w) / 2;
        int y = Math.round(e.y) + slide;
        if (p.position == Position.CENTER && a < 1f) {
            // В центре — «проявление» с лёгким сдвигом
            y += Pixel.snap((1f - shown) * 8);
        }

        Pixel.panel(c, x, y, w, h, th, a);
        c.fill(x + 2, y + 2, x + w - 2, y + 4, Pixel.alpha(th.accent(), a)); // акцентная полоска сверху
        if (icon) {
            // Иконка слегка «покачивается», пока ждём ответа
            int bob = e.answer == null && ((now - e.shown) / 400) % 2 == 0 ? -1 : 0;
            Pixel.icon(c, p.icon, x + 8, y + 8 + bob, 2, th.accent(), a);
        }
        int ty = y + 8;
        for (OrderedText line : q) {
            c.drawText(tr, line, x + textX, ty, Pixel.alpha(th.title(), a), th.shadow());
            ty += 10;
        }
        if (!q.isEmpty() && !lines.isEmpty()) ty += 2;
        for (OrderedText line : lines) {
            c.drawText(tr, line, x + textX, ty, Pixel.alpha(th.text(), a), th.shadow());
            ty += 10;
        }

        // --- разделитель и клавиши
        int sepY = y + 7 + textH + 3;
        Pixel.separator(c, x + 6, x + w - 6, sepY, th, a);
        int ky = sepY + 5;
        int kx = x + (w - keysW) / 2;
        boolean flashYes = e.answer == PromptAnswer.YES, flashNo = e.answer == PromptAnswer.NO;
        kx = keycap(c, tr, kx, ky, p.yesKey, yesL, p.yesColor, th, flashYes, e.answer != null && !flashYes, a);
        kx += 14;
        keycap(c, tr, kx, ky, p.noKey, noL, p.noColor, th, flashNo, e.answer != null && !flashNo, a);

        // --- таймер
        if (p.timerBar && p.timeoutMs > 0) {
            long ref = e.answeredAt > 0 ? e.answeredAt : now;
            float rem = Pixel.clamp01(1f - (ref - e.shown) / (float) p.timeoutMs);
            int bx1 = x + 6, bx2 = x + w - 6, by = y + h - 6;
            c.fill(bx1, by, bx2, by + 2, Pixel.alpha(th.dark(), a));
            int fill = Pixel.snap((bx2 - bx1) * rem);
            // Последние 25% времени полоска мигает красным
            int col = rem < 0.25f && (now / 250) % 2 == 0 ? 0xFFFF4B4B : th.accent();
            if (fill > 0) c.fill(bx1, by, bx1 + fill, by + 2, Pixel.alpha(col, a));
        }

        // --- счётчик очереди
        if (queued > 0) {
            String s = "+" + queued;
            int bw = tr.getWidth(s) + 6;
            int bx = x + w - bw - 3, by = y - 5;
            c.fill(bx, by, bx + bw, by + 11, Pixel.alpha(0xFF000000, a));
            c.fill(bx + 1, by + 1, bx + bw - 1, by + 10, Pixel.alpha(th.accent(), a));
            c.drawText(tr, s, bx + 3, by + 2, Pixel.alpha(0xFF000000, a), false);
        }
    }

    /** Пиксельная клавиша с буквой + подпись. Возвращает x после подписи. */
    private static int keycap(DrawContext c, TextRenderer tr, int x, int y, char key, Text label, int color,
                              Theme th, boolean pressed, boolean dim, float a) {
        int base = dim ? Pixel.mix(color, 0xFF303030, 0.7f) : pressed ? Pixel.mix(color, 0xFFFFFFFF, 0.3f) : color;
        int depth = pressed ? 1 : 3;           // нажатая клавиша «проваливается»
        int top = y + (3 - depth);
        int size = 16;
        // контур со срезанными углами
        int o = Pixel.alpha(0xFF000000, a);
        c.fill(x + 1, top, x + size - 1, top + 1, o);
        c.fill(x + 1, y + size + 1, x + size - 1, y + size + 2, o);
        c.fill(x, top + 1, x + 1, y + size + 1, o);
        c.fill(x + size - 1, top + 1, x + size, y + size + 1, o);
        // толщина клавиши
        c.fill(x + 1, top + size - depth, x + size - 1, y + size + 1, Pixel.alpha(Pixel.mix(base, 0xFF000000, 0.5f), a));
        // верхняя грань
        c.fill(x + 1, top + 1, x + size - 1, top + size - depth, Pixel.alpha(base, a));
        c.fill(x + 1, top + 1, x + size - 1, top + 2, Pixel.alpha(Pixel.mix(base, 0xFFFFFFFF, 0.4f), a));
        c.fill(x + 1, top + 2, x + 2, top + size - depth, Pixel.alpha(Pixel.mix(base, 0xFFFFFFFF, 0.2f), a));
        if (pressed) c.fill(x - 1, top - 1, x + size + 1, top, Pixel.alpha(0xFFFFFFFF, a * 0.8f));
        String k = String.valueOf(key);
        c.drawText(tr, k, x + (size - tr.getWidth(k)) / 2 + 1, top + (size - depth - 8) / 2 + 1,
                Pixel.alpha(dim ? 0xFFA0A0A0 : 0xFFFFFFFF, a), true);
        int lx = x + size + 4;
        int lc = dim ? Pixel.mix(th.text(), 0xFF000000, 0.4f) : pressed ? 0xFFFFFFFF : th.title();
        c.drawText(tr, label, lx, y + 5, Pixel.alpha(lc, a), th.shadow());
        return lx + tr.getWidth(label);
    }
}
