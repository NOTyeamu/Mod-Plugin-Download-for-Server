package my.yeamu.pixelui.paper;

import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.command.PixelUICommand;
import my.yeamu.pixelui.paper.internal.PixelService;
import my.yeamu.pixelui.paper.internal.Protocol;
import my.yeamu.pixelui.paper.internal.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Главный класс плагина PixelUI.
 * Канал "pixelui:main": сервер шлёт команды интерфейса, клиент — hello/click/close.
 * Формат пакета: VarInt длина + UTF-8 JSON (так его читает PacketByteBuf.readString на клиенте).
 */
public final class PixelUIPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    public static final String CHANNEL = "pixelui:main";

    private static PixelUIPlugin instance;
    private Settings settings;
    private PixelService service;

    /** Экземпляр плагина. Бросает исключение, если PixelUI не включён. */
    public static PixelUIPlugin get() {
        PixelUIPlugin p = instance;
        if (p == null) throw new IllegalStateException("PixelUI ещё не включён (добавьте depend: [PixelUI] в plugin.yml)");
        return p;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadSettings();
        service = new PixelService(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getCommand("pixelui");
        if (cmd != null) {
            PixelUICommand c = new PixelUICommand(this);
            cmd.setExecutor(c);
            cmd.setTabCompleter(c);
        }
        getLogger().info("PixelUI " + getPluginMeta().getVersion() + " включён");
    }

    @Override
    public void onDisable() {
        if (service != null) service.shutdown();
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        instance = null;
    }

    public void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
    }

    public Settings settings() {
        return settings;
    }

    public PixelService service() {
        return service;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        JsonObject o = Protocol.decode(message);
        if (o == null) return;
        // Обработка (события, обработчики кнопок) — всегда в главном потоке
        if (Bukkit.isPrimaryThread()) service.handle(player, o);
        else Bukkit.getScheduler().runTask(this, () -> service.handle(player, o));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        service.quit(e.getPlayer());
    }
}
