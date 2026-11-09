package my.yeamu.modgate.gate;

import my.yeamu.modgate.ModGatePlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

/**
 * Принимает пакеты канала modgate:handshake от клиента.
 */
public final class HandshakeListener implements PluginMessageListener {

    private final ModGatePlugin plugin;

    public HandshakeListener(ModGatePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!ModGatePlugin.CHANNEL.equals(channel)) return;
        if (message.length > 64 * 1024) { // защита от мусора
            plugin.gateManager().onBadHandshake(player, "пакет слишком большой");
            return;
        }
        try {
            HandshakeData data = HandshakeData.decode(message);
            plugin.gateManager().onHandshake(player, data);
        } catch (Exception e) {
            plugin.gateManager().onBadHandshake(player, e.getMessage());
        }
    }
}
