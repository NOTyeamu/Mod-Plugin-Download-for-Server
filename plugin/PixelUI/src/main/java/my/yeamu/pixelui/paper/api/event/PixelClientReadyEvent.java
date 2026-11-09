package my.yeamu.pixelui.paper.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** У игрока есть мод PixelUI — можно слать кастомный интерфейс (например, приветствие при входе). */
public final class PixelClientReadyEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String modVersion;

    public PixelClientReadyEvent(Player player, String modVersion) {
        this.player = player;
        this.modVersion = modVersion;
    }

    public Player getPlayer() { return player; }
    public String getModVersion() { return modVersion; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
