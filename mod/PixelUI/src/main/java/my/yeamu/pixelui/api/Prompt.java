package my.yeamu.pixelui.api;

import com.google.gson.JsonObject;

/**
 * Вопрос-плашка: «Принять приглашение в клан?  [Y] Да   [N] Нет».
 * Игрок отвечает клавишами (по умолчанию Y и N) прямо в игре, без открытия экрана.
 * Одновременно показывается один вопрос, остальные ждут в очереди.
 *
 * <pre>
 * PixelUI.prompt(Prompt.builder("duel")
 *         .question("&eSteve вызывает тебя на дуэль")
 *         .text("Ставка: 10 алмазов")
 *         .icon("sword").theme(Theme.NETHER)
 *         .timeout(20_000)
 *         .build(), answer -> { if (answer == PromptAnswer.YES) ... });
 * </pre>
 */
public final class Prompt {

    public final String id;
    public final String question;
    public final String text;
    public final String icon;
    public final Theme theme;
    public final Position position;
    /** 0 — без ограничения времени. */
    public final long timeoutMs;
    public final String yesLabel, noLabel;
    /** Клавиши: буква A–Z или цифра 0–9. */
    public final char yesKey, noKey;
    /** Цвета клавиш (ARGB). */
    public final int yesColor, noColor;
    public final boolean timerBar;
    /** Ширина 0 — автоматически. */
    public final int width;
    public final String sound;

    private Prompt(Builder b) {
        id = b.id == null || b.id.isBlank() ? "prompt" : b.id;
        question = b.question == null ? "" : b.question;
        text = b.text == null ? "" : b.text;
        icon = b.icon;
        theme = b.theme;
        Position p = b.position;
        position = p.isTop() ? Position.TOP_CENTER : p.isBottom() ? Position.BOTTOM_CENTER : Position.CENTER;
        timeoutMs = Math.max(0, b.timeoutMs);
        yesLabel = b.yesLabel == null ? "Да" : b.yesLabel;
        noLabel = b.noLabel == null ? "Нет" : b.noLabel;
        yesKey = key(b.yesKey, 'Y');
        noKey = key(b.noKey == yesKey ? 'N' : b.noKey, yesKey == 'N' ? 'Y' : 'N');
        yesColor = b.yesColor;
        noColor = b.noColor;
        timerBar = b.timerBar;
        width = b.width <= 0 ? 0 : Math.max(160, Math.min(b.width, 360));
        sound = b.sound;
    }

    private static char key(char c, char def) {
        char u = Character.toUpperCase(c);
        return (u >= 'A' && u <= 'Z') || (u >= '0' && u <= '9') ? u : def;
    }

    public static Builder builder(String id) {
        return new Builder().id(id);
    }

    public static Prompt fromJson(JsonObject o) {
        Builder b = builder(o.has("id") ? o.get("id").getAsString() : "prompt");
        if (o.has("question")) b.question(o.get("question").getAsString());
        if (o.has("text")) b.text(o.get("text").getAsString());
        if (o.has("icon")) b.icon(o.get("icon").getAsString());
        if (o.has("theme")) b.theme(Theme.fromJson(o.get("theme")));
        if (o.has("position")) b.position(Position.parse(o.get("position").getAsString(), Position.TOP_CENTER));
        if (o.has("timeout")) b.timeout(o.get("timeout").getAsLong());
        if (o.has("yes")) b.yesLabel(o.get("yes").getAsString());
        if (o.has("no")) b.noLabel(o.get("no").getAsString());
        if (o.has("yesKey")) { String s = o.get("yesKey").getAsString(); if (!s.isEmpty()) b.yesKey(s.charAt(0)); }
        if (o.has("noKey")) { String s = o.get("noKey").getAsString(); if (!s.isEmpty()) b.noKey(s.charAt(0)); }
        if (o.has("yesColor")) b.yesColor(Theme.parseColor(o.get("yesColor").getAsString(), 0xFF3FA535));
        if (o.has("noColor")) b.noColor(Theme.parseColor(o.get("noColor").getAsString(), 0xFFC83737));
        if (o.has("timerBar")) b.timerBar(o.get("timerBar").getAsBoolean());
        if (o.has("width")) b.width(o.get("width").getAsInt());
        if (o.has("sound")) b.sound(o.get("sound").getAsString());
        return b.build();
    }

    public static final class Builder {
        private String id, question, text, icon = "bell", yesLabel, noLabel;
        private String sound = "minecraft:block.note_block.pling";
        private Theme theme = Theme.DARK;
        private Position position = Position.TOP_CENTER;
        private long timeoutMs = 15_000;
        private char yesKey = 'Y', noKey = 'N';
        private int yesColor = 0xFF3FA535, noColor = 0xFFC83737, width;
        private boolean timerBar = true;

        public Builder id(String v) { id = v; return this; }
        public Builder question(String v) { question = v; return this; }
        public Builder text(String v) { text = v; return this; }
        public Builder icon(String v) { icon = v; return this; }
        public Builder theme(Theme v) { theme = v == null ? Theme.DARK : v; return this; }
        public Builder position(Position v) { position = v == null ? Position.TOP_CENTER : v; return this; }
        public Builder timeout(long ms) { timeoutMs = ms; return this; }
        public Builder yesLabel(String v) { yesLabel = v; return this; }
        public Builder noLabel(String v) { noLabel = v; return this; }
        public Builder yesKey(char v) { yesKey = v; return this; }
        public Builder noKey(char v) { noKey = v; return this; }
        public Builder yesColor(int argb) { yesColor = argb | 0xFF000000; return this; }
        public Builder noColor(int argb) { noColor = argb | 0xFF000000; return this; }
        public Builder timerBar(boolean v) { timerBar = v; return this; }
        public Builder width(int v) { width = v; return this; }
        public Builder sound(String v) { sound = v; return this; }

        public Prompt build() {
            return new Prompt(this);
        }
    }
}
