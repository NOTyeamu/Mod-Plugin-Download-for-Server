package my.yeamu.modgate.gate;

import my.yeamu.modgate.ModGatePlugin;
import my.yeamu.modgate.config.GateConfig;
import my.yeamu.modgate.manifest.Manifest;
import my.yeamu.modgate.util.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ядро плагина: ожидание рукопожатия, проверка, карантин, кик.
 * Все методы вызываются в главном потоке сервера (события и задачи планировщика),
 * тяжёлых операций здесь нет — сеть и диск работают в фоне.
 */
public final class GateManager implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ModGatePlugin plugin;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();
    /** Игроки в карантине. Потокобезопасно: читается и из асинхронного чата. */
    private final Set<UUID> quarantined = ConcurrentHashMap.newKeySet();

    public GateManager(ModGatePlugin plugin) {
        this.plugin = plugin;
    }

    private GateConfig cfg() { return plugin.gateConfig(); }

    // ------------------------------------------------------------------ события

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerState state = new PlayerState(player.getUniqueId(), player.getName());
        states.put(player.getUniqueId(), state);

        // Новый игрок не должен видеть тех, кто сейчас в карантине
        for (UUID id : quarantined) {
            Player q = Bukkit.getPlayer(id);
            if (q != null && !q.equals(player)) player.hidePlayer(plugin, q);
        }

        GateConfig cfg = cfg();
        if (cfg.mode() == GateConfig.Mode.OFF) {
            state.status = PlayerState.Status.DISABLED;
            return;
        }
        PlayerState.Status exempt = exemptionOf(player, cfg);
        if (exempt != null) {
            state.status = exempt;
            plugin.joinLogger().log(player, "ПРОПУЩЕН", exempt.name(), null);
            return;
        }

        // Если клиент уже успел зарегистрировать канал — сразу просим рукопожатие
        Bukkit.getScheduler().runTaskLater(plugin, () -> requestHandshake(player), 2L);

        long timeoutTicks = Math.max(1, cfg.handshakeTimeout()) * 20L;
        state.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state.timeoutTask = null;
            if (player.isOnline() && state.status == PlayerState.Status.PENDING) {
                fail(player, state, PlayerState.Reason.NO_MOD, null);
            }
        }, timeoutTicks);
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!ModGatePlugin.CHANNEL.equals(event.getChannel())) return;
        PlayerState state = states.get(event.getPlayer().getUniqueId());
        if (state != null && state.status == PlayerState.Status.PENDING) {
            requestHandshake(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerState state = states.remove(id);
        if (state != null) state.cancelTasks();
        if (quarantined.remove(id)) showToAll(event.getPlayer());
    }

    // ------------------------------------------------------------------ рукопожатие

    /** Отправляет клиенту просьбу прислать рукопожатие (если он слушает наш канал). */
    private void requestHandshake(Player player) {
        if (!player.isOnline()) return;
        if (player.getListeningPluginChannels().contains(ModGatePlugin.CHANNEL)) {
            send(player, "request", "");
        }
    }

    private void send(Player player, String action, String text) {
        try {
            player.sendPluginMessage(plugin, ModGatePlugin.CHANNEL, HandshakeData.encodeServerMessage(action, text));
        } catch (Exception ignored) {
            // клиент мог отключиться — это не ошибка
        }
    }

    public void onBadHandshake(Player player, String details) {
        PlayerState state = states.get(player.getUniqueId());
        if (state == null || !isChecked(state)) return;
        plugin.getLogger().warning("Плохое рукопожатие от " + player.getName() + ": " + details);
        fail(player, state, PlayerState.Reason.BAD_HANDSHAKE, null);
    }

    public void onHandshake(Player player, HandshakeData data) {
        PlayerState state = states.get(player.getUniqueId());
        if (state == null) return;
        state.handshake = data;
        if (!isChecked(state)) return; // исключённым просто запоминаем, что у них стоит

        Manifest manifest = plugin.manifestService().current();
        PlayerState.Reason reason = evaluate(data, manifest);
        if (reason == null) {
            pass(player, state, manifest == null);
        } else {
            fail(player, state, reason, data);
        }
    }

    /** Возвращает причину отказа или null, если всё в порядке. */
    public PlayerState.Reason evaluate(HandshakeData data, Manifest manifest) {
        if (manifest == null) return null; // манифеста нет совсем — не можем проверить, пускаем
        GateConfig cfg = cfg();
        if (VersionUtil.compare(data.modVersion(), manifest.minModVersion()) < 0) {
            return PlayerState.Reason.OUTDATED_MOD;
        }
        boolean manifestBad = cfg.exactManifestVersion()
                ? data.manifestVersion() != manifest.version()
                : data.manifestVersion() < manifest.version();
        if (manifestBad) return PlayerState.Reason.OUTDATED_MANIFEST;
        if (cfg.checkModsHash() && !manifest.expectedHash().equalsIgnoreCase(data.modsHash())) {
            return PlayerState.Reason.FOREIGN_MODS;
        }
        return null;
    }

    private boolean isChecked(PlayerState s) {
        return s.status == PlayerState.Status.PENDING
                || s.status == PlayerState.Status.FAILED
                || s.status == PlayerState.Status.OK;
    }

    // ------------------------------------------------------------------ итог

    private void pass(Player player, PlayerState state, boolean unverified) {
        boolean wasFailed = state.status == PlayerState.Status.FAILED;
        state.status = PlayerState.Status.OK;
        state.reason = null;
        state.cancelTasks();
        release(player);
        HandshakeData d = state.handshake;
        plugin.joinLogger().log(player, "ДОПУЩЕН", unverified ? "манифест недоступен, проверка пропущена" : "ok", d);
        if (wasFailed || !cfg().messages().ok().isEmpty()) {
            player.sendMessage(prefixed(cfg().messages().ok()));
        }
        send(player, "result", "ok");
    }

    private void fail(Player player, PlayerState state, PlayerState.Reason reason, HandshakeData data) {
        GateConfig cfg = cfg();
        // Повторный отказ по той же причине (например, повторный пакет) — не дублируем таймеры
        if (state.status == PlayerState.Status.FAILED && state.reason == reason) return;
        state.status = PlayerState.Status.FAILED;
        state.reason = reason;
        if (state.timeoutTask != null) { state.timeoutTask.cancel(); state.timeoutTask = null; }

        String reasonText = reasonText(reason, data);
        plugin.joinLogger().log(player, "ОТКАЗ", reason.name() + " — " + MM.stripTags(reasonText), data);
        send(player, "result", reason.name().toLowerCase(Locale.ROOT));

        if (cfg.mode() == GateConfig.Mode.WARN) {
            player.sendMessage(prefixed(fill(cfg.messages().warn(), reasonText, cfg.kickDelay())));
            return;
        }

        // strict: карантин + напоминания + кик по таймеру
        if (state.kickTask == null) {
            quarantine(player);
            state.quarantineStartedAt = System.currentTimeMillis();
            int kickDelay = Math.max(1, cfg.kickDelay());
            state.kickTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                state.kickTask = null;
                if (player.isOnline() && state.status == PlayerState.Status.FAILED) {
                    kick(player, state.reason, state.handshake);
                }
            }, kickDelay * 20L);
            long every = Math.max(1, cfg.reminderInterval()) * 20L;
            state.reminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || state.status != PlayerState.Status.FAILED) return;
                long left = kickDelay - (System.currentTimeMillis() - state.quarantineStartedAt) / 1000L;
                if (left > 0) player.sendMessage(prefixed(fill(cfg().messages().quarantine(),
                        reasonText(state.reason, state.handshake), (int) left)));
            }, every, every);
        }
        player.sendMessage(prefixed(fill(cfg.messages().quarantine(), reasonText, cfg.kickDelay())));
    }

    public void kick(Player player, PlayerState.Reason reason, HandshakeData data) {
        String reasonText = reason == null ? "" : reasonText(reason, data);
        player.kick(MM.deserialize(fill(cfg().messages().kick(), reasonText, 0)));
    }

    /** Кикает всех, у кого устарел мод или сборка относительно текущего манифеста. */
    public int kickOutdated() {
        Manifest manifest = plugin.manifestService().current();
        if (manifest == null) return 0;
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerState s = states.get(p.getUniqueId());
            if (s == null || !isChecked(s)) continue;
            PlayerState.Reason r = s.handshake == null ? PlayerState.Reason.NO_MOD : evaluate(s.handshake, manifest);
            if (r != null) {
                if (!cfg().messages().kickOutdated().isEmpty()) p.sendMessage(prefixed(cfg().messages().kickOutdated()));
                plugin.joinLogger().log(p, "КИК", "kick-outdated: " + r.name(), s.handshake);
                kick(p, r, s.handshake);
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ карантин

    private void quarantine(Player player) {
        quarantined.add(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.hidePlayer(plugin, player);
        }
        player.setFallDistance(0f);
    }

    private void release(Player player) {
        if (quarantined.remove(player.getUniqueId())) showToAll(player);
    }

    private void showToAll(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) other.showPlayer(plugin, player);
        }
    }

    public boolean isQuarantined(UUID id) { return quarantined.contains(id); }

    // ------------------------------------------------------------------ исключения

    private PlayerState.Status exemptionOf(Player player, GateConfig cfg) {
        if (player.hasPermission("modgate.bypass")) return PlayerState.Status.BYPASS;
        Set<String> ex = cfg.exemptPlayers();
        if (ex.contains(player.getName().toLowerCase(Locale.ROOT))
                || ex.contains(player.getUniqueId().toString().toLowerCase(Locale.ROOT))) {
            return PlayerState.Status.EXEMPT;
        }
        if (cfg.bedrockEnabled() && isBedrock(player, cfg)) return PlayerState.Status.BEDROCK;
        return null;
    }

    /** Bedrock-игрок: ник с префиксом Floodgate или (если есть) проверка через Floodgate API. */
    private boolean isBedrock(Player player, GateConfig cfg) {
        String prefix = cfg.bedrockPrefix();
        if (prefix != null && !prefix.isEmpty() && player.getName().startsWith(prefix)) return true;
        if (cfg.useFloodgateApi() && Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            try {
                // Через рефлексию, чтобы не тянуть Floodgate в зависимости сборки
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object instance = api.getMethod("getInstance").invoke(null);
                Method m = api.getMethod("isFloodgatePlayer", UUID.class);
                return (boolean) m.invoke(instance, player.getUniqueId());
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ сообщения

    private String reasonText(PlayerState.Reason reason, HandshakeData data) {
        GateConfig.Messages m = cfg().messages();
        Manifest manifest = plugin.manifestService().current();
        String have, need, template;
        switch (reason) {
            case OUTDATED_MOD -> {
                template = m.outdatedMod();
                have = data != null ? data.modVersion() : "?";
                need = manifest != null ? manifest.modVersion() : "?";
            }
            case OUTDATED_MANIFEST -> {
                template = m.outdatedManifest();
                have = data != null ? String.valueOf(data.manifestVersion()) : "?";
                need = manifest != null ? String.valueOf(manifest.version()) : "?";
            }
            case FOREIGN_MODS -> { template = m.foreignMods(); have = ""; need = ""; }
            case BAD_HANDSHAKE -> { template = m.badHandshake(); have = ""; need = ""; }
            default -> { template = m.noMod(); have = ""; need = ""; }
        }
        // Данные клиента недоверенные — экранируем теги MiniMessage
        return template.replace("<have>", MM.escapeTags(have)).replace("<need>", MM.escapeTags(need));
    }

    private String fill(String template, String reasonText, int seconds) {
        return template
                .replace("<reason>", reasonText)
                .replace("<url>", cfg().siteUrl())
                .replace("<seconds>", String.valueOf(seconds));
    }

    private Component prefixed(String mini) {
        return MM.deserialize(cfg().messages().prefix() + mini);
    }

    // ------------------------------------------------------------------ доступ

    public PlayerState state(UUID id) { return states.get(id); }
    public Collection<PlayerState> states() { return states.values(); }

    public void shutdown() {
        for (PlayerState s : states.values()) s.cancelTasks();
        for (UUID id : quarantined) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) showToAll(p);
        }
        quarantined.clear();
    }
}
