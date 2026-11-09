package my.yeamu.pixelauth;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Настройки из config.yml (неизменяемый снимок, при /pixelauth reload создаётся заново). */
public final class Settings {

    public final String storageType;
    public final String mysqlHost, mysqlDatabase, mysqlUser, mysqlPassword, mysqlParams;
    public final int mysqlPort;

    public final int timeoutSeconds, maxAttempts, uiDelayTicks, reminderSeconds;
    public final boolean sessionEnabled;
    public final int sessionMinutes;

    public final int maxAccountsPerIp, passwordMin, passwordMax;
    public final Set<String> unsafePasswords;

    public final int ipBlockKicks, ipBlockMinutes;

    public final Pattern nameRegex;
    public final boolean nameCaseSensitive;

    public final boolean blindness, hidePlayer, teleportToSpawn;
    public final Set<String> allowedCommands;

    public final boolean bedrockAutoLogin;
    public final String bedrockPrefix;

    public final int hashIterations;
    public final boolean hideCommands, passiveIfAuthMe, logEnabled;

    public final boolean resetEnabled, resetRequireSameIp;
    public final int resetCooldownMinutes, resetValidHours, resetPendingDays;

    public Settings(FileConfiguration c) {
        storageType = c.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        mysqlHost = c.getString("storage.mysql.host", "localhost");
        mysqlPort = c.getInt("storage.mysql.port", 3306);
        mysqlDatabase = c.getString("storage.mysql.database", "minecraft");
        mysqlUser = c.getString("storage.mysql.user", "root");
        mysqlPassword = c.getString("storage.mysql.password", "");
        mysqlParams = c.getString("storage.mysql.params", "useSSL=false");

        timeoutSeconds = clamp(c.getInt("login.timeout-seconds", 60), 10, 3600);
        maxAttempts = clamp(c.getInt("login.max-attempts", 5), 1, 100);
        uiDelayTicks = clamp(c.getInt("login.ui-delay-ticks", 20), 0, 200);
        reminderSeconds = clamp(c.getInt("login.reminder-seconds", 10), 3, 600);
        sessionEnabled = c.getBoolean("session.enabled", true);
        sessionMinutes = clamp(c.getInt("session.minutes", 30), 1, 60 * 24 * 30);

        maxAccountsPerIp = Math.max(0, c.getInt("register.max-accounts-per-ip", 3));
        passwordMin = clamp(c.getInt("register.password-min", 6), 1, 64);
        passwordMax = clamp(c.getInt("register.password-max", 64), passwordMin, 128);
        Set<String> unsafe = new HashSet<>();
        for (String s : c.getStringList("register.unsafe-passwords")) unsafe.add(s.toLowerCase(Locale.ROOT));
        unsafePasswords = Set.copyOf(unsafe);

        ipBlockKicks = Math.max(0, c.getInt("ip-block.kicks", 3));
        ipBlockMinutes = clamp(c.getInt("ip-block.minutes", 10), 1, 60 * 24);

        Pattern p;
        try {
            p = Pattern.compile(c.getString("names.regex", "^[A-Za-z0-9_]{3,16}$"));
        } catch (Exception e) {
            p = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
        }
        nameRegex = p;
        nameCaseSensitive = c.getBoolean("names.case-sensitive", true);

        blindness = c.getBoolean("limbo.blindness", true);
        hidePlayer = c.getBoolean("limbo.hide-player", true);
        teleportToSpawn = c.getBoolean("limbo.teleport-to-spawn", false);
        Set<String> cmds = new HashSet<>();
        List<String> list = c.getStringList("limbo.allowed-commands");
        if (list.isEmpty()) list = List.of("login", "l", "log", "register", "reg");
        for (String s : list) cmds.add(s.toLowerCase(Locale.ROOT).replace("/", ""));
        // запрос сброса пароля должен работать и со старым config.yml
        cmds.addAll(List.of("forgotpassword", "resetpassword", "forgot"));
        allowedCommands = Set.copyOf(cmds);

        bedrockAutoLogin = c.getBoolean("bedrock.auto-login", true);
        bedrockPrefix = c.getString("bedrock.prefix", ".");

        hashIterations = clamp(c.getInt("hash.iterations", 120000), 10000, 2_000_000);
        resetEnabled = c.getBoolean("reset.enabled", true);
        resetCooldownMinutes = clamp(c.getInt("reset.cooldown-minutes", 10), 0, 60 * 24);
        resetValidHours = clamp(c.getInt("reset.valid-hours", 24), 1, 24 * 30);
        resetPendingDays = clamp(c.getInt("reset.pending-days", 7), 1, 365);
        resetRequireSameIp = c.getBoolean("reset.require-same-ip", true);
        hideCommands = c.getBoolean("security.hide-commands-in-console", true);
        passiveIfAuthMe = c.getBoolean("compat.passive-if-authme", true);
        logEnabled = c.getBoolean("log.enabled", true);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
