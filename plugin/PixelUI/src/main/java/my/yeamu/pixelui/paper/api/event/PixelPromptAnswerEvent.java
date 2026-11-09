package my.yeamu.pixelui.paper.api.event;

import my.yeamu.pixelui.paper.api.PixelAnswer;
import my.yeamu.pixelui.paper.api.PixelPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Игрок ответил на вопрос (или время вышло / вопрос отменён).
 * Вызывается для вопросов любого плагина — можно слушать чужие вопросы по id.
 * Отмена события — не вызывать onYes/onNo/onTimeout/onAnswer этого вопроса.
 */
public final class PixelPromptAnswerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PixelPrompt prompt;
    private final PixelAnswer answer;
    private final boolean fallback;
    private boolean cancelled;

    public PixelPromptAnswerEvent(Player player, PixelPrompt prompt, PixelAnswer answer, boolean fallback) {
        this.player = player;
        this.prompt = prompt;
        this.answer = answer;
        this.fallback = fallback;
    }

    public Player getPlayer() { return player; }
    public PixelPrompt getPrompt() { return prompt; }
    public String getPromptId() { return prompt.id(); }
    public PixelAnswer getAnswer() { return answer; }
    /** true — ответ через чат (у игрока нет мода). */
    public boolean isFallback() { return fallback; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { cancelled = cancel; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
