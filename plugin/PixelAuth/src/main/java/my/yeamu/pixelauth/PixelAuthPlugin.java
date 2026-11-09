package my.yeamu.pixelauth;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * PixelAuth — авторизация на сервере (замена AuthMe). Работает сама по себе (чат + титры),
 * а если стоит PixelUI — показывает пиксельные окна входа и регистрации игрокам с модом.
 */
public final class PixelAuthPlugin extends JavaPlugin {

    private static PixelAuthPlugin instance;

    private volatile Settings settings;
    private volatile Msg msg;
    private Storage storage;
    private AuthManager auth;
    private Screens screens;
    private LoginLog loginLog;
    private boolean passive;

    public static PixelAuthPlugin instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        settings = new Settings(getConfig());
        msg = new Msg(getConfig());

        storage = new Storage(getDataFolder(), settings);
        try {
            storage.open();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Не удалось открыть базу (" + settings.storageType + "). Плагин выключен!", e);
            // Важно: без авторизации сервер открыт всем — включаем whitelist
            Bukkit.setWhitelist(true);
            getLogger().severe("Включён whitelist, чтобы никто не зашёл без пароля.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        loginLog = new LoginLog(getDataFolder(), storage.executor(), getLogger(), settings.logEnabled);

        passive = settings.passiveIfAuthMe && Bukkit.getPluginManager().isPluginEnabled("AuthMe");
        if (passive) {
            getLogger().warning("Найден AuthMe — PixelAuth в пассивном режиме. Сделайте /pixelauth import-authme, затем удалите AuthMe.");
        }

        screens = new Screens(this);
        auth = new AuthManager(this);
        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        if (screens.pixelAvailable()) {
            try {
                getServer().getPluginManager().registerEvents(new PixelHookListener(this), this);
                getLogger().info("PixelUI найден — игроки с модом увидят пиксельные окна входа.");
            } catch (Throwable t) {
                getLogger().log(Level.WARNING, "Не удалось подключиться к PixelUI", t);
            }
        } else {
            getLogger().info("PixelUI не найден — вход через чат и титры.");
        }

        AuthCommands ac = new AuthCommands(this);
        for (String c : new String[]{"login", "register", "logout", "changepassword"}) bind(c, ac);
        bind("pixelauth", new AdminCommand(this));

        if (settings.hideCommands) {
            try {
                ConsoleFilter.install();
            } catch (Throwable t) {
                getLogger().warning("Не удалось скрыть пароли из консоли: " + t);
            }
        }

        // /reload или включение на живом сервере: те, кто уже онлайн, проходят проверку заново
        // (сессия по IP обычно пустит их сразу).
        if (!passive) {
            for (Player p : Bukkit.getOnlinePlayers()) auth.join(p);
        }
        getLogger().info("PixelAuth " + getPluginMeta().getVersion() + " включён (база: " + settings.storageType + ").");
    }

    private void bind(String name, TabExecutor ex) {
        PluginCommand c = getCommand(name);
        if (c == null) {
            getLogger().warning("Команда /" + name + " не описана в plugin.yml");
            return;
        }
        c.setExecutor(ex);
        c.setTabCompleter(ex);
    }

    @Override
    public void onDisable() {
        if (auth != null) auth.shutdown();
        if (storage != null) storage.close();
        instance = null;
    }

    /** /pixelauth reload: настройки и тексты (база не переподключается). */
    void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig());
        msg = new Msg(getConfig());
        if (loginLog != null) loginLog.setEnabled(settings.logEnabled);
    }

    public Settings settings() { return settings; }
    public Msg msg() { return msg; }
    public Storage storage() { return storage; }
    public AuthManager auth() { return auth; }
    Screens screens() { return screens; }
    LoginLog loginLog() { return loginLog; }
    public boolean passive() { return passive; }
}
