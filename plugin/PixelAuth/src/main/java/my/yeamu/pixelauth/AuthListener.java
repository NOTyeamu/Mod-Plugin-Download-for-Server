package my.yeamu.pixelauth;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Locale;

/**
 * Блокирует всё, пока игрок не вошёл. Приоритет LOWEST — чтобы другие плагины уже
 * видели отменённое событие и не реагировали на него.
 */
final class AuthListener implements Listener {

    private final PixelAuthPlugin plugin;

    AuthListener(PixelAuthPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean locked(Player p) {
        return !plugin.auth().isAuthed(p);
    }

    private void block(Player p, Cancellable e) {
        if (locked(p)) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- вход / выход

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (plugin.passive() || e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        plugin.auth().preLogin(e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.passive()) return;
        plugin.auth().join(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.passive()) return;
        plugin.auth().quit(e.getPlayer());
    }

    // ---------------------------------------------------------------- карантин

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedPosition() || !locked(e.getPlayer())) return;
        // поворачивать голову можно, ходить — нет
        e.setTo(e.getFrom().clone().setDirection(e.getTo().getDirection()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!locked(e.getPlayer())) return;
        e.setCancelled(true);
        plugin.msg().send(e.getPlayer(), "need-auth");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!locked(e.getPlayer())) return;
        String msg = e.getMessage();
        String label = msg.length() > 1 ? msg.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT) : "";
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1); // pixelauth:login
        if (plugin.settings().allowedCommands.contains(label)) return;
        e.setCancelled(true);
        plugin.msg().send(e.getPlayer(), "need-auth");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) { block(e.getPlayer(), e); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        // мобы не агрятся на игрока, пока он вводит пароль
        if (e.getTarget() instanceof Player p) block(p, e);
    }
}
