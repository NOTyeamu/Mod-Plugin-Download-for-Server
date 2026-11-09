package my.yeamu.modgate.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Неизменяемый снимок config.yml. Безопасно читать из любого потока.
 */
public record GateConfig(
        Mode mode,
        String siteUrl,
        URI manifestUrl,
        int refreshMinutes,
        int httpTimeoutSeconds,
        String publicKey,
        int handshakeTimeout,
        int kickDelay,
        int reminderInterval,
        boolean checkModsHash,
        boolean exactManifestVersion,
        Set<String> exemptPlayers,
        boolean bedrockEnabled,
        String bedrockPrefix,
        boolean useFloodgateApi,
        boolean logEnabled,
        Messages messages
) {

    /** Режим работы плагина. */
    public enum Mode { STRICT, WARN, OFF }

    /** Тексты сообщений (MiniMessage). */
    public record Messages(String prefix, String noMod, String outdatedMod, String outdatedManifest,
                           String foreignMods, String badHandshake, String quarantine, String warn,
                           String kick, String ok, String kickOutdated) {}

    public static GateConfig load(JavaPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        Mode mode;
        try {
            mode = Mode.valueOf(c.getString("mode", "strict").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неизвестный режим в config.yml, использую strict");
            mode = Mode.STRICT;
        }

        String rawUrl = c.getString("manifest.url", "https://yeamu.my.id/manifest.json");
        URI manifestUrl = URI.create(rawUrl);
        // Разрешаем только HTTPS — иначе манифест можно подменить по дороге
        if (!"https".equalsIgnoreCase(manifestUrl.getScheme())) {
            plugin.getLogger().severe("manifest.url должен начинаться с https:// — загрузка манифеста отключена");
            manifestUrl = null;
        }

        // Ники и UUID храним в нижнем регистре
        Set<String> exempt = new HashSet<>();
        List<String> list = c.getStringList("exemptions.players");
        for (String s : list) exempt.add(s.trim().toLowerCase(Locale.ROOT));

        Messages m = new Messages(
                c.getString("messages.prefix", ""),
                c.getString("messages.reasons.no-mod", "нет мода"),
                c.getString("messages.reasons.outdated-mod", "мод устарел"),
                c.getString("messages.reasons.outdated-manifest", "сборка устарела"),
                c.getString("messages.reasons.foreign-mods", "не хватает модов сборки"),
                c.getString("messages.reasons.bad-handshake", "плохие данные"),
                c.getString("messages.quarantine", "<reason> <url>"),
                c.getString("messages.warn", "<reason> <url>"),
                c.getString("messages.kick", "<reason> <url>"),
                c.getString("messages.ok", ""),
                c.getString("messages.kick-outdated", "")
        );

        return new GateConfig(
                mode,
                c.getString("site-url", "https://yeamu.my.id"),
                manifestUrl,
                c.getInt("manifest.refresh-minutes", 5),
                c.getInt("manifest.timeout-seconds", 10),
                c.getString("manifest.public-key", "").trim(),
                c.getInt("timings.handshake-timeout", 10),
                c.getInt("timings.kick-delay", 30),
                c.getInt("timings.reminder-interval", 10),
                c.getBoolean("checks.mods-hash", true),
                c.getBoolean("checks.exact-manifest-version", false),
                Set.copyOf(exempt),
                c.getBoolean("exemptions.bedrock.enabled", true),
                c.getString("exemptions.bedrock.prefix", "."),
                c.getBoolean("exemptions.bedrock.use-floodgate-api", true),
                c.getBoolean("log.enabled", true),
                m
        );
    }
}
