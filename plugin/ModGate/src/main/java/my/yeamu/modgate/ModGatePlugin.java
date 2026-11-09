package my.yeamu.modgate;

import my.yeamu.modgate.command.ModGateCommand;
import my.yeamu.modgate.config.GateConfig;
import my.yeamu.modgate.gate.GateManager;
import my.yeamu.modgate.gate.HandshakeListener;
import my.yeamu.modgate.gate.QuarantineListener;
import my.yeamu.modgate.log.JoinLogger;
import my.yeamu.modgate.manifest.ManifestService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Главный класс плагина ModGate.
 * Связывает конфиг, загрузку манифеста, проверку рукопожатий и команды.
 */
public final class ModGatePlugin extends JavaPlugin {

    /** Имя двустороннего канала плагин-сообщений. */
    public static final String CHANNEL = "modgate:handshake";

    private volatile GateConfig gateConfig;
    private ManifestService manifestService;
    private GateManager gateManager;
    private JoinLogger joinLogger;
    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gateConfig = GateConfig.load(this);
        this.joinLogger = new JoinLogger(this);
        this.manifestService = new ManifestService(this);
        this.gateManager = new GateManager(this);

        // Регистрируем канал в обе стороны
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, new HandshakeListener(this));

        getServer().getPluginManager().registerEvents(gateManager, this);
        getServer().getPluginManager().registerEvents(new QuarantineListener(this), this);

        PluginCommand cmd = getCommand("modgate");
        if (cmd != null) {
            ModGateCommand executor = new ModGateCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Кэш с диска — на случай, если сайт сейчас недоступен
        manifestService.loadCache();
        scheduleRefresh();
        getLogger().info("ModGate включён, режим: " + gateConfig.mode());
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) refreshTask.cancel();
        if (gateManager != null) gateManager.shutdown();
        if (joinLogger != null) joinLogger.close();
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    /** Перезапускает периодическую загрузку манифеста (при старте и после /modgate reload). */
    private void scheduleRefresh() {
        if (refreshTask != null) refreshTask.cancel();
        long period = Math.max(1, gateConfig.refreshMinutes()) * 60L * 20L;
        // Задача лишь запускает асинхронный HTTP-запрос — главный поток не блокируется
        refreshTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> manifestService.refresh(), 1L, period);
    }

    /** Перечитывает конфиг и сразу обновляет манифест. */
    public void reloadEverything() {
        reloadConfig();
        this.gateConfig = GateConfig.load(this);
        joinLogger.reopen();
        scheduleRefresh();
    }

    public GateConfig gateConfig() { return gateConfig; }
    public ManifestService manifestService() { return manifestService; }
    public GateManager gateManager() { return gateManager; }
    public JoinLogger joinLogger() { return joinLogger; }
}
