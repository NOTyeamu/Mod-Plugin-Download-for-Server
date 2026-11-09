package my.yeamu.pixelui.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Меню: окно с заголовком, строками текста и сеткой пиксельных кнопок (1–3 колонки).
 * Нажатие уходит в Listener (локальное меню) или на сервер (меню от плагина).
 */
public final class Menu {

    /** Стиль кнопки: default, primary (цвет акцента темы), success, danger, info, gold или "#RRGGBB". */
    public record Button(String id, String label, String icon, String style, boolean close, String tooltip, boolean enabled) {
        public static Button of(String id, String label) {
            return new Button(id, label, null, "default", false, null, true);
        }
        public Button style(String s) { return new Button(id, label, icon, s, close, tooltip, enabled); }
        public Button icon(String i) { return new Button(id, label, i, style, close, tooltip, enabled); }
        public Button closes() { return new Button(id, label, icon, style, true, tooltip, enabled); }
        public Button tooltip(String t) { return new Button(id, label, icon, style, close, t, enabled); }
        public Button enabled(boolean e) { return new Button(id, label, icon, style, close, tooltip, e); }
    }

    /** Обработчик локального меню (если меню открывает другой клиентский мод). */
    public interface Listener {
        void onClick(Menu menu, Button button);
        default void onClose(Menu menu) {}
    }

    public final String id;
    public final String title;
    public final String icon;
    public final Theme theme;
    public final int width;
    public final int columns;
    public final boolean closeable;
    public final List<String> lines;
    public final List<Button> buttons;

    private Menu(Builder b) {
        this.id = b.id;
        this.title = b.title == null ? "" : b.title;
        this.icon = b.icon;
        this.theme = b.theme;
        this.width = b.width <= 0 ? 220 : Math.max(120, Math.min(b.width, 400));
        this.columns = Math.max(1, Math.min(3, b.columns));
        this.closeable = b.closeable;
        this.lines = Collections.unmodifiableList(new ArrayList<>(b.lines));
        this.buttons = Collections.unmodifiableList(new ArrayList<>(b.buttons));
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public Button button(String buttonId) {
        for (Button b : buttons) if (b.id().equals(buttonId)) return b;
        return null;
    }

    public static Menu fromJson(JsonObject o) {
        Builder b = builder(o.has("id") ? o.get("id").getAsString() : "menu");
        if (o.has("title")) b.title(o.get("title").getAsString());
        if (o.has("icon")) b.icon(o.get("icon").getAsString());
        if (o.has("theme")) b.theme(Theme.fromJson(o.get("theme")));
        if (o.has("width")) b.width(o.get("width").getAsInt());
        if (o.has("columns")) b.columns(o.get("columns").getAsInt());
        if (o.has("closeable")) b.closeable(o.get("closeable").getAsBoolean());
        if (o.has("lines") && o.get("lines").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("lines")) b.line(e.getAsString());
        }
        if (o.has("buttons") && o.get("buttons").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("buttons");
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) continue;
                JsonObject bo = e.getAsJsonObject();
                b.button(new Button(
                        bo.has("id") ? bo.get("id").getAsString() : "btn" + b.buttons.size(),
                        bo.has("label") ? bo.get("label").getAsString() : "?",
                        bo.has("icon") ? bo.get("icon").getAsString() : null,
                        bo.has("style") ? bo.get("style").getAsString() : "default",
                        bo.has("close") && bo.get("close").getAsBoolean(),
                        bo.has("tooltip") ? bo.get("tooltip").getAsString() : null,
                        !bo.has("enabled") || bo.get("enabled").getAsBoolean()));
            }
        }
        return b.build();
    }

    public static final class Builder {
        private final String id;
        private String title, icon;
        private Theme theme = Theme.DARK;
        private int width = 220, columns = 1;
        private boolean closeable = true;
        private final List<String> lines = new ArrayList<>();
        private final List<Button> buttons = new ArrayList<>();

        private Builder(String id) { this.id = id == null ? "menu" : id; }

        public Builder title(String v) { title = v; return this; }
        public Builder icon(String v) { icon = v; return this; }
        public Builder theme(Theme v) { theme = v == null ? Theme.DARK : v; return this; }
        public Builder width(int v) { width = v; return this; }
        public Builder columns(int v) { columns = v; return this; }
        /** false — меню нельзя закрыть на Esc, только кнопкой. */
        public Builder closeable(boolean v) { closeable = v; return this; }
        public Builder line(String v) { if (lines.size() < 30) lines.add(v == null ? "" : v); return this; }
        public Builder button(Button v) { if (buttons.size() < 18) buttons.add(v); return this; }
        public Builder button(String id, String label) { return button(Button.of(id, label)); }

        public Menu build() {
            return new Menu(this);
        }
    }
}
