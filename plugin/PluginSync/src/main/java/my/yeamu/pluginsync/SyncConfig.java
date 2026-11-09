package my.yeamu.pluginsync;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/** Настройки из config.yml (неизменяемый снимок, безопасно читать из любого потока). */
public record SyncConfig(
        URI manifestUrl,
        String publicKey,
        int timeoutSeconds,
        boolean checkOnStartup,
        int intervalMinutes,
        Mode mode,
        boolean installMissing,
        boolean allowDowngrade,
        Set<String> managed,
        Set<String> ignore,
        long maxSizeBytes,
        Set<String> allowedDomains,
        RestartWhen restartWhen,
        int restartCountdown,
        String restartMethod,
        boolean notifyAdminsOnJoin,
        boolean usePixelUI,
        int parallelDownloads,
        int batchWaitSeconds) {

    public enum Mode { AUTO, NOTIFY, OFF }

    public enum RestartWhen { NEVER, EMPTY, ALWAYS }

    public static SyncConfig load(FileConfiguration c, Logger log) {
        URI url = null;
        String raw = c.getString("manifest.url", "").trim();
        try {
            URI u = URI.create(raw);
            if ("https".equalsIgnoreCase(u.getScheme()) && u.getHost() != null) url = u;
            else log.severe("manifest.url должен быть HTTPS: " + raw);
        } catch (Exception e) {
            log.severe("Неверный manifest.url: " + raw);
        }
        String key = c.getString("manifest.public-key", "").trim();
        if (key.isEmpty()) log.severe("manifest.public-key пуст — без проверки подписи ничего скачиваться не будет");
        return new SyncConfig(
                url,
                key,
                Math.max(5, c.getInt("manifest.timeout-seconds", 30)),
                c.getBoolean("check.on-startup", true),
                Math.max(0, c.getInt("check.interval-minutes", 30)),
                parse(Mode.class, c.getString("check.mode", "auto"), Mode.AUTO, log),
                c.getBoolean("install-missing", true),
                c.getBoolean("allow-downgrade", false),
                lower(c.getStringList("managed")),
                lower(c.getStringList("ignore")),
                Math.max(1, c.getLong("max-size-mb", 100)) * 1024L * 1024L,
                lower(c.getStringList("allowed-domains")),
                parse(RestartWhen.class, c.getString("restart.when", "never"), RestartWhen.NEVER, log),
                Math.max(5, c.getInt("restart.countdown", 60)),
                c.getString("restart.method", "restart").toLowerCase(Locale.ROOT),
                c.getBoolean("notify.admins-on-join", true),
                c.getBoolean("notify.pixelui", true),
                Math.max(1, Math.min(16, c.getInt("download.parallel", 4))),
                Math.max(0, Math.min(3600, c.getInt("restart.batch-wait-seconds", 90))));
    }

    /** Управляем ли мы этим плагином (с учётом managed/ignore). */
    public boolean manages(String pluginName) {
        String n = pluginName.toLowerCase(Locale.ROOT);
        if (ignore.contains(n)) return false;
        return managed.isEmpty() || managed.contains(n);
    }

    /** Домен или его поддомен в белом списке. */
    public boolean domainAllowed(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        for (String d : allowedDomains) {
            if (h.equals(d) || h.endsWith("." + d)) return true;
        }
        return false;
    }

    private static Set<String> lower(java.util.List<String> list) {
        Set<String> out = new HashSet<>();
        for (String s : list) if (s != null && !s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        return Set.copyOf(out);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String v, E def, Logger log) {
        try {
            return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warning("Неизвестное значение «" + v + "», использую " + def.name().toLowerCase(Locale.ROOT));
            return def;
        }
    }
}
