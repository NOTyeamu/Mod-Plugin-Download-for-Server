package my.yeamu.nightvote;

import my.yeamu.pixelui.paper.api.PixelAnswer;
import my.yeamu.pixelui.paper.api.PixelIcon;
import my.yeamu.pixelui.paper.api.PixelNotification;
import my.yeamu.pixelui.paper.api.PixelPosition;
import my.yeamu.pixelui.paper.api.PixelPrompt;
import my.yeamu.pixelui.paper.api.PixelTheme;
import my.yeamu.pixelui.paper.api.PixelUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Голосования по мирам. Всё работает в главном потоке (таймеры Bukkit), тяжёлой работы нет.
 */
final class VoteManager implements Listener {

    static final String PROMPT_ID = "nightvote";

    /** Одно голосование в одном мире. */
    private static final class Vote {
        final World world;
        final String starter;
        final Set<UUID> asked = new LinkedHashSet<>();
        final Map<UUID, Boolean> answers = new LinkedHashMap<>();
        final long endsAt;
        BukkitTask timer;
        boolean ended;

        Vote(World world, String starter, long endsAt) {
            this.world = world;
            this.starter = starter;
            this.endsAt = endsAt;
        }

        int yes() { return (int) answers.values().stream().filter(b -> b).count(); }
        int no() { return answers.size() - yes(); }
        int waiting() { return asked.size() - answers.size(); }
    }

    private final NightVotePlugin plugin;
    private final Map<UUID, Vote> active = new HashMap<>();
    /** Была ли ночь на прошлой проверке (для автозапуска с наступлением ночи). */
    private final Map<UUID, Boolean> wasNight = new HashMap<>();
    /** До какого момента (мс) в мире нельзя начать новое голосование. */
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private BukkitTask watcher;
    private Method authMethod;

    VoteManager(NightVotePlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    // ------------------------------------------------------------------ наступление ночи

    /** Раз в секунду смотрим, где наступила ночь. */
    void startWatcher() {
        watcher = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World w : Bukkit.getWorlds()) {
                if (!enabledWorld(w)) continue;
                boolean night = isNight(w);
                Boolean prev = wasNight.put(w.getUID(), night);
                if (prev == null) continue;
                if (!prev && night) {
                    // Новая ночь — сбрасываем паузу прошлой
                    cooldownUntil.remove(w.getUID());
                    if (cfg().getBoolean("trigger.nightfall", true)) {
                        long delay = Math.max(0, cfg().getLong("start-delay-seconds", 6)) * 20L;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> start(w, null, false), delay);
                    }
                }
            }
        }, 40L, 20L);
    }

    void shutdown() {
        if (watcher != null) watcher.cancel();
        cancelAll();
    }

    boolean enabledWorld(World w) {
        List<String> list = cfg().getStringList("worlds");
        if (list.isEmpty()) return w.getEnvironment() == World.Environment.NORMAL;
        return list.contains(w.getName());
    }

    boolean isNight(World w) {
        long t = w.getTime();
        return t >= cfg().getLong("night-start", 12542) && t < cfg().getLong("night-end", 23460);
    }

    // ------------------------------------------------------------------ запуск

    /**
     * Начать голосование в мире. starter — кто начал (null — автоматически).
     * loud — сообщать игроку, почему не получилось (для команды).
     */
    boolean start(World w, Player starter, boolean loud) {
        if (!enabledWorld(w)) {
            if (loud && starter != null) plugin.send(starter, "world-disabled", "&cВ этом мире голосования нет.");
            return false;
        }
        if (!isNight(w)) {
            if (loud && starter != null) plugin.send(starter, "not-night", "&cСейчас не ночь.");
            return false;
        }
        if (active.containsKey(w.getUID())) {
            if (loud && starter != null) plugin.send(starter, "already", "&eГолосование уже идёт.");
            return false;
        }
        long now = System.currentTimeMillis();
        Long cd = cooldownUntil.get(w.getUID());
        if (cd != null && cd > now) {
            if (loud && starter != null) plugin.send(starter, "cooldown",
                    "&cПовторное голосование через {s} сек.", "s", (cd - now + 999) / 1000);
            return false;
        }
        List<Player> voters = new ArrayList<>();
        for (Player p : w.getPlayers()) if (canVote(p)) voters.add(p);
        if (voters.isEmpty()) return false;

        int seconds = Math.max(5, cfg().getInt("vote.duration-seconds", 30));
        Vote v = new Vote(w, starter == null ? null : starter.getName(), now + seconds * 1000L);
        active.put(w.getUID(), v);
        for (Player p : voters) {
            v.asked.add(p.getUniqueId());
            ask(v, p, seconds);
        }
        // Таймер: счётчик в строке над хотбаром, конец по времени, отмена если наступило утро
        v.timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (v.ended) return;
            if (!isNight(w)) {
                finish(v, Result.MORNING);
                return;
            }
            if (System.currentTimeMillis() >= v.endsAt) {
                finish(v, null);
                return;
            }
            showTally(v);
        }, 20L, 20L);
        plugin.getLogger().info("Голосование в мире " + w.getName() + " (" + voters.size() + " игроков)"
                + (starter != null ? ", начал " + starter.getName() : ""));
        return true;
    }

    private void ask(Vote v, Player p, int seconds) {
        String text = v.starter == null
                ? plugin.msg("prompt-text", "&7Наступила ночь. Голосуют: {count}")
                : plugin.msg("prompt-text-player", "&7Предложил &f{player}&7. Голосуют: {count}");
        PixelPrompt.builder(PROMPT_ID)
                .question(plugin.msg("prompt-question", "&eПропустить ночь?"))
                .text(NightVotePlugin.fill(text, "player", v.starter == null ? "" : v.starter, "count", v.asked.size()))
                .icon(PixelIcon.MOON)
                .theme(theme(cfg().getString("vote.theme", "ender")))
                .position(position(cfg().getString("vote.position", "TOP_CENTER")))
                .timeout(Duration.ofSeconds(seconds))
                .yesLabel(plugin.msg("yes-label", "Пропустить"))
                .noLabel(plugin.msg("no-label", "Оставить"))
                .keys('Y', 'N')
                .timerBar(true)
                .width(260)
                .sound(cfg().getString("vote.sound", "minecraft:block.note_block.chime"))
                .onAnswer((pl, answer) -> onAnswer(v, pl, answer))
                .send(p);
    }

    // ------------------------------------------------------------------ ответы

    private void onAnswer(Vote v, Player p, PixelAnswer answer) {
        if (v.ended || !v.asked.contains(p.getUniqueId()) || v.answers.containsKey(p.getUniqueId())) return;
        if (answer != PixelAnswer.YES && answer != PixelAnswer.NO) return; // тайм-аут/отмена — не голос
        boolean yes = answer == PixelAnswer.YES;
        v.answers.put(p.getUniqueId(), yes);
        if (cfg().getBoolean("vote.announce-votes", true)) {
            for (Player o : online(v)) {
                plugin.send(o, yes ? "voted-yes" : "voted-no",
                        yes ? "&f{player} &aза пропуск ночи" : "&f{player} &cпротив пропуска",
                        "player", p.getName());
            }
        }
        check(v);
    }

    /** Завершить досрочно, если все ответили или исход уже не изменится. */
    private void check(Vote v) {
        if (v.ended) return;
        int yes = v.yes(), rest = v.waiting(), total = v.asked.size();
        int req = cfg().getInt("vote.required-percent", 50);
        int minYes = Math.max(1, cfg().getInt("vote.min-yes", 1));
        if (total == 0) {
            finish(v, Result.NO_PLAYERS);
            return;
        }
        if (rest == 0) {
            finish(v, null);
            return;
        }
        // Даже если все оставшиеся проголосуют «Нет» — ночь всё равно пропустят
        if (yes >= minYes && yes * 100L > (long) req * total) {
            finish(v, Result.SKIPPED);
            return;
        }
        // Даже если все оставшиеся проголосуют «Да» — не хватит
        if (yes + rest < minYes || (yes + rest) * 100L <= (long) req * total) {
            finish(v, Result.KEPT);
        }
    }

    private enum Result { SKIPPED, KEPT, MORNING, NO_PLAYERS, CANCELLED }

    /** result == null — посчитать по текущим голосам. */
    private void finish(Vote v, Result result) {
        if (v.ended) return;
        v.ended = true;
        if (v.timer != null) v.timer.cancel();
        active.remove(v.world.getUID());
        List<Player> players = online(v);
        // Убираем вопрос у тех, кто ещё не ответил
        for (Player p : players) {
            if (!v.answers.containsKey(p.getUniqueId())) PixelUI.cancelPrompt(p, PROMPT_ID);
        }
        int yes = v.yes(), no = v.no();
        if (result == null) {
            int req = cfg().getInt("vote.required-percent", 50);
            int minYes = Math.max(1, cfg().getInt("vote.min-yes", 1));
            int counted = cfg().getBoolean("vote.silent-as-no", false) ? v.asked.size() : yes + no;
            result = yes >= minYes && counted > 0 && yes * 100L > (long) req * counted ? Result.SKIPPED : Result.KEPT;
        }
        cooldownUntil.put(v.world.getUID(),
                System.currentTimeMillis() + Math.max(0, cfg().getLong("vote.cooldown-seconds", 90)) * 1000L);

        switch (result) {
            case SKIPPED -> {
                skipNight(v.world);
                notifyAll(v.world.getPlayers(), plugin.msg("skipped-title", "&aНочь пропущена"),
                        NightVotePlugin.fill(plugin.msg("result-text", "&aДа: {yes} &8· &cНет: {no}"), "yes", yes, "no", no),
                        PixelIcon.SUN, PixelTheme.SUCCESS);
            }
            case KEPT -> notifyAll(players, plugin.msg("kept-title", "&eНочь остаётся"),
                    NightVotePlugin.fill(plugin.msg("result-text", "&aДа: {yes} &8· &cНет: {no}"), "yes", yes, "no", no),
                    PixelIcon.MOON, PixelTheme.DARK);
            case MORNING -> { /* утро наступило само — молча закрываем */ }
            case NO_PLAYERS, CANCELLED -> { }
        }
        plugin.getLogger().info("Итог в мире " + v.world.getName() + ": " + result + " (да " + yes + ", нет " + no
                + ", не ответили " + v.waiting() + ")");
    }

    /** Перемотать на утро следующего дня (счётчик дней и фаза луны идут как положено). */
    private void skipNight(World w) {
        long morning = Math.floorMod(cfg().getLong("on-skip.time", 0), 24000L);
        long full = w.getFullTime();
        long dayStart = full - Math.floorMod(full, 24000L);
        w.setFullTime(dayStart + 24000L + morning);
        if (cfg().getBoolean("on-skip.clear-weather", true)) {
            w.setStorm(false);
            w.setThundering(false);
        }
        wasNight.put(w.getUID(), false);
    }

    private void notifyAll(List<Player> players, String title, String text, String icon, PixelTheme theme) {
        for (Player p : players) {
            PixelUI.notify(p, PixelNotification.builder()
                    .id(PROMPT_ID + "-result")
                    .title(title).text(text).icon(icon).theme(theme)
                    .position(PixelPosition.TOP_RIGHT)
                    .duration(4500)
                    .build());
        }
    }

    /** Счётчик голосов над хотбаром. */
    private void showTally(Vote v) {
        if (!cfg().getBoolean("vote.actionbar", true)) return;
        long left = Math.max(0, (v.endsAt - System.currentTimeMillis() + 999) / 1000);
        String s = NightVotePlugin.fill(plugin.msg("actionbar",
                        "&9☾ &fПропуск ночи: &aДа {yes} &8· &cНет {no} &8· &7ждём {wait} &8· &e{left} с"),
                "yes", v.yes(), "no", v.no(), "wait", v.waiting(), "left", left);
        for (Player p : online(v)) p.sendActionBar(NightVotePlugin.color(s));
    }

    private List<Player> online(Vote v) {
        List<Player> out = new ArrayList<>();
        for (UUID id : v.asked) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------------ кто голосует

    private boolean canVote(Player p) {
        if (p.hasPermission("nightvote.exempt") && !p.isOp()) return false;
        if (cfg().getBoolean("vote.ignore-spectators", true) && p.getGameMode() == GameMode.SPECTATOR) return false;
        return !cfg().getBoolean("vote.require-login", true) || loggedIn(p);
    }

    /** Вошёл ли игрок в аккаунт PixelAuth (если плагина нет — считаем, что да). */
    private boolean loggedIn(Player p) {
        Plugin auth = Bukkit.getPluginManager().getPlugin("PixelAuth");
        if (auth == null || !auth.isEnabled()) return true;
        try {
            if (authMethod == null) {
                Class<?> api = Class.forName("my.yeamu.pixelauth.api.PixelAuthAPI", true, auth.getClass().getClassLoader());
                authMethod = api.getMethod("isAuthenticated", Player.class);
            }
            return (boolean) authMethod.invoke(null, p);
        } catch (Throwable t) {
            return true;
        }
    }

    // ------------------------------------------------------------------ события

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        if (!cfg().getBoolean("trigger.bed", true)) return;
        Player p = e.getPlayer();
        if (!canVote(p)) return;
        start(p.getWorld(), p, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        leave(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        leave(e.getPlayer().getUniqueId());
    }

    /** Игрок ушёл из мира — его голос (или ожидание голоса) больше не учитывается. */
    private void leave(UUID id) {
        for (Vote v : new ArrayList<>(active.values())) {
            if (v.asked.remove(id)) {
                v.answers.remove(id);
                check(v);
            }
        }
    }

    // ------------------------------------------------------------------ команды

    int cancelAll() {
        List<Vote> all = new ArrayList<>(active.values());
        for (Vote v : all) finish(v, Result.CANCELLED);
        return all.size();
    }

    List<String> status() {
        List<String> out = new ArrayList<>();
        for (Vote v : active.values()) {
            long left = Math.max(0, (v.endsAt - System.currentTimeMillis()) / 1000);
            out.add("&9" + v.world.getName() + "&7: &aда " + v.yes() + " &8· &cнет " + v.no()
                    + " &8· &7ждём " + v.waiting() + " &8· &e" + left + " с");
        }
        return out;
    }
}
