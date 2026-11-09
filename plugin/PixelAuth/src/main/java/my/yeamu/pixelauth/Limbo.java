package my.yeamu.pixelauth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * «Карантин» до входа: игрок висит на месте, ничего не видит и невидим для других.
 * Запоминает исходное состояние и возвращает его после входа (или при выходе с сервера).
 */
final class Limbo {

    private static final float DEFAULT_WALK = 0.2f, DEFAULT_FLY = 0.1f;

    private final boolean allowFlight, flying, blind, hidden;
    private final float walk, fly;
    private final Location back;

    private Limbo(boolean allowFlight, boolean flying, float walk, float fly, Location back, boolean blind, boolean hidden) {
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.walk = walk;
        this.fly = fly;
        this.back = back;
        this.blind = blind;
        this.hidden = hidden;
    }

    static Limbo apply(Plugin plugin, Player p, Settings s) {
        // Скорость сохраняется в данных игрока: если сервер упал во время карантина, там останется 0 —
        // тогда возвращаем стандартные значения.
        float walk = p.getWalkSpeed() <= 0f ? DEFAULT_WALK : p.getWalkSpeed();
        float fly = p.getFlySpeed() <= 0f ? DEFAULT_FLY : p.getFlySpeed();
        Location back = s.teleportToSpawn ? p.getLocation().clone() : null;
        boolean hadBlind = p.hasPotionEffect(PotionEffectType.BLINDNESS);
        Limbo l = new Limbo(p.getAllowFlight(), p.isFlying(), walk, fly, back, s.blindness && !hadBlind, s.hidePlayer);

        if (back != null) p.teleportAsync(p.getWorld().getSpawnLocation());
        p.setAllowFlight(true);
        p.setFlying(true); // не падаем в лаву/пустоту, пока вводим пароль
        p.setWalkSpeed(0f);
        p.setFlySpeed(0f);
        if (l.blind) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (s.timeoutSeconds + 15) * 20, 0, false, false, false));
        }
        if (l.hidden) {
            for (Player o : Bukkit.getOnlinePlayers()) if (o != p) o.hidePlayer(plugin, p);
        }
        return l;
    }

    void restore(Plugin plugin, Player p) {
        p.setWalkSpeed(walk);
        p.setFlySpeed(fly);
        p.setAllowFlight(allowFlight);
        p.setFlying(allowFlight && flying);
        if (blind) p.removePotionEffect(PotionEffectType.BLINDNESS);
        if (hidden) {
            for (Player o : Bukkit.getOnlinePlayers()) if (o != p) o.showPlayer(plugin, p);
        }
        if (back != null) p.teleportAsync(back);
    }
}
