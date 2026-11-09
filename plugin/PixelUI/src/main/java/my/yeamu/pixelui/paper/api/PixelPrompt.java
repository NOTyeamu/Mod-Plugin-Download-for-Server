package my.yeamu.pixelui.paper.api;

import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.internal.Txt;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Вопрос-плашка «Да (Y) / Нет (N)». Игрок с модом отвечает клавишами прямо в игре,
 * без мода — кнопками [Да] [Нет] в чате. Обработчики вызываются в главном потоке.
 * <pre>
 * PixelPrompt.builder("clan-invite")
 *     .question("&eВас пригласили в клан &6Dragons")
 *     .text("Принять приглашение?")
 *     .icon(PixelIcon.SHIELD).theme(PixelTheme.GOLD)
 *     .timeout(Duration.ofSeconds(20))
 *     .onYes(p -&gt; clans.join(p, "Dragons"))
 *     .onNo(p -&gt; p.sendMessage("Отказано"))
 *     .send(player);
 * </pre>
 */
public final class PixelPrompt {

    private final String id, question, text, icon, sound, yesLabel, noLabel;
    private final PixelTheme theme;
    private final PixelPosition position;
    private final long timeoutMs;
    private final char yesKey, noKey;
    private final int yesColor, noColor, width;
    private final boolean timerBar;
    private final Consumer<Player> onYes, onNo, onTimeout;
    private final BiConsumer<Player, PixelAnswer> onAnswer;

    private PixelPrompt(Builder b) {
        id = b.id;
        question = b.question;
        text = b.text;
        icon = b.icon;
        sound = b.sound;
        yesLabel = b.yesLabel;
        noLabel = b.noLabel;
        theme = b.theme;
        position = b.position;
        timeoutMs = b.timeoutMs;
        yesKey = b.yesKey;
        noKey = b.noKey;
        yesColor = b.yesColor;
        noColor = b.noColor;
        width = b.width;
        timerBar = b.timerBar;
        onYes = b.onYes;
        onNo = b.onNo;
        onTimeout = b.onTimeout;
        onAnswer = b.onAnswer;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() { return id; }
    public String question() { return question; }
    public String text() { return text; }
    public String icon() { return icon; }
    public String sound() { return sound; }
    public String yesLabel() { return yesLabel; }
    public String noLabel() { return noLabel; }
    public PixelTheme theme() { return theme; }
    public PixelPosition position() { return position; }
    public long timeoutMs() { return timeoutMs; }
    public char yesKey() { return yesKey; }
    public char noKey() { return noKey; }
    public Consumer<Player> onYes() { return onYes; }
    public Consumer<Player> onNo() { return onNo; }
    public Consumer<Player> onTimeout() { return onTimeout; }
    public BiConsumer<Player, PixelAnswer> onAnswer() { return onAnswer; }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "prompt");
        o.addProperty("id", id);
        o.addProperty("question", Txt.section(question));
        o.addProperty("text", Txt.section(text));
        o.addProperty("icon", icon);
        o.add("theme", theme.toJson());
        o.addProperty("position", position.name());
        o.addProperty("timeout", timeoutMs);
        o.addProperty("yes", Txt.section(yesLabel));
        o.addProperty("no", Txt.section(noLabel));
        o.addProperty("yesKey", String.valueOf(yesKey));
        o.addProperty("noKey", String.valueOf(noKey));
        o.addProperty("yesColor", String.format("#%06X", yesColor & 0xFFFFFF));
        o.addProperty("noColor", String.format("#%06X", noColor & 0xFFFFFF));
        o.addProperty("timerBar", timerBar);
        if (width > 0) o.addProperty("width", width);
        if (sound != null) o.addProperty("sound", sound);
        return o;
    }

    public static final class Builder {
        private final String id;
        private String question = "", text = "", icon = PixelIcon.BELL, yesLabel = "Да", noLabel = "Нет";
        private String sound = "minecraft:block.note_block.pling";
        private PixelTheme theme = PixelTheme.DARK;
        private PixelPosition position = PixelPosition.TOP_CENTER;
        private long timeoutMs = 15_000;
        private char yesKey = 'Y', noKey = 'N';
        private int yesColor = 0x3FA535, noColor = 0xC83737, width;
        private boolean timerBar = true;
        private Consumer<Player> onYes, onNo, onTimeout;
        private BiConsumer<Player, PixelAnswer> onAnswer;

        private Builder(String id) {
            this.id = id == null || id.isBlank() ? "prompt" : id;
        }

        /** Главный вопрос (до 3 строк). &-коды и MiniMessage. */
        public Builder question(String v) { question = v == null ? "" : v; return this; }
        /** Пояснение под вопросом (до 5 строк, \n — новая строка). */
        public Builder text(String v) { text = v == null ? "" : v; return this; }
        public Builder icon(String v) { icon = v == null ? PixelIcon.NONE : v; return this; }
        public Builder theme(PixelTheme v) { theme = Objects.requireNonNull(v); return this; }
        /** TOP_CENTER, CENTER или BOTTOM_CENTER. */
        public Builder position(PixelPosition v) { position = Objects.requireNonNull(v); return this; }
        /** 0 — без ограничения времени. */
        public Builder timeout(long ms) { timeoutMs = Math.max(0, ms); return this; }
        public Builder timeout(Duration d) { return timeout(d.toMillis()); }
        public Builder yesLabel(String v) { yesLabel = v == null ? "Да" : v; return this; }
        public Builder noLabel(String v) { noLabel = v == null ? "Нет" : v; return this; }
        /** Свои клавиши: буква A–Z или цифра 0–9 (по умолчанию Y и N). */
        public Builder keys(char yes, char no) {
            yesKey = Character.toUpperCase(yes);
            noKey = Character.toUpperCase(no);
            return this;
        }
        /** Цвета клавиш 0xRRGGBB. */
        public Builder colors(int yes, int no) { yesColor = yes; noColor = no; return this; }
        /** Полоска оставшегося времени внизу. */
        public Builder timerBar(boolean v) { timerBar = v; return this; }
        /** Ширина 160..360, 0 — авто. */
        public Builder width(int v) { width = v; return this; }
        /** Звук появления (null — без звука). */
        public Builder sound(String v) { sound = v; return this; }
        public Builder onYes(Consumer<Player> v) { onYes = v; return this; }
        public Builder onNo(Consumer<Player> v) { onNo = v; return this; }
        public Builder onTimeout(Consumer<Player> v) { onTimeout = v; return this; }
        /** Любой исход, включая CANCELLED. */
        public Builder onAnswer(BiConsumer<Player, PixelAnswer> v) { onAnswer = v; return this; }

        public PixelPrompt build() {
            return new PixelPrompt(this);
        }

        public void send(Player player) {
            PixelUI.prompt(player, build());
        }
    }
}
