package my.yeamu.pixelauth.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Игрок успешно вошёл (главный поток). Подходит, чтобы телепортировать на спавн, выдать кит и т.п. */
public final class PixelAuthLoginEvent extends Event {

    /** Как игрок вошёл. */
    public enum Type { PASSWORD, REGISTER, SESSION, BEDROCK, FORCED }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Type type;

    public PixelAuthLoginEvent(Player player, Type type) {
        this.player = player;
        this.type = type;
    }

    public Player getPlayer() { return player; }
    public Type getType() { return type; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
