package my.yeamu.pixelui.paper.internal;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/** Настройки из config.yml. */
public record Settings(String notifyFallback, String bannerFallback, String menuFallback, String promptFallback,
                       boolean logHello) {

    public static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("fallback.notify", "chat").toLowerCase(Locale.ROOT),
                c.getString("fallback.banner", "bossbar").toLowerCase(Locale.ROOT),
                c.getString("fallback.menu", "chat").toLowerCase(Locale.ROOT),
                c.getString("fallback.prompt", "chat").toLowerCase(Locale.ROOT),
                c.getBoolean("log-hello", true));
    }
}
