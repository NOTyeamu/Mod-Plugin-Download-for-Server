package my.yeamu.pixelui.paper.internal;

import com.google.gson.JsonObject;
import my.yeamu.pixelui.paper.PixelUIPlugin;
import my.yeamu.pixelui.paper.api.PixelBanner;
import my.yeamu.pixelui.paper.api.PixelMenu;
import my.yeamu.pixelui.paper.api.PixelNotification;
import my.yeamu.pixelui.paper.api.event.PixelClientReadyEvent;
import my.yeamu.pixelui.paper.api.event.PixelMenuClickEvent;
import my.yeamu.pixelui.paper.api.event.PixelMenuCloseEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Вся логика: отправка пакетов клиенту, запасные варианты для игроков без мода,
 * учёт открытых меню и обработка нажатий. Всё состояние меняется в главном потоке.
 */
public final class PixelService {

    /** Открытое меню: token защищает чат-кнопки от старых/поддельных кликов. */
    private record OpenMenu(PixelMenu menu, String token, boolean fallback) {}

    /** Запасная плашка-боссбар. */
    private static final class BarState {
        final BossBar bar;
        long expireAt;

        BarState(BossBar bar) {
            this.bar = bar;
        }
    }

    private final PixelUIPlugin plugin;
    private final Map<UUID, String> versions = new ConcurrentHashMap<>();
    private final Map<UUID, OpenMenu> menus = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, BarState>> bars = new ConcurrentHashMap<>();

    public PixelService(PixelUIPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- состояние

    public boolean hasMod(Player p) {
        return versions.containsKey(p.getUniqueId()) || p.getListeningPluginChannels().contains(PixelUIPlugin.CHANNEL);
    }

    public String version(Player p) {
        return versions.get(p.getUniqueId());
    }

    public PixelMenu openedMenu(Player p) {
        OpenMenu m = menus.get(p.getUniqueId());
        return m == null ? null : m.menu();
    }

    public int moddedOnline() {
        int n = 0;
        for (Player p : Bukkit.getOnlinePlayers()) if (hasMod(p)) n++;
        return n;
    }

    /** Выполнить в главном потоке (сразу, если мы уже в нём). */
    private void sync(Runnable r) {
        if (Bukkit.isPrimaryThread()) r.run();
        else if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, r);
    }

    private void send(Player p, JsonObject o) {
        try {
            p.sendPluginMessage(plugin, PixelUIPlugin.CHANNEL, Protocol.encode(o));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось отправить пакет " + p.getName() + ": " + e.getMessage());
        }
    }

    private static void playSound(Player p, String id, float pitch) {
        if (id == null || id.isBlank()) return;
        try {
            p.playSound(Sound.sound(Key.key(id), Sound.Source.MASTER, 1f, pitch));
        } catch (Exception ignored) {
            // неверный id звука — просто без звука
        }
    }

    // ---------------------------------------------------------------- уведомления

    public void notify(Player p, PixelNotification n) {
        sync(() -> {
            if (!p.isOnline()) return;
            if (hasMod(p)) {
                send(p, n.toJson());
                return;
            }
            Component title = Txt.component(n.title());
            Component text = Txt.component(n.text());
            switch (plugin.settings().notifyFallback()) {
                case "none" -> { return; }
                case "actionbar" -> p.sendActionBar(n.title().isEmpty() ? text
                        : title.append(Component.text(" — ", NamedTextColor.DARK_GRAY)).append(text));
                case "title" -> p.showTitle(Title.title(title, text, Title.Times.times(
                        Duration.ofMillis(200), Duration.ofMillis(n.durationMs()), Duration.ofMillis(300))));
                default -> {
                    Component msg = Component.text("[!] ", n.theme().chatColor(), TextDecoration.BOLD)
                            .append(title.decoration(TextDecoration.BOLD, false));
                    if (!n.text().isEmpty()) {
                        msg = msg.append(Component.newline()).append(text.colorIfAbsent(NamedTextColor.GRAY));
                    }
                    p.sendMessage(msg);
                }
            }
            playSound(p, n.sound(), n.pitch());
        });
    }

    // ---------------------------------------------------------------- плашки

    public void banner(Player p, PixelBanner b) {
        sync(() -> {
            if (!p.isOnline()) return;
            if (hasMod(p)) {
                send(p, b.toJson());
                return;
            }
            switch (plugin.settings().bannerFallback()) {
                case "none" -> { }
                case "actionbar" -> p.sendActionBar(Txt.component(b.text()));
                default -> showBossBar(p, b);
            }
            playSound(p, b.sound(), 1f);
        });
    }

    private void showBossBar(Player p, PixelBanner b) {
        Map<String, BarState> map = bars.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        float prog = b.progress() < 0 ? 1f : b.progress();
        Component name = Txt.component(b.text());
        BarState st = map.get(b.id());
        if (st == null) {
            st = new BarState(BossBar.bossBar(name, prog, b.theme().barColor(), BossBar.Overlay.PROGRESS));
            map.put(b.id(), st);
            p.showBossBar(st.bar);
        } else {
            st.bar.name(name);
            st.bar.progress(prog);
            st.bar.color(b.theme().barColor());
        }
        if (b.durationMs() > 0) {
            long expire = System.currentTimeMillis() + b.durationMs();
            st.expireAt = expire;
            final BarState ref = st;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // убираем, только если плашку не продлили новой отправкой
                if (ref.expireAt == expire) removeBar(p, b.id());
            }, Math.max(1L, b.durationMs() / 50L));
        } else {
            st.expireAt = 0;
        }
    }

    private void removeBar(Player p, String id) {
        Map<String, BarState> map = bars.get(p.getUniqueId());
        if (map == null) return;
        if (id == null) {
            for (BarState st : map.values()) p.hideBossBar(st.bar);
            map.clear();
        } else {
            BarState st = map.remove(id);
            if (st != null) p.hideBossBar(st.bar);
        }
    }

    public void removeBanner(Player p, String id) {
        sync(() -> {
            if (hasMod(p)) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "banner_remove");
                if (id != null) o.addProperty("id", id);
                send(p, o);
            }
            removeBar(p, id);
        });
    }

    public void clear(Player p) {
        sync(() -> {
            if (hasMod(p)) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "clear");
                send(p, o);
            }
            removeBar(p, null);
        });
    }

    // ---------------------------------------------------------------- меню

    public void openMenu(Player p, PixelMenu menu) {
        sync(() -> {
            if (!p.isOnline()) return;
            OpenMenu old = menus.remove(p.getUniqueId());
            if (old != null) fireClose(p, old.menu(), PixelMenuCloseEvent.Reason.REPLACED);

            boolean mod = hasMod(p);
            if (!mod && "none".equals(plugin.settings().menuFallback())) return;
            String token = Long.toString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE, 36);
            menus.put(p.getUniqueId(), new OpenMenu(menu, token, !mod));
            if (mod) send(p, menu.toJson());
            else sendChatMenu(p, menu, token);
        });
    }

    /** Запасное меню в чате: строки + кликабельные [кнопки]. */
    private void sendChatMenu(Player p, PixelMenu menu, String token) {
        NamedTextColor accent = menu.theme().chatColor();
        Component sep = Component.text("■■■■■■■■■■■■■■■■■■■■", NamedTextColor.DARK_GRAY);
        p.sendMessage(sep);
        p.sendMessage(Component.text("▶ ", accent).append(Txt.component(menu.title()).decorate(TextDecoration.BOLD)));
        for (String line : menu.lines()) p.sendMessage(Txt.component(line).colorIfAbsent(NamedTextColor.GRAY));
        Component row = Component.empty();
        for (PixelMenu.Button b : menu.buttons()) {
            NamedTextColor c = switch (b.style()) {
                case "success" -> NamedTextColor.GREEN;
                case "danger" -> NamedTextColor.RED;
                case "info", "primary" -> NamedTextColor.AQUA;
                case "gold" -> NamedTextColor.GOLD;
                default -> NamedTextColor.WHITE;
            };
            Component btn;
            if (b.enabled()) {
                btn = Component.text("[", c).append(Txt.component(b.label())).append(Component.text("]", c))
                        .clickEvent(ClickEvent.runCommand("/pixelui click " + token + " " + b.id()));
                if (b.tooltip() != null) btn = btn.hoverEvent(HoverEvent.showText(Txt.component(b.tooltip())));
            } else {
                btn = Component.text("[" + Txt.plain(b.label()) + "]", NamedTextColor.DARK_GRAY);
            }
            row = row.append(btn).append(Component.text(" "));
        }
        if (!menu.buttons().isEmpty()) p.sendMessage(row);
        p.sendMessage(sep);
    }

    public void closeMenu(Player p) {
        sync(() -> {
            OpenMenu m = menus.remove(p.getUniqueId());
            if (m == null) return;
            if (!m.fallback()) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "menu_close");
                o.addProperty("id", m.menu().id());
                send(p, o);
            }
            fireClose(p, m.menu(), PixelMenuCloseEvent.Reason.SERVER);
        });
    }

    /** Клик из чат-версии меню (команда /pixelui click). */
    public boolean clickByToken(Player p, String token, String buttonId) {
        OpenMenu m = menus.get(p.getUniqueId());
        if (m == null || !m.token().equals(token)) return false;
        click(p, m, buttonId);
        return true;
    }

    private void click(Player p, OpenMenu m, String buttonId) {
        PixelMenu menu = m.menu();
        PixelMenu.Button b = menu.button(buttonId);
        if (b == null || !b.enabled()) return;
        PixelMenuClickEvent ev = new PixelMenuClickEvent(p, menu, b, m.fallback());
        Bukkit.getPluginManager().callEvent(ev);
        if (b.close()) menus.remove(p.getUniqueId(), m);
        if (!ev.isCancelled()) {
            try {
                if (b.handler() != null) b.handler().accept(p);
                if (menu.onClick() != null) menu.onClick().accept(p, b);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка в обработчике кнопки " + menu.id() + "/" + b.id(), e);
            }
        }
        if (b.close()) fireClose(p, menu, PixelMenuCloseEvent.Reason.BUTTON);
    }

    private void fireClose(Player p, PixelMenu menu, PixelMenuCloseEvent.Reason reason) {
        Bukkit.getPluginManager().callEvent(new PixelMenuCloseEvent(p, menu, reason));
        if (menu.onClose() != null) {
            try {
                menu.onClose().accept(p);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка в onClose меню " + menu.id(), e);
            }
        }
    }

    // ---------------------------------------------------------------- пакеты от клиента

    /** Вызывается в главном потоке. */
    public void handle(Player p, JsonObject o) {
        try {
            String type = o.has("type") ? o.get("type").getAsString() : "";
            switch (type) {
                case "hello" -> {
                    String v = o.has("version") ? o.get("version").getAsString() : "?";
                    boolean first = versions.put(p.getUniqueId(), v) == null;
                    if (plugin.settings().logHello()) plugin.getLogger().info(p.getName() + " — клиент PixelUI " + v);
                    if (first) Bukkit.getPluginManager().callEvent(new PixelClientReadyEvent(p, v));
                }
                case "click" -> {
                    OpenMenu m = menus.get(p.getUniqueId());
                    if (m == null || m.fallback()) return;
                    if (!m.menu().id().equals(o.get("menu").getAsString())) return;
                    click(p, m, o.get("button").getAsString());
                }
                case "close" -> {
                    OpenMenu m = menus.get(p.getUniqueId());
                    if (m == null || m.fallback()) return;
                    if (!m.menu().id().equals(o.get("menu").getAsString())) return;
                    menus.remove(p.getUniqueId(), m);
                    fireClose(p, m.menu(), PixelMenuCloseEvent.Reason.PLAYER);
                }
                default -> { }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Плохой пакет от " + p.getName() + ": " + e.getMessage());
        }
    }

    public void quit(Player p) {
        OpenMenu m = menus.remove(p.getUniqueId());
        if (m != null) fireClose(p, m.menu(), PixelMenuCloseEvent.Reason.QUIT);
        versions.remove(p.getUniqueId());
        bars.remove(p.getUniqueId());
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeBar(p, null);
            if (menus.containsKey(p.getUniqueId())) closeMenu(p);
        }
        menus.clear();
        bars.clear();
        versions.clear();
    }
}
