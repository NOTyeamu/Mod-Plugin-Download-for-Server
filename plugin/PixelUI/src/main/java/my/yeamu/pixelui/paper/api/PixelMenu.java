package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.internal.Txt;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Кастомное меню: заголовок, строки текста и сетка кнопок (1–3 столбца).
 * <pre>
 * PixelMenu.builder("shop").title("&6Магазин").icon(PixelIcon.COIN).theme(PixelTheme.GOLD)
 *     .line("Баланс: &e120 монет")
 *     .button("buy", "Купить", b -> b.style(PixelButtonStyle.SUCCESS)
 *             .icon(PixelIcon.item(Material.DIAMOND)).closes().onClick(p -> buy(p)))
 *     .button("cancel", "Отмена", b -> b.style(PixelButtonStyle.DANGER).closes())
 *     .onClose(p -> ...)
 *     .open(player);
 * </pre>
 * Обработчики вызываются в главном потоке сервера.
 */
public final class PixelMenu {

    private final String id, title, icon;
    private final PixelTheme theme;
    private final int width, columns;
    private final boolean closeable;
    private final List<String> lines;
    private final List<Button> buttons;
    private final BiConsumer<Player, Button> onClick;
    private final Consumer<Player> onClose;

    private PixelMenu(Builder b) {
        id = b.id;
        title = b.title;
        icon = b.icon;
        theme = b.theme;
        width = b.width;
        columns = b.columns;
        closeable = b.closeable;
        lines = List.copyOf(b.lines);
        buttons = List.copyOf(b.buttons);
        onClick = b.onClick;
        onClose = b.onClose;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() { return id; }
    public String title() { return title; }
    public String icon() { return icon; }
    public PixelTheme theme() { return theme; }
    public int width() { return width; }
    public int columns() { return columns; }
    public boolean closeable() { return closeable; }
    public List<String> lines() { return lines; }
    public List<Button> buttons() { return buttons; }
    public BiConsumer<Player, Button> onClick() { return onClick; }
    public Consumer<Player> onClose() { return onClose; }

    public Button button(String id) {
        for (Button b : buttons) if (b.id.equals(id)) return b;
        return null;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "menu");
        o.addProperty("id", id);
        o.addProperty("title", Txt.section(title));
        if (icon != null) o.addProperty("icon", icon);
        o.add("theme", theme.toJson());
        o.addProperty("width", width);
        o.addProperty("columns", columns);
        o.addProperty("closeable", closeable);
        JsonArray l = new JsonArray();
        for (String s : lines) l.add(Txt.section(s));
        o.add("lines", l);
        JsonArray arr = new JsonArray();
        for (Button b : buttons) {
            JsonObject bo = new JsonObject();
            bo.addProperty("id", b.id);
            bo.addProperty("label", Txt.section(b.label));
            if (b.icon != null) bo.addProperty("icon", b.icon);
            bo.addProperty("style", b.style);
            bo.addProperty("close", b.close);
            if (b.tooltip != null) bo.addProperty("tooltip", Txt.section(b.tooltip));
            bo.addProperty("enabled", b.enabled);
            arr.add(bo);
        }
        o.add("buttons", arr);
        return o;
    }

    /** Кнопка меню. */
    public static final class Button {
        private final String id, label, icon, style, tooltip;
        private final boolean close, enabled;
        private final Consumer<Player> handler;

        private Button(ButtonBuilder b) {
            id = b.id;
            label = b.label;
            icon = b.icon;
            style = b.style;
            tooltip = b.tooltip;
            close = b.close;
            enabled = b.enabled;
            handler = b.handler;
        }

        public String id() { return id; }
        public String label() { return label; }
        public String icon() { return icon; }
        public String style() { return style; }
        public String tooltip() { return tooltip; }
        public boolean close() { return close; }
        public boolean enabled() { return enabled; }
        public Consumer<Player> handler() { return handler; }
    }

    public static final class ButtonBuilder {
        private final String id, label;
        private String icon, style = "default", tooltip;
        private boolean close, enabled = true;
        private Consumer<Player> handler;

        private ButtonBuilder(String id, String label) {
            this.id = Objects.requireNonNull(id);
            this.label = label == null ? "" : label;
        }

        public ButtonBuilder icon(String v) { icon = v; return this; }
        public ButtonBuilder style(PixelButtonStyle v) { style = v.id(); return this; }
        /** Свой цвет кнопки 0xRRGGBB. */
        public ButtonBuilder color(int rgb) { style = String.format("#%06X", rgb & 0xFFFFFF); return this; }
        public ButtonBuilder tooltip(String v) { tooltip = v; return this; }
        /** Закрывать меню после нажатия. */
        public ButtonBuilder close(boolean v) { close = v; return this; }
        public ButtonBuilder closes() { return close(true); }
        /** Неактивная кнопка рисуется серой и не нажимается. */
        public ButtonBuilder enabled(boolean v) { enabled = v; return this; }
        public ButtonBuilder onClick(Consumer<Player> v) { handler = v; return this; }
    }

    public static final class Builder {
        private final String id;
        private String title = "", icon;
        private PixelTheme theme = PixelTheme.DARK;
        private int width = 220, columns = 1;
        private boolean closeable = true;
        private final List<String> lines = new ArrayList<>();
        private final List<Button> buttons = new ArrayList<>();
        private BiConsumer<Player, Button> onClick;
        private Consumer<Player> onClose;

        private Builder(String id) {
            this.id = id == null || id.isBlank() ? "menu" : id;
        }

        public Builder title(String v) { title = v == null ? "" : v; return this; }
        public Builder icon(String v) { icon = v; return this; }
        public Builder theme(PixelTheme v) { theme = Objects.requireNonNull(v); return this; }
        /** Ширина окна 120..400 (в игровых пикселях GUI). */
        public Builder width(int v) { width = Math.max(120, Math.min(v, 400)); return this; }
        /** Количество столбцов кнопок 1..3. */
        public Builder columns(int v) { columns = Math.max(1, Math.min(v, 3)); return this; }
        /** false — меню нельзя закрыть клавишей Esc. */
        public Builder closeable(boolean v) { closeable = v; return this; }

        public Builder line(String v) {
            if (lines.size() < 30) lines.add(v == null ? "" : v);
            return this;
        }

        public Builder lines(String... v) {
            for (String s : v) line(s);
            return this;
        }

        public Builder button(String id, String label) {
            return button(id, label, b -> {});
        }

        public Builder button(String id, String label, Consumer<ButtonBuilder> setup) {
            if (buttons.size() >= 18) throw new IllegalStateException("Максимум 18 кнопок в меню");
            for (Button b : buttons) {
                if (b.id.equals(id)) throw new IllegalArgumentException("Кнопка " + id + " уже есть");
            }
            ButtonBuilder bb = new ButtonBuilder(id, label);
            setup.accept(bb);
            buttons.add(new Button(bb));
            return this;
        }

        /** Общий обработчик нажатий (вызывается после обработчика кнопки). */
        public Builder onClick(BiConsumer<Player, Button> v) { onClick = v; return this; }
        /** Меню закрылось любым способом. */
        public Builder onClose(Consumer<Player> v) { onClose = v; return this; }

        public PixelMenu build() {
            return new PixelMenu(this);
        }

        public void open(Player player) {
            PixelUI.openMenu(player, build());
        }
    }
}
