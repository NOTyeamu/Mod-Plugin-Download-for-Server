package my.yeamu.pixelui.api;

import com.google.gson.JsonObject;

/**
 * Плашка — одна строка с иконкой и (необязательно) полосой прогресса.
 * Висит сверху, в центре или над хотбаром. Есть id: повторный вызов с тем же id обновляет плашку
 * (текст/прогресс плавно меняются). duration = 0 — висит, пока её не уберут.
 */
public final class Banner {

    public final String id;
    public final String text;
    public final String icon;
    public final Theme theme;
    public final Position position;
    public final long durationMs;
    /** 0..1, отрицательное — без полосы. */
    public final float progress;
    public final String sound;

    private Banner(Builder b) {
        this.id = b.id == null || b.id.isBlank() ? "default" : b.id;
        this.text = b.text == null ? "" : b.text;
        this.icon = b.icon;
        this.theme = b.theme;
        Position p = b.position;
        // Плашки всегда по центру по горизонтали
        this.position = p.isTop() ? Position.TOP_CENTER : p.isBottom() ? Position.BOTTOM_CENTER : Position.CENTER;
        this.durationMs = Math.max(0, b.durationMs);
        this.progress = b.progress;
        this.sound = b.sound;
    }

    public static Builder builder(String id) {
        return new Builder().id(id);
    }

    public static Banner fromJson(JsonObject o) {
        Builder b = builder(o.has("id") ? o.get("id").getAsString() : "default");
        if (o.has("text")) b.text(o.get("text").getAsString());
        if (o.has("icon")) b.icon(o.get("icon").getAsString());
        if (o.has("theme")) b.theme(Theme.fromJson(o.get("theme")));
        if (o.has("position")) b.position(Position.parse(o.get("position").getAsString(), Position.TOP_CENTER));
        if (o.has("duration")) b.duration(o.get("duration").getAsLong());
        if (o.has("progress")) b.progress(o.get("progress").getAsFloat());
        if (o.has("sound")) b.sound(o.get("sound").getAsString());
        return b.build();
    }

    public static final class Builder {
        private String id, text, icon = "none", sound;
        private Theme theme = Theme.DARK;
        private Position position = Position.TOP_CENTER;
        private long durationMs;
        private float progress = -1f;

        public Builder id(String v) { id = v; return this; }
        public Builder text(String v) { text = v; return this; }
        public Builder icon(String v) { icon = v; return this; }
        public Builder theme(Theme v) { theme = v == null ? Theme.DARK : v; return this; }
        public Builder position(Position v) { position = v == null ? Position.TOP_CENTER : v; return this; }
        public Builder duration(long ms) { durationMs = ms; return this; }
        public Builder progress(float v) { progress = v; return this; }
        public Builder sound(String v) { sound = v; return this; }

        public Banner build() {
            return new Banner(this);
        }
    }
}
