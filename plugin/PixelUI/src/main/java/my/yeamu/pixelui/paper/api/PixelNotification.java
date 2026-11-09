package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.internal.Txt;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

/**
 * Кастомное уведомление (тост). Пример:
 * <pre>
 * PixelNotification.builder()
 *     .title("&aКвест выполнен").text("Награда: 50 монет")
 *     .icon(PixelIcon.COIN).theme(PixelTheme.GOLD).position(PixelPosition.TOP_RIGHT)
 *     .duration(Duration.ofSeconds(6)).sound("minecraft:entity.player.levelup")
 *     .send(player);
 * </pre>
 * Повторная отправка с тем же id обновляет уведомление на месте (удобно для таймеров).
 */
public final class PixelNotification {

    private final String id, title, text, icon, sound;
    private final PixelTheme theme;
    private final PixelPosition position;
    private final long durationMs;
    private final float pitch;
    private final boolean progress;
    private final int width;

    private PixelNotification(Builder b) {
        id = b.id;
        title = b.title;
        text = b.text;
        icon = b.icon;
        sound = b.sound;
        theme = b.theme;
        position = b.position;
        durationMs = b.durationMs;
        pitch = b.pitch;
        progress = b.progress;
        width = b.width;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PixelNotification of(String title, String text) {
        return builder().title(title).text(text).build();
    }

    public String id() { return id; }
    public String title() { return title; }
    public String text() { return text; }
    public String icon() { return icon; }
    public String sound() { return sound; }
    public PixelTheme theme() { return theme; }
    public PixelPosition position() { return position; }
    public long durationMs() { return durationMs; }
    public float pitch() { return pitch; }
    public boolean progress() { return progress; }
    public int width() { return width; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "notify");
        if (id != null) o.addProperty("id", id);
        o.addProperty("title", Txt.section(title));
        o.addProperty("text", Txt.section(text));
        o.addProperty("icon", icon);
        o.add("theme", theme.toJson());
        o.addProperty("position", position.name());
        o.addProperty("duration", durationMs);
        if (sound != null) o.addProperty("sound", sound);
        o.addProperty("pitch", pitch);
        o.addProperty("progress", progress);
        if (width > 0) o.addProperty("width", width);
        return o;
    }

    public static final class Builder {
        private String id, title = "", text = "", icon = PixelIcon.INFO, sound;
        private PixelTheme theme = PixelTheme.DARK;
        private PixelPosition position = PixelPosition.TOP_RIGHT;
        private long durationMs = 5000;
        private float pitch = 1f;
        private boolean progress = true;
        private int width;

        private Builder() {}

        /** Уведомление с тем же id заменяет старое. */
        public Builder id(String v) { id = v; return this; }
        /** Заголовок: &-коды или MiniMessage. */
        public Builder title(String v) { title = v == null ? "" : v; return this; }
        /** Текст, перенос строки — \n. */
        public Builder text(String v) { text = v == null ? "" : v; return this; }
        /** PixelIcon.* или PixelIcon.item(...), PixelIcon.NONE — без иконки. */
        public Builder icon(String v) { icon = v == null ? PixelIcon.NONE : v; return this; }
        public Builder theme(PixelTheme v) { theme = Objects.requireNonNull(v); return this; }
        public Builder position(PixelPosition v) { position = Objects.requireNonNull(v); return this; }
        public Builder duration(long ms) { durationMs = Math.max(500, Math.min(ms, 120_000)); return this; }
        public Builder duration(Duration d) { return duration(d.toMillis()); }
        /** Звук при появлении (id звука), "" — без звука. */
        public Builder sound(String v) { sound = v; return this; }
        public Builder pitch(float v) { pitch = v; return this; }
        /** Полоска оставшегося времени внизу. */
        public Builder progress(boolean v) { progress = v; return this; }
        /** Фиксированная ширина 100..300, 0 — по тексту. */
        public Builder width(int v) { width = v; return this; }

        public PixelNotification build() {
            return new PixelNotification(this);
        }

        public void send(Player player) {
            PixelUI.notify(player, build());
        }
    }
}
