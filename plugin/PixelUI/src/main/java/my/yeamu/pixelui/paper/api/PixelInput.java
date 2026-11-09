package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Пиксельное окно ввода: до 5 полей (обычные или пароль звёздочками), кнопки «Готово» и «Отмена».
 * Работает ТОЛЬКО у игроков с модом PixelUI (ввод текста в чате за нас не сделаешь):
 * {@link PixelUI#input} вернёт false — сделайте свой запасной вариант (команда, чат).
 * Обработчики вызываются в главном потоке.
 * <pre>
 * PixelInput.builder("rename")
 *     .title("&amp;eПереименовать питомца")
 *     .line("Введите новое имя:")
 *     .field(PixelInput.Field.text("Имя").placeholder("Шарик").maxLength(16))
 *     .icon(PixelIcon.BONE).theme(PixelTheme.GOLD)
 *     .onSubmit((p, values) -&gt; pets.rename(p, values.get(0)))
 *     .send(player);
 * </pre>
 * Если {@code closeOnSubmit(false)} — окно ждёт ответа: вызовите {@link PixelUI#inputError}
 * (показать ошибку и дать ввести снова) или {@link PixelUI#closeInput}.
 */
public final class PixelInput {

    /** Поле ввода. Неизменяемое: методы возвращают копию. */
    public record Field(String label, String placeholder, String value, boolean password, int maxLength) {

        public static Field text(String label) {
            return new Field(label == null ? "" : label, "", "", false, 64);
        }

        public static Field password(String label) {
            return new Field(label == null ? "" : label, "", "", true, 64);
        }

        public Field placeholder(String s) {
            return new Field(label, s == null ? "" : s, value, password, maxLength);
        }

        /** Начальное значение (для паролей не используйте). */
        public Field value(String s) {
            return new Field(label, placeholder, s == null ? "" : s, password, maxLength);
        }

        public Field maxLength(int n) {
            return new Field(label, placeholder, value, password, Math.max(1, Math.min(256, n)));
        }
    }

    private final String id, title, icon, submit, submitIcon, cancel, error, extra, extraIcon, toggleLabel;
    private final PixelTheme theme;
    private final int width, timerSeconds;
    private final boolean closeable, closeOnSubmit, showPasswordToggle;
    private final List<String> lines;
    private final List<Field> fields;
    private final BiConsumer<Player, List<String>> onSubmit, onExtra;
    private final Consumer<Player> onCancel, onClose;

    private PixelInput(Builder b) {
        id = b.id;
        title = b.title;
        icon = b.icon;
        submit = b.submit;
        submitIcon = b.submitIcon;
        cancel = b.cancel;
        error = b.error;
        theme = b.theme;
        width = b.width;
        timerSeconds = b.timerSeconds;
        closeable = b.closeable;
        closeOnSubmit = b.closeOnSubmit;
        lines = List.copyOf(b.lines);
        fields = b.fields.isEmpty() ? List.of(Field.text("")) : List.copyOf(b.fields);
        onSubmit = b.onSubmit;
        onCancel = b.onCancel;
        onClose = b.onClose;
        extra = b.extra;
        extraIcon = b.extraIcon;
        toggleLabel = b.toggleLabel;
        showPasswordToggle = b.showPasswordToggle;
        onExtra = b.onExtra;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() { return id; }
    public String title() { return title; }
    public List<String> lines() { return lines; }
    public List<Field> fields() { return fields; }
    public boolean closeable() { return closeable; }
    public boolean closeOnSubmit() { return closeOnSubmit; }
    public int timerSeconds() { return timerSeconds; }
    public BiConsumer<Player, List<String>> onSubmit() { return onSubmit; }
    public Consumer<Player> onCancel() { return onCancel; }
    public Consumer<Player> onClose() { return onClose; }
    public BiConsumer<Player, List<String>> onExtra() { return onExtra; }
    public String extra() { return extra; }

    /** Копия с другим текстом ошибки и таймером (удобно для повторного открытия). */
    public PixelInput with(String newError, int newTimerSeconds) {
        Builder b = toBuilder();
        b.error = newError == null ? "" : newError;
        b.timerSeconds = Math.max(0, newTimerSeconds);
        return b.build();
    }

    public Builder toBuilder() {
        Builder b = new Builder(id);
        b.title = title; b.icon = icon; b.submit = submit; b.submitIcon = submitIcon; b.cancel = cancel;
        b.error = error; b.theme = theme; b.width = width; b.timerSeconds = timerSeconds;
        b.closeable = closeable; b.closeOnSubmit = closeOnSubmit;
        b.lines.addAll(lines); b.fields.addAll(fields);
        b.onSubmit = onSubmit; b.onCancel = onCancel; b.onClose = onClose;
        b.extra = extra; b.extraIcon = extraIcon; b.toggleLabel = toggleLabel;
        b.showPasswordToggle = showPasswordToggle; b.onExtra = onExtra;
        return b;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "input");
        o.addProperty("id", id);
        o.addProperty("title", title);
        o.addProperty("icon", icon);
        o.add("theme", theme.toJson());
        o.addProperty("width", width);
        JsonArray ls = new JsonArray();
        for (String s : lines) ls.add(s);
        o.add("lines", ls);
        JsonArray fs = new JsonArray();
        for (Field f : fields) {
            JsonObject fo = new JsonObject();
            fo.addProperty("label", f.label());
            fo.addProperty("placeholder", f.placeholder());
            fo.addProperty("value", f.value());
            fo.addProperty("password", f.password());
            fo.addProperty("maxLength", f.maxLength());
            fs.add(fo);
        }
        o.add("fields", fs);
        o.addProperty("submit", submit);
        o.addProperty("submitIcon", submitIcon);
        if (cancel != null) o.addProperty("cancel", cancel);
        o.addProperty("closeable", closeable);
        o.addProperty("closeOnSubmit", closeOnSubmit);
        o.addProperty("timer", timerSeconds);
        o.addProperty("error", error);
        if (extra != null) o.addProperty("extra", extra);
        o.addProperty("extraIcon", extraIcon);
        o.addProperty("showToggle", showPasswordToggle);
        o.addProperty("toggleLabel", toggleLabel);
        return o;
    }

    /** Показать игроку. false — у игрока нет мода PixelUI. */
    public boolean send(Player player) {
        return PixelUI.input(player, this);
    }

    public static final class Builder {
        private final String id;
        private String title = "", icon = "", submit = "Готово", submitIcon = "", cancel = "Отмена", error = "";
        private PixelTheme theme = PixelTheme.DARK;
        private int width = 240, timerSeconds;
        private boolean closeable = true, closeOnSubmit = true;
        private final List<String> lines = new ArrayList<>();
        private final List<Field> fields = new ArrayList<>();
        private BiConsumer<Player, List<String>> onSubmit;
        private Consumer<Player> onCancel, onClose;
        private String extra, extraIcon = "", toggleLabel = "Показать пароль";
        private boolean showPasswordToggle = true;
        private BiConsumer<Player, List<String>> onExtra;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder title(String s) { title = s == null ? "" : s; return this; }
        /** Строка текста под заголовком (цвета &amp;a, &amp;l … поддерживаются). */
        public Builder line(String s) { lines.add(s == null ? "" : s); return this; }
        public Builder lines(List<String> l) { lines.clear(); if (l != null) lines.addAll(l); return this; }
        public Builder field(Field f) { if (f != null && fields.size() < 5) fields.add(f); return this; }
        public Builder icon(String s) { icon = s == null ? "" : s; return this; }
        public Builder theme(PixelTheme t) { theme = t == null ? PixelTheme.DARK : t; return this; }
        public Builder width(int w) { width = Math.max(160, Math.min(400, w)); return this; }
        public Builder submit(String label) { submit = label == null ? "Готово" : label; return this; }
        public Builder submitIcon(String i) { submitIcon = i == null ? "" : i; return this; }
        /** null — без кнопки отмены. */
        public Builder cancel(String label) { cancel = label; return this; }
        /** Можно ли закрыть клавишей Esc. */
        public Builder closeable(boolean b) { closeable = b; return this; }
        /** false — после отправки окно ждёт решения сервера (inputError / closeInput). */
        public Builder closeOnSubmit(boolean b) { closeOnSubmit = b; return this; }
        /** Показать таймер обратного отсчёта (только визуально). */
        public Builder timer(int seconds) { timerSeconds = Math.max(0, seconds); return this; }
        /** Текст ошибки, виден сразу при открытии. */
        public Builder error(String s) { error = s == null ? "" : s; return this; }
        public Builder onSubmit(BiConsumer<Player, List<String>> h) { onSubmit = h; return this; }
        public Builder onCancel(Consumer<Player> h) { onCancel = h; return this; }
        /** Окно закрылось не кнопкой (Esc, другой экран и т.п.). */
        public Builder onClose(Consumer<Player> h) { onClose = h; return this; }

        /**
         * Доп. кнопка под основными (например «Забыли пароль?»). null — без неё.
         * Окно не закрывает; обработчик — {@link #onExtra}. Нужен мод PixelUI 1.1.5+ (у старых кнопки просто нет).
         */
        public Builder extra(String label) { extra = label == null || label.isEmpty() ? null : label; return this; }
        public Builder extraIcon(String i) { extraIcon = i == null ? "" : i; return this; }
        /** Нажата доп. кнопка: игрок и текущие значения полей. */
        public Builder onExtra(BiConsumer<Player, List<String>> h) { onExtra = h; return this; }
        /** Галочка «Показать пароль» под полями с паролем (по умолчанию есть). */
        public Builder showPasswordToggle(boolean b) { showPasswordToggle = b; return this; }
        public Builder toggleLabel(String s) { toggleLabel = s == null || s.isEmpty() ? "Показать пароль" : s; return this; }

        public PixelInput build() { return new PixelInput(this); }

        public boolean send(Player player) { return build().send(player); }
    }
}
