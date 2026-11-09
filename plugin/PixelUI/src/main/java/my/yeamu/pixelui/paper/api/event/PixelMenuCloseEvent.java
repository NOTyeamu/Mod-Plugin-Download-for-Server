package my.yeamu.pixelui.paper.api.event;

import my.yeamu.pixelui.paper.api.PixelMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Меню закрылось. */
public final class PixelMenuCloseEvent extends Event {

    public enum Reason {
        /** Игрок нажал Esc. */ PLAYER,
        /** Нажата кнопка с close. */ BUTTON,
        /** Открыто другое меню. */ REPLACED,
        /** Закрыл плагин (PixelUI.closeMenu). */ SERVER,
        /** Игрок вышел. */ QUIT
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PixelMenu menu;
    private final Reason reason;

    public PixelMenuCloseEvent(Player player, PixelMenu menu, Reason reason) {
        this.player = player;
        this.menu = menu;
        this.reason = reason;
    }

    public Player getPlayer() { return player; }
    public PixelMenu getMenu() { return menu; }
    public Reason getReason() { return reason; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
