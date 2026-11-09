package my.yeamu.pixelui.paper.api.event;

import my.yeamu.pixelui.paper.api.PixelMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Игрок нажал кнопку меню. Отмена — обработчики кнопки и меню не вызываются. */
public final class PixelMenuClickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PixelMenu menu;
    private final PixelMenu.Button button;
    private final boolean fallback;
    private boolean cancelled;

    public PixelMenuClickEvent(Player player, PixelMenu menu, PixelMenu.Button button, boolean fallback) {
        this.player = player;
        this.menu = menu;
        this.button = button;
        this.fallback = fallback;
    }

    public Player getPlayer() { return player; }
    public PixelMenu getMenu() { return menu; }
    public PixelMenu.Button getButton() { return button; }
    /** true — нажато в чат-версии меню (у игрока нет мода). */
    public boolean isFallback() { return fallback; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
