package my.yeamu.pixelui.api;

import com.google.gson.JsonObject;

/**
 * Уведомление (тост): иконка, заголовок, текст, полоска времени.
 * Текст понимает цветовые коды § и &, перенос строки — \n.
 * Уведомление с тем же id заменяет предыдущее (удобно для счётчиков).
 */
public final class Notification {

    public final String id;
    public final String title;
    public final String text;
    public final String icon;
    public final Theme theme;
    public final Position position;
    public final long durationMs;
    public final String sound;
    public final float pitch;
    public final boolean progressBar;
    public final int width;

    private Notification(Builder b) {
        this.id = b.id;
        this.title = b.title == null ? "" : b.title;
        this.text = b.text == null ? "" : b.text;
        this.icon = b.icon;
        this.theme = b.theme;
        this.position = b.position;
        this.durationMs = b.durationMs <= 0 ? 5000 : Math.min(b.durationMs, 120_000);
        this.sound = b.sound;
        this.pitch = b.pitch;
        this.progressBar = b.progressBar;
        this.width = b.width;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Notification fromJson(JsonObject o) {
        Builder b = builder();
        if (o.has("id")) b.id(o.get("id").getAsString());
        if (o.has("title")) b.title(o.get("title").getAsString());
        if (o.has("text")) b.text(o.get("text").getAsString());
        if (o.has("icon")) b.icon(o.get("icon").getAsString());
        if (o.has("theme")) b.theme(Theme.fromJson(o.get("theme")));
        if (o.has("position")) b.position(Position.parse(o.get("position").getAsString(), Position.TOP_RIGHT));
        if (o.has("duration")) b.duration(o.get("duration").getAsLong());
        if (o.has("sound")) b.sound(o.get("sound").getAsString());
        if (o.has("pitch")) b.pitch(o.get("pitch").getAsFloat());
        if (o.has("progress")) b.progressBar(o.get("progress").getAsBoolean());
        if (o.has("width")) b.width(o.get("width").getAsInt());
        return b.build();
    }

    public static final class Builder {
        private String id, title, text, icon = "info", sound;
        private Theme theme = Theme.DARK;
        private Position position = Position.TOP_RIGHT;
        private long durationMs = 5000;
        private float pitch = 1f;
        private boolean progressBar = true;
        private int width;

        public Builder id(String v) { id = v; return this; }
        public Builder title(String v) { title = v; return this; }
        public Builder text(String v) { text = v; return this; }
        /** Имя пиксельной иконки (info, check, cross, warn, star, heart, coin, bell, gift, sword, shield, clock),
         *  предмет "item:minecraft:diamond" или "none". */
        public Builder icon(String v) { icon = v; return this; }
        public Builder theme(Theme v) { theme = v == null ? Theme.DARK : v; return this; }
        public Builder position(Position v) { position = v == null ? Position.TOP_RIGHT : v; return this; }
        public Builder duration(long ms) { durationMs = ms; return this; }
        /** Звук при показе, например "minecraft:entity.experience_orb.pickup". */
        public Builder sound(String v) { sound = v; return this; }
        public Builder pitch(float v) { pitch = v; return this; }
        public Builder progressBar(boolean v) { progressBar = v; return this; }
        /** Ширина в пикселях GUI (0 — по тексту, максимум 210). */
        public Builder width(int v) { width = v; return this; }

        public Notification build() {
            return new Notification(this);
        }
    }
}
