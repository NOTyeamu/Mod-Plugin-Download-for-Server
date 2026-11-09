package my.yeamu.pixelui.screen;

import my.yeamu.pixelui.api.InputForm;
import my.yeamu.pixelui.api.Theme;
import my.yeamu.pixelui.render.Pixel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Пиксельное окно ввода (пароль, ник, код и т.д.).
 *
 * Поля — обычные TextFieldWidget (ввод, вставка, Tab между полями работают как в ваниле),
 * но сверху рисуем свои пиксельные коробки: пароль показывается звёздочками.
 * Enter — отправить, Esc — закрыть (если окно разрешает).
 */
public class InputScreen extends Screen {

    private static final long ANIM = 220;
    private static final int BOX_H = 18;

    public final boolean fromServer;
    private final InputForm form;
    private final Consumer<List<String>> onSubmit;
    private final Runnable onCancel;
    private final Runnable onClosed;
    private final long opened = System.currentTimeMillis();
    private final long deadline;

    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final List<Integer> labelY = new ArrayList<>();
    private ButtonWidget submitBtn;
    private ButtonWidget cancelBtn;
    private List<OrderedText> title = List.of();
    private List<OrderedText> lines = List.of();
    private String[] values;
    private String error;
    private long errorAt;
    /** Ждём ответа сервера после отправки — повторно не шлём. */
    private boolean waiting;
    private long lastSubmit;
    private boolean enterDown = true; // чтобы Enter, открывший чат/команду, не отправил форму
    private boolean silent;
    private int px, py, pw, ph, header, errorY, timerY;

    public InputScreen(InputForm form, boolean fromServer, Consumer<List<String>> onSubmit,
                       Runnable onCancel, Runnable onClosed) {
        super(Pixel.text(form.title));
        this.form = form;
        this.fromServer = fromServer;
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
        this.onClosed = onClosed;
        this.error = form.error == null ? "" : form.error;
        this.errorAt = error.isEmpty() ? 0 : opened;
        this.deadline = form.timer > 0 ? opened + form.timer * 1000L : 0;
        this.values = new String[form.fields.size()];
        for (int i = 0; i < values.length; i++) values[i] = form.fields.get(i).value();
    }

    public InputForm form() {
        return form;
    }

    public void markReplaced() {
        silent = true;
    }

    public void closeSilently() {
        silent = true;
        if (client != null && client.currentScreen == this) client.setScreen(null);
    }

    /** Сервер сообщил об ошибке (например, неверный пароль). */
    public void setError(String msg, boolean clearPasswords) {
        error = msg == null ? "" : msg;
        errorAt = System.currentTimeMillis();
        waiting = false;
        if (clearPasswords) {
            TextFieldWidget first = null;
            for (int i = 0; i < fields.size(); i++) {
                if (form.fields.get(i).password()) {
                    fields.get(i).setText("");
                    values[i] = "";
                    if (first == null) first = fields.get(i);
                }
            }
            if (first != null) focus(first);
        }
        Pixel.sound("minecraft:block.note_block.bass", 0.7f);
    }

    private void focus(TextFieldWidget w) {
        for (TextFieldWidget f : fields) f.setFocused(f == w);
        setFocused(w);
    }

    @Override
    protected void init() {
        // При изменении размера окна init() вызывается заново — сохраняем введённое
        int focusedIdx = 0;
        for (int i = 0; i < fields.size(); i++) {
            values[i] = fields.get(i).getText();
            if (fields.get(i).isFocused()) focusedIdx = i;
        }
        fields.clear();
        labelY.clear();

        pw = Math.min(form.width, width - 16);
        boolean icon = Pixel.hasIcon(form.icon);
        int titleX = icon ? 30 : 10;
        title = form.title.isEmpty() ? List.of() : textRenderer.wrapLines(Pixel.text(form.title), pw - titleX - 34);
        if (title.size() > 2) title = title.subList(0, 2);
        List<OrderedText> l = new ArrayList<>();
        for (String s : form.lines) {
            if (s.isEmpty()) l.add(OrderedText.EMPTY);
            else l.addAll(textRenderer.wrapLines(Pixel.text(s), pw - 20));
        }
        if (l.size() > 8) l = l.subList(0, 8);
        lines = l;

        header = Math.max(24, 8 + title.size() * 10 + 6);
        int body = lines.isEmpty() ? 0 : 6 + lines.size() * 10;
        int fieldsH = 0;
        for (InputForm.Field f : form.fields) fieldsH += (f.label().isEmpty() ? 0 : 11) + BOX_H + 5;
        ph = header + body + 8 + fieldsH + 12 + 24 + (deadline > 0 ? 6 : 2);
        px = (width - pw) / 2;
        py = Math.max(6, (height - ph) / 2);

        int y = py + header + body + 8;
        for (int i = 0; i < form.fields.size(); i++) {
            InputForm.Field f = form.fields.get(i);
            if (!f.label().isEmpty()) {
                labelY.add(y);
                y += 11;
            } else {
                labelY.add(-1);
            }
            TextFieldWidget w = new TextFieldWidget(textRenderer, px + 10, y, pw - 20, BOX_H, Pixel.text(f.label()));
            w.setMaxLength(f.maxLength());
            w.setText(values[i] == null ? "" : values[i]);
            addDrawableChild(w);
            fields.add(w);
            y += BOX_H + 5;
        }
        errorY = y;
        y += 12;

        int gap = 4;
        boolean hasCancel = form.cancel != null;
        int bw = hasCancel ? (pw - 20 - gap) / 2 : pw - 20;
        submitBtn = ButtonWidget.builder(Pixel.text(form.submit), b -> submit())
                .dimensions(px + 10, y, bw, 20).build();
        addDrawableChild(submitBtn);
        if (hasCancel) {
            cancelBtn = ButtonWidget.builder(Pixel.text(form.cancel), b -> cancel())
                    .dimensions(px + 10 + bw + gap, y, bw, 20).build();
            addDrawableChild(cancelBtn);
        } else {
            cancelBtn = null;
        }
        timerY = y + 24;
        if (!fields.isEmpty()) focus(fields.get(Math.min(focusedIdx, fields.size() - 1)));
    }

    private void submit() {
        long now = System.currentTimeMillis();
        if (waiting || now - lastSubmit < 300) return; // защита от двойной отправки
        lastSubmit = now;
        List<String> out = new ArrayList<>();
        for (TextFieldWidget f : fields) out.add(f.getText());
        if (!form.closeOnSubmit) waiting = true;
        onSubmit.accept(out);
        if (form.closeOnSubmit) {
            silent = true;
            close();
        }
    }

    private void cancel() {
        silent = true;
        onCancel.run();
        close();
    }

    @Override
    public void tick() {
        super.tick();
        if (client == null) return;
        // Enter читаем напрямую с клавиатуры — нажатие срабатывает один раз по «фронту»
        long win = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_KP_ENTER) == GLFW.GLFW_PRESS;
        if (down && !enterDown) submit();
        enterDown = down;
    }

    private float anim() {
        return Pixel.easeOut((System.currentTimeMillis() - opened) / (float) ANIM);
    }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, Pixel.alpha(0xA0000000, anim()));
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta); // фон + стандартные виджеты (перекроем своими)
        float t = anim();
        Theme th = form.theme;
        int dy = Pixel.snap((1f - t) * 16);
        // Тряска окна при ошибке (ступеньками по 2 пикселя)
        long since = System.currentTimeMillis() - errorAt;
        int dx = errorAt > 0 && since < 300 ? Pixel.snap((float) Math.sin(since / 25.0) * 6f * (1f - since / 300f)) : 0;
        int x0 = px + dx;
        Pixel.panel(c, x0, py + dy, pw, ph, th, Math.max(t, 0.01f));
        if (t < 0.6f) return;
        float a = (t - 0.6f) / 0.4f;
        int top = py + dy;

        // Заголовок
        boolean icon = Pixel.hasIcon(form.icon);
        int titleX = x0 + (icon ? 30 : 10);
        if (icon) Pixel.icon(c, form.icon, x0 + 8, top + (header - 16) / 2, 2, th.accent(), a);
        int ty = top + (header - title.size() * 10) / 2 + 1;
        for (OrderedText line : title) {
            c.drawText(textRenderer, line, titleX, ty, Pixel.alpha(th.title(), a), th.shadow());
            ty += 10;
        }
        // Таймер справа в заголовке
        long left = deadline > 0 ? Math.max(0, deadline - System.currentTimeMillis()) : 0;
        if (deadline > 0) {
            String s = (left + 999) / 1000 + "с";
            int col = left < 10_000 ? 0xFFFF5555 : th.text();
            c.drawText(textRenderer, s, x0 + pw - 10 - textRenderer.getWidth(s), top + (header - 8) / 2, Pixel.alpha(col, a), th.shadow());
        }
        Pixel.separator(c, x0 + 4, x0 + pw - 4, top + header - 2, th, a);

        int ly = top + header + 6;
        for (OrderedText line : lines) {
            c.drawText(textRenderer, line, x0 + 10, ly, Pixel.alpha(th.text(), a), th.shadow());
            ly += 10;
        }

        // Поля ввода
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        for (int i = 0; i < fields.size(); i++) {
            TextFieldWidget w = fields.get(i);
            InputForm.Field f = form.fields.get(i);
            if (labelY.get(i) >= 0) {
                c.drawText(textRenderer, Pixel.text(f.label()), x0 + 10, labelY.get(i) + dy + 1, Pixel.alpha(th.title(), a), th.shadow());
            }
            drawBox(c, w, f, w.getX() + dx, w.getY() + dy, th, a, blink);
        }

        // Ошибка под полями
        if (!error.isEmpty()) {
            Text et = Pixel.text(error);
            String plain = et.getString();
            int ex = x0 + 10;
            if (Pixel.hasIcon("cross")) {
                Pixel.icon(c, "cross", ex, errorY + dy, 1, 0xFFFF5555, a);
                ex += 11;
            }
            String cut = textRenderer.trimToWidth(plain, pw - 20 - (ex - x0 - 10));
            c.drawText(textRenderer, cut.equals(plain) ? et : Text.literal(cut), ex, errorY + dy, Pixel.alpha(0xFFFF5555, a), th.shadow());
        }

        // Кнопки
        String sIcon = waiting ? "clock" : form.submitIcon;
        Pixel.button(c, textRenderer, submitBtn.getX() + dx, submitBtn.getY() + dy, submitBtn.getWidth(), submitBtn.getHeight(),
                Pixel.text(waiting ? "..." : form.submit), sIcon, Pixel.mix(th.accent(), 0xFF000000, 0.35f), th.accent(),
                submitBtn.isSelected(), !waiting, a);
        if (cancelBtn != null) {
            Pixel.button(c, textRenderer, cancelBtn.getX() + dx, cancelBtn.getY() + dy, cancelBtn.getWidth(), cancelBtn.getHeight(),
                    Pixel.text(form.cancel), "", th == Theme.STONE ? 0xFF8B8B8B : 0xFF5A5A5A, th.accent(),
                    cancelBtn.isSelected(), true, a);
        }

        // Полоска таймера внизу панели
        if (deadline > 0) {
            float frac = left / (form.timer * 1000f);
            int bw = Pixel.snap((pw - 20) * frac);
            int by = timerY + dy;
            c.fill(x0 + 10, by, x0 + pw - 10, by + 2, Pixel.alpha(th.dark(), a));
            c.fill(x0 + 10, by, x0 + 10 + bw, by + 2, Pixel.alpha(left < 10_000 ? 0xFFFF5555 : th.accent(), a));
        }
    }

    /** Пиксельная коробка поля: рамка, тёмный фон, текст/звёздочки, мигающий курсор. */
    private void drawBox(DrawContext c, TextFieldWidget w, InputForm.Field f, int x, int y, Theme th, float a, boolean blink) {
        int bw = w.getWidth(), bh = w.getHeight();
        boolean focused = w.isFocused();
        int outline = focused ? th.accent() : 0xFF000000;
        c.fill(x, y, x + bw, y + bh, Pixel.alpha(outline, a));
        c.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, Pixel.alpha(0xFF0E0E0E, a));
        // внутренняя «вдавленная» тень в стиле слота инвентаря
        c.fill(x + 1, y + 1, x + bw - 1, y + 2, Pixel.alpha(0xFF2A2A2A, a));
        c.fill(x + 1, y + 1, x + 2, y + bh - 1, Pixel.alpha(0xFF2A2A2A, a));

        String raw = w.getText();
        int room = bw - 12;
        int tx = x + 5, ty = y + (bh - 8) / 2 + 1;
        if (raw.isEmpty()) {
            if (!f.placeholder().isEmpty()) {
                c.drawText(textRenderer, textRenderer.trimToWidth(f.placeholder(), room), tx, ty, Pixel.alpha(0xFF6A6A6A, a), false);
            }
        } else {
            String shown = f.password() ? "*".repeat(raw.length()) : raw;
            // Длинный текст — показываем хвост (там, где печатаем)
            while (shown.length() > 1 && textRenderer.getWidth(shown) > room) shown = shown.substring(1);
            c.drawText(textRenderer, shown, tx, ty, Pixel.alpha(0xFFFFFFFF, a), true);
            tx += textRenderer.getWidth(shown);
        }
        if (focused && blink && !waiting) {
            c.fill(tx + 1, y + 4, tx + 2, y + bh - 4, Pixel.alpha(0xFFFFFFFF, a));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return form.closeable;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (!silent) {
            silent = true;
            onClosed.run();
        }
    }
}
