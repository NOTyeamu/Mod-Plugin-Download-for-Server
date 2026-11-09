package my.yeamu.pluginsync;

import my.yeamu.pluginsync.command.PluginSyncCommand;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

/**
 * PluginSync — сам скачивает и обновляет плагины сервера (включая себя)
 * по подписанному списку plugins.json с сайта.
 *
 * Цикл: проверка → скачивание в plugins/PluginSync/pending/ → (авто)перезапуск →
 * при остановке сервера новые jar ставятся в plugins/ → при запуске грузятся новые версии.
 */
public final class PluginSyncPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private volatile SyncConfig config;
    private UpdateService updates;
    private RestartManager restarts;
    private BukkitTask timer;
    private boolean pixelUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = SyncConfig.load(getConfig(), getLogger());
        updates = new UpdateService(this);
        updates.loadPending();
        restarts = new RestartManager(this);
        pixelUI = Bukkit.getPluginManager().isPluginEnabled("PixelUI");

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        PluginCommand cmd = getCommand("pluginsync");
        if (cmd != null) {
            PluginSyncCommand handler = new PluginSyncCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // Первая проверка — когда сервер полностью запустился (все плагины загружены)
        if (config.checkOnStartup() && config.mode() != SyncConfig.Mode.OFF) {
            Bukkit.getScheduler().runTaskLater(this, () -> runCheck(Bukkit.getConsoleSender(), false), 100L);
        }
        scheduleTimer();
        getLogger().info("PluginSync " + getPluginMeta().getVersion() + " запущен (режим " + config.mode().name().toLowerCase() + ")");
    }

    @Override
    public void onDisable() {
        if (timer != null) timer.cancel();
        if (restarts != null) restarts.cancel(false);
        if (updates == null) return;
        if (Bukkit.isStopping()) {
            // Сервер выключается — теперь можно безопасно поменять jar
            updates.applyPending();
        } else {
            // Плагин выключили на ходу (например, плагин-менеджером) — ничего не трогаем
            updates.shutdownWorker();
        }
    }

    // ------------------------------------------------------------------ API для команд и слушателей

    public SyncConfig syncConfig() { return config; }
    public UpdateService updates() { return updates; }
    public RestartManager restarts() { return restarts; }

    public void reload() {
        reloadConfig();
        config = SyncConfig.load(getConfig(), getLogger());
        pixelUI = Bukkit.getPluginManager().isPluginEnabled("PixelUI");
        scheduleTimer();
    }

    private void scheduleTimer() {
        if (timer != null) timer.cancel();
        timer = null;
        if (config.mode() == SyncConfig.Mode.OFF || config.intervalMinutes() <= 0) return;
        long ticks = config.intervalMinutes() * 60L * 20L;
        timer = Bukkit.getScheduler().runTaskTimer(this, () -> runCheck(Bukkit.getConsoleSender(), false), ticks, ticks);
    }

    /**
     * Запустить проверку (из главного потока). Результат сообщается отправителю и админам.
     * @param forceDownload скачать даже в режиме notify (команда /pluginsync update)
     */
    public boolean runCheck(CommandSender sender, boolean forceDownload) {
        CompletableFuture<UpdateService.Report> f = updates.check(forceDownload);
        if (f == null) return false;
        boolean manual = !(sender instanceof org.bukkit.command.ConsoleCommandSender) || forceDownload;
        f.whenComplete((report, err) -> Bukkit.getScheduler().runTask(this, () -> {
            if (err != null) {
                msg(sender, "&cОшибка проверки: " + err.getMessage());
                getLogger().warning("Ошибка проверки: " + err);
                return;
            }
            handleReport(sender, report, manual);
        }));
        return true;
    }

    private void handleReport(CommandSender sender, UpdateService.Report r, boolean manual) {
        if (!r.ok()) {
            if (manual) msg(sender, "&cСписок плагинов недоступен: &f" + r.error());
            return;
        }
        if (r.fromCache() && manual) msg(sender, "&eСайт недоступен, использую кэш v" + r.manifestVersion());
        for (String s : r.failed()) msg(sender, "&c✖ " + s);

        if (!r.available().isEmpty()) {
            String list = String.join(", ", r.available());
            getLogger().info("Доступны обновления: " + list);
            notifyAdmins("info", "Доступны обновления", list,
                    "&6[PluginSync] &eДоступны обновления: &f" + list + " &7(/pluginsync update)");
        }
        if (r.somethingNew()) {
            String list = String.join(", ", updates.pendingNames());
            getLogger().info("Скачано, ждёт перезапуска: " + list);
            notifyAdmins("success", "Обновления скачаны", list + ". Поставятся при перезапуске.",
                    "&6[PluginSync] &aСкачано: &f" + list + " &7— поставится при перезапуске (/pluginsync restart)");
            if (config.restartWhen() == SyncConfig.RestartWhen.ALWAYS && !restarts.isRunning()) {
                restarts.start(config.restartCountdown(), "скачаны обновления");
            } else {
                maybeRestart();
            }
        } else if (manual && r.available().isEmpty() && r.failed().isEmpty()) {
            msg(sender, updates.hasPending()
                    ? "&bНового нет. Ждут перезапуска: &f" + String.join(", ", updates.pendingNames())
                    : "&aВсе плагины актуальны &7(список v" + r.manifestVersion() + ")");
        }
    }

    /** Режим restart.when: empty — перезапуск, когда на сервере никого нет и есть скачанное. */
    public void maybeRestart() {
        if (config.restartWhen() != SyncConfig.RestartWhen.EMPTY) return;
        if (!updates.hasPending() || restarts.isRunning() || updates.isBusy()) return;
        if (!Bukkit.getOnlinePlayers().isEmpty()) return;
        restarts.start(10, "сервер пуст, ставим обновления");
    }

    // ------------------------------------------------------------------ сообщения

    public void msg(CommandSender to, String legacy) {
        to.sendMessage(LEGACY.deserialize(legacy));
    }

    /**
     * Уведомление игроку: через PixelUI (если стоит плагин и клиентский мод), иначе в чат.
     */
    public void notify(Player p, String kind, String title, String text, String chatFallback) {
        if (pixelUI && config.usePixelUI()) {
            try {
                if (PixelHook.notify(p, kind, title, text, 6000L)) return;
            } catch (Throwable t) {
                pixelUI = false; // PixelUI сломан или другой версии — дальше только чат
                getLogger().warning("PixelUI недоступен: " + t);
            }
        }
        msg(p, chatFallback);
    }

    private void notifyAdmins(String kind, String title, String text, String chat) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("pluginsync.admin")) notify(p, kind, title, text, chat);
        }
    }
}
