package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.internal.Txt;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

/**
 * Плашка — полоса сверху/в центре/снизу с текстом, иконкой и прогрессом.
 * Отправка с тем же id плавно обновляет плашку (текст, прогресс).
 * <pre>
 * PixelBanner.builder("event").text("&eИвент закончится через 5:00")
 *     .icon(PixelIcon.CLOCK).theme(PixelTheme.WARNING).progress(0.4f).send(player);
 * </pre>
 */
public final class PixelBanner {

    private final String id, text, icon, sound;
    private final PixelTheme theme;
    private final PixelPosition position;
    private final long durationMs;
    private final float progress;

    private PixelBanner(Builder b) {
        id = b.id;
        text = b.text;
        icon = b.icon;
        sound = b.sound;
        theme = b.theme;
        position = b.position;
        durationMs = b.durationMs;
        progress = b.progress;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() { return id; }
    public String text() { return text; }
    public String icon() { return icon; }
    public String sound() { return sound; }
    public PixelTheme theme() { return theme; }
    public PixelPosition position() { return position; }
    public long durationMs() { return durationMs; }
    public float progress() { return progress; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "banner");
        o.addProperty("id", id);
        o.addProperty("text", Txt.section(text));
        o.addProperty("icon", icon);
        o.add("theme", theme.toJson());
        o.addProperty("position", position.name());
        o.addProperty("duration", durationMs);
        o.addProperty("progress", progress);
        if (sound != null) o.addProperty("sound", sound);
        return o;
    }

    public static final class Builder {
        private final String id;
        private String text = "", icon = PixelIcon.NONE, sound;
        private PixelTheme theme = PixelTheme.DARK;
        private PixelPosition position = PixelPosition.TOP_CENTER;
        private long durationMs;
        private float progress = -1f;

        private Builder(String id) {
            this.id = id == null || id.isBlank() ? "default" : id;
        }

        public Builder text(String v) { text = v == null ? "" : v; return this; }
        public Builder icon(String v) { icon = v == null ? PixelIcon.NONE : v; return this; }
        public Builder theme(PixelTheme v) { theme = Objects.requireNonNull(v); return this; }
        /** TOP_CENTER, CENTER или BOTTOM_CENTER (остальные клиент приведёт к ближайшей). */
        public Builder position(PixelPosition v) { position = Objects.requireNonNull(v); return this; }
        /** 0 — пока не уберут вручную (PixelUI.removeBanner). */
        public Builder duration(long ms) { durationMs = Math.max(0, ms); return this; }
        public Builder duration(Duration d) { return duration(d.toMillis()); }
        /** 0..1 — полоска прогресса, -1 — без неё. */
        public Builder progress(float v) { progress = v < 0 ? -1f : Math.min(1f, v); return this; }
        public Builder sound(String v) { sound = v; return this; }

        public PixelBanner build() {
            return new PixelBanner(this);
        }

        public void send(Player player) {
            PixelUI.banner(player, build());
        }
    }
}
