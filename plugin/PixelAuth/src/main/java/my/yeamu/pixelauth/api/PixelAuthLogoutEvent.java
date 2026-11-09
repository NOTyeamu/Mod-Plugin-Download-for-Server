package my.yeamu.pixelauth.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Игрок вышел из аккаунта командой /logout (или через API) и снова в карантине. */
public final class PixelAuthLogoutEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PixelAuthLogoutEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
