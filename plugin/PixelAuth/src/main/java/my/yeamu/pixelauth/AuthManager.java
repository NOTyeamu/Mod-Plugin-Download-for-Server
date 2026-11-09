package my.yeamu.pixelauth;

import my.yeamu.pixelauth.api.PixelAuthLoginEvent;
import my.yeamu.pixelauth.api.PixelAuthLogoutEvent;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Вся логика входа. Правила потоков:
 * <ul>
 *   <li>база — только в потоке Storage (или в асинхронном pre-login);</li>
 *   <li>хеширование — в отдельном пуле hasher;</li>
 *   <li>изменение состояния игрока и любые вызовы Bukkit — только в главном потоке.</li>
 * </ul>
 */
public final class AuthManager {

    public static final String AUTH_ID = "pixelauth";
    public static final String CHANGE_ID = "pixelauth-change";

    /** Состояние игрока на сервере. */
    static final class State {
        final UUID uuid;
        final String name;
        final String ip;
        volatile Account account;   // null — не зарегистрирован
        volatile boolean loaded;    // аккаунт загружен из базы
        volatile boolean authed;
        boolean busy;               // идёт проверка пароля — второй запрос игнорируем
        int attempts;
        long deadline;
        int seconds;
        BukkitTask timeoutTask, tickTask;
        BossBar bar;
        Limbo limbo;

        State(UUID uuid, String name, String ip) {
            this.uuid = uuid;
            this.name = name;
            this.ip = ip;
        }
    }

    private final PixelAuthPlugin plugin;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();
    /** Аккаунты, загруженные в pre-login (ключ — ник в нижнем регистре). */
    private final Map<String, Optional<Account>> preloaded = new ConcurrentHashMap<>();
    private final Map<String, Integer> ipKicks = new ConcurrentHashMap<>();
    private final Map<String, Long> ipBlocked = new ConcurrentHashMap<>();
    private final ExecutorService hasher = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "PixelAuth-Hash");
        t.setDaemon(true);
        return t;
    });

    AuthManager(PixelAuthPlugin plugin) {
        this.plugin = plugin;
    }

    private Settings s() { return plugin.settings(); }
    private Msg msg() { return plugin.msg(); }
    private Screens screens() { return plugin.screens(); }

    // ================================================================ вход на сервер

    /** Асинхронный pre-login: проверки ника/IP и загрузка аккаунта (главный поток не ждёт базу). */
    void preLogin(AsyncPlayerPreLoginEvent e) {
        String name = e.getName();
        String ip = e.getAddress().getHostAddress();
        boolean bedrock = isBedrockName(name);

        if (!bedrock && !s().nameRegex.matcher(name).matches()) {
            deny(e, msg().comp("kick-name-invalid"));
            return;
        }
        Long until = ipBlocked.get(ip);
        if (until != null) {
            long left = until - System.currentTimeMillis();
            if (left > 0) {
                deny(e, msg().comp("kick-ip-blocked", "min", Math.max(1, (left + 59_999) / 60_000)));
                return;
            }
            ipBlocked.remove(ip);
        }
        // Не даём «выбить» вошедшего игрока, зайдя под его ником (offline-mode)
        for (State st : states.values()) {
            if (st.authed && st.name.equalsIgnoreCase(name)) {
                deny(e, msg().comp("kick-already-online"));
                return;
            }
        }
        try {
            Account a = plugin.storage().find(name);
            if (a != null && s().nameCaseSensitive && !bedrock && !a.realName().equals(name)) {
                deny(e, msg().comp("kick-name-case", "real", a.realName()));
                return;
            }
            preloaded.put(name.toLowerCase(Locale.ROOT), Optional.ofNullable(a));
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Ошибка базы при входе " + name, ex);
            deny(e, msg().comp("storage-error"));
        }
    }

    private static void deny(AsyncPlayerPreLoginEvent e, net.kyori.adventure.text.Component reason) {
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, reason);
    }

    void join(Player p) {
        if (p.hasMetadata("NPC")) return; // NPC из Citizens и подобных
        State st = new State(p.getUniqueId(), p.getName(), ip(p));
        states.put(st.uuid, st);

        // Новичок не видит тех, кто ещё не вошёл
        if (s().hidePlayer) {
            for (State o : states.values()) {
                if (o == st || o.authed) continue;
                Player op = Bukkit.getPlayer(o.uuid);
                if (op != null) p.hidePlayer(plugin, op);
            }
        }

        if (s().bedrockAutoLogin && isBedrock(p)) {
            st.loaded = true;
            complete(p, st, PixelAuthLoginEvent.Type.BEDROCK);
            return;
        }

        Optional<Account> pre = preloaded.remove(p.getName().toLowerCase(Locale.ROOT));
        if (pre != null) {
            st.account = pre.orElse(null);
            st.loaded = true;
            afterLoad(p, st);
            return;
        }
        // Аккаунт не загружен (например, после /reload) — сначала карантин, потом база
        startLimbo(p, st);
        db(() -> {
            Account a;
            try {
                a = plugin.storage().find(st.name);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Ошибка базы для " + st.name, ex);
                sync(() -> { if (p.isOnline()) p.kick(msg().comp("storage-error")); });
                return;
            }
            sync(() -> {
                if (!p.isOnline() || states.get(st.uuid) != st || st.authed) return;
                st.account = a;
                st.loaded = true;
                afterLoad(p, st);
            });
        });
    }

    private void afterLoad(Player p, State st) {
        if (sessionValid(st)) {
            complete(p, st, PixelAuthLoginEvent.Type.SESSION);
            return;
        }
        startLimbo(p, st);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !st.authed && states.get(st.uuid) == st) screens().show(p, st, null);
        }, Math.max(1, s().uiDelayTicks));
    }

    private boolean sessionValid(State st) {
        Account a = st.account;
        if (!s().sessionEnabled || a == null || a.lastLogin() <= 0) return false;
        if (!st.ip.equals(a.lastIp())) return false;
        if (a.uuid() != null && !a.uuid().equals(st.uuid.toString())) return false;
        return System.currentTimeMillis() - a.lastLogin() < s().sessionMinutes * 60_000L;
    }

    private void startLimbo(Player p, State st) {
        if (st.limbo != null) return;
        st.limbo = Limbo.apply(plugin, p, s());
        st.attempts = 0;
        st.seconds = 0;
        st.deadline = System.currentTimeMillis() + s().timeoutSeconds * 1000L;
        st.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !st.authed) {
                plugin.loginLog().write(st.name, st.ip, "KICK", "timeout");
                p.kick(msg().comp("kick-timeout"));
            }
        }, s().timeoutSeconds * 20L);
        st.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(p, st), 20L, 20L);
    }

    /** Раз в секунду: боссбар с таймером и напоминание в чате — только для игроков без PixelUI. */
    private void tick(Player p, State st) {
        if (!p.isOnline() || st.authed) return;
        st.seconds++;
        int left = secondsLeft(st);
        if (screens().pixel(p)) {
            if (st.bar != null) {
                p.hideBossBar(st.bar);
                st.bar = null;
            }
            return;
        }
        float progress = Math.max(0f, Math.min(1f, left / (float) s().timeoutSeconds));
        if (st.bar == null) {
            st.bar = BossBar.bossBar(msg().comp("bossbar", "time", left), progress, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            p.showBossBar(st.bar);
        } else {
            st.bar.name(msg().comp("bossbar", "time", left));
            st.bar.progress(progress);
            st.bar.color(left <= 10 ? BossBar.Color.RED : BossBar.Color.YELLOW);
        }
        if (st.loaded && st.seconds % s().reminderSeconds == 0) screens().chatPrompt(p, st.account != null, false);
    }

    int secondsLeft(State st) {
        return (int) Math.max(0, (st.deadline - System.currentTimeMillis() + 999) / 1000);
    }

    private void stopTasks(Player p, State st) {
        if (st.timeoutTask != null) st.timeoutTask.cancel();
        if (st.tickTask != null) st.tickTask.cancel();
        st.timeoutTask = null;
        st.tickTask = null;
        if (st.bar != null) {
            p.hideBossBar(st.bar);
            st.bar = null;
        }
    }

    // ================================================================ действия игрока

    /** /login или кнопка «Войти» в пиксельном окне. */
    public void attemptLogin(Player p, String password) {
        State st = states.get(p.getUniqueId());
        if (st == null || st.authed) {
            msg().send(p, "already-logged");
            return;
        }
        if (!st.loaded || st.busy) return;
        Account a = st.account;
        if (a == null) {
            screens().error(p, AUTH_ID, msg().raw("not-registered"));
            return;
        }
        st.busy = true;
        int iter = s().hashIterations;
        async(hasher, () -> {
            Passwords.Result r = Passwords.verify(password, a.hash());
            String newHash = r == Passwords.Result.OK && Passwords.needsRehash(a.hash(), iter)
                    ? Passwords.hash(password, iter) : null;
            sync(() -> {
                st.busy = false;
                if (!p.isOnline() || states.get(st.uuid) != st || st.authed) return;
                switch (r) {
                    case OK -> {
                        if (newHash != null) {
                            // Старый хеш AuthMe — тихо переводим в PBKDF2
                            st.account = a.withHash(newHash);
                            db(() -> {
                                try {
                                    plugin.storage().updateHash(a.name(), newHash);
                                } catch (SQLException ex) {
                                    plugin.getLogger().log(Level.WARNING, "Не удалось обновить хеш " + a.name(), ex);
                                }
                            });
                        }
                        complete(p, st, PixelAuthLoginEvent.Type.PASSWORD);
                    }
                    case WRONG -> wrongPassword(p, st);
                    case UNSUPPORTED -> {
                        plugin.loginLog().write(st.name, st.ip, "FAIL", "unsupported hash " + Passwords.format(a.hash()));
                        screens().error(p, AUTH_ID, msg().raw("unsupported-hash"));
                    }
                }
            });
        });
    }

    private void wrongPassword(Player p, State st) {
        st.attempts++;
        int left = s().maxAttempts - st.attempts;
        plugin.loginLog().write(st.name, st.ip, "FAIL", "wrong password (" + st.attempts + "/" + s().maxAttempts + ")");
        if (left <= 0) {
            int kicks = ipKicks.merge(st.ip, 1, Integer::sum);
            if (s().ipBlockKicks > 0 && kicks >= s().ipBlockKicks) {
                ipBlocked.put(st.ip, System.currentTimeMillis() + s().ipBlockMinutes * 60_000L);
                ipKicks.remove(st.ip);
                plugin.loginLog().write(st.name, st.ip, "IP-BLOCK", s().ipBlockMinutes + " min");
            }
            plugin.loginLog().write(st.name, st.ip, "KICK", "too many attempts");
            p.kick(msg().comp("kick-attempts"));
            return;
        }
        screens().error(p, AUTH_ID, msg().raw("wrong-password", "attempts", left));
    }

    /** /register или кнопка «Создать аккаунт». */
    public void attemptRegister(Player p, String password, String repeat) {
        State st = states.get(p.getUniqueId());
        if (st == null || st.authed) {
            msg().send(p, "already-logged");
            return;
        }
        if (!st.loaded || st.busy) return;
        if (st.account != null) {
            screens().error(p, AUTH_ID, msg().raw("already-registered"));
            return;
        }
        String err = validate(p.getName(), password, repeat);
        if (err != null) {
            screens().error(p, AUTH_ID, err);
            return;
        }
        st.busy = true;
        boolean ipBypass = p.hasPermission("pixelauth.unlimited");
        int maxIp = s().maxAccountsPerIp;
        int iter = s().hashIterations;
        async(hasher, () -> {
            String hash = Passwords.hash(password, iter);
            db(() -> {
                String fail = null;
                Account acc = null;
                try {
                    if (!ipBypass && maxIp > 0 && plugin.storage().countByIp(st.ip) >= maxIp) {
                        fail = msg().raw("ip-limit", "max", maxIp);
                    } else {
                        long now = System.currentTimeMillis();
                        acc = new Account(st.name.toLowerCase(Locale.ROOT), st.name, st.uuid.toString(), hash, st.ip, now, st.ip, now);
                        if (!plugin.storage().insert(acc)) fail = msg().raw("already-registered");
                    }
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка регистрации " + st.name, ex);
                    fail = msg().raw("storage-error");
                }
                String f = fail;
                Account created = acc;
                sync(() -> {
                    st.busy = false;
                    if (!p.isOnline() || states.get(st.uuid) != st || st.authed) return;
                    if (f != null) {
                        screens().error(p, AUTH_ID, f);
                        return;
                    }
                    st.account = created;
                    complete(p, st, PixelAuthLoginEvent.Type.REGISTER);
                });
            });
        });
    }

    /** /changepassword или окно смены пароля. repeat = null — без повтора (команда). */
    public void changePassword(Player p, String oldPw, String newPw, String repeat, boolean fromUi) {
        State st = states.get(p.getUniqueId());
        if (st == null || !st.authed) {
            msg().send(p, "need-auth");
            return;
        }
        Account a = st.account;
        if (a == null) {
            changeError(p, fromUi, msg().raw("not-registered"));
            return;
        }
        if (st.busy) return;
        String err = validate(p.getName(), newPw, repeat);
        if (err == null && oldPw.equals(newPw)) err = msg().raw("password-same");
        if (err != null) {
            changeError(p, fromUi, err);
            return;
        }
        st.busy = true;
        int iter = s().hashIterations;
        async(hasher, () -> {
            Passwords.Result r = Passwords.verify(oldPw, a.hash());
            if (r != Passwords.Result.OK) {
                sync(() -> {
                    st.busy = false;
                    if (p.isOnline()) changeError(p, fromUi, msg().raw(r == Passwords.Result.WRONG ? "wrong-old-password" : "unsupported-hash"));
                });
                return;
            }
            String hash = Passwords.hash(newPw, iter);
            db(() -> {
                boolean ok = true;
                try {
                    plugin.storage().updateHash(a.name(), hash);
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка смены пароля " + a.name(), ex);
                    ok = false;
                }
                boolean done = ok;
                sync(() -> {
                    st.busy = false;
                    if (!p.isOnline()) return;
                    if (!done) {
                        changeError(p, fromUi, msg().raw("storage-error"));
                        return;
                    }
                    st.account = st.account == null ? null : st.account.withHash(hash);
                    screens().closeChange(p);
                    screens().success(p, "changepass-success", "");
                    plugin.loginLog().write(st.name, st.ip, "CHANGEPASS", "");
                });
            });
        });
    }

    private void changeError(Player p, boolean fromUi, String raw) {
        if (fromUi) screens().error(p, CHANGE_ID, raw);
        else p.sendMessage(Msg.color(msg().raw("prefix") + raw));
    }

    /** Открыть окно смены пароля (если есть PixelUI). */
    public boolean openChangeScreen(Player p) {
        State st = states.get(p.getUniqueId());
        if (st == null || !st.authed || st.account == null) return false;
        return screens().openChange(p);
    }

    public void logout(Player p) {
        State st = states.get(p.getUniqueId());
        if (st == null || !st.authed) {
            msg().send(p, "need-auth");
            return;
        }
        if (st.account == null) return; // Bedrock-игрок без аккаунта — выходить некуда
        st.authed = false;
        Account a = st.account;
        st.account = a.withLogin(a.lastIp(), 0, null); // убиваем сессию
        db(() -> {
            try {
                plugin.storage().updateLogin(a.name(), a.lastIp(), 0, null);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Ошибка базы при выходе " + a.name(), ex);
            }
        });
        plugin.loginLog().write(st.name, st.ip, "LOGOUT", "");
        msg().send(p, "logout-success");
        Bukkit.getPluginManager().callEvent(new PixelAuthLogoutEvent(p));
        if (s().hidePlayer) {
            // вышедший снова не видит других неавторизованных
            for (State o : states.values()) {
                Player op = o == st || o.authed ? null : Bukkit.getPlayer(o.uuid);
                if (op != null) p.hidePlayer(plugin, op);
            }
        }
        afterLoad(p, st);
    }

    /** Кнопка «Выйти» в окне входа. */
    void quitFromScreen(Player p) {
        State st = states.get(p.getUniqueId());
        if (st != null && !st.authed) p.kick(msg().comp("kick-quit"));
    }

    /** Окно входа закрылось без кнопки (другой мод открыл свой экран и т.п.) — откроем снова. */
    void screenClosed(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            State st = states.get(p.getUniqueId());
            if (p.isOnline() && st != null && !st.authed && st.loaded) screens().show(p, st, null);
        }, 20L);
    }

    /** Мод PixelUI поздоровался позже, чем мы показали чат — переключаемся на красивое окно. */
    void clientReady(Player p) {
        State st = states.get(p.getUniqueId());
        if (st != null && !st.authed && st.loaded) screens().show(p, st, null);
    }

    // ================================================================ администратор / API

    public boolean forceLogin(Player p) {
        State st = states.get(p.getUniqueId());
        if (st == null || st.authed || !st.loaded) return false;
        complete(p, st, PixelAuthLoginEvent.Type.FORCED);
        return true;
    }

    /** Аккаунт удалён админом: если игрок онлайн — кикаем, при следующем входе он зарегистрируется заново. */
    void accountDeleted(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null && states.containsKey(p.getUniqueId())) p.kick(msg().comp("kick-unregistered"));
    }

    void accountChanged(String name, Account a) {
        for (State st : states.values()) {
            if (st.name.equalsIgnoreCase(name)) {
                if (st.account != null) st.account = st.account.withHash(a.hash());
                else if (!st.authed) st.account = a;
            }
        }
    }

    public boolean isAuthed(Player p) {
        if (plugin.passive()) return true;
        State st = states.get(p.getUniqueId());
        return st == null || st.authed;
    }

    public boolean isRegisteredCached(Player p) {
        State st = states.get(p.getUniqueId());
        return st != null && st.account != null;
    }

    // ================================================================ завершение входа / выход

    private void complete(Player p, State st, PixelAuthLoginEvent.Type type) {
        st.authed = true;
        st.attempts = 0;
        stopTasks(p, st);
        if (st.limbo != null) {
            st.limbo.restore(plugin, p);
            st.limbo = null;
        }
        if (s().hidePlayer) {
            // теперь игрок видит всех, кто уже вошёл
            for (State o : states.values()) {
                Player op = o == st || !o.authed ? null : Bukkit.getPlayer(o.uuid);
                if (op != null) p.showPlayer(plugin, op);
            }
        }
        screens().closeAuth(p);
        ipKicks.remove(st.ip);
        switch (type) {
            case PASSWORD, FORCED -> screens().success(p, "login-success-title", "login-success");
            case REGISTER -> screens().success(p, "register-success-title", "register-success");
            case SESSION -> msg().send(p, "session-login", "name", p.getName());
            case BEDROCK -> msg().send(p, "bedrock-login", "name", p.getName());
        }
        Account a = st.account;
        if (a != null) {
            long now = System.currentTimeMillis();
            st.account = a.withLogin(st.ip, now, st.uuid.toString());
            db(() -> {
                try {
                    plugin.storage().updateLogin(a.name(), st.ip, now, st.uuid.toString());
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка базы после входа " + a.name(), ex);
                }
            });
        }
        plugin.loginLog().write(st.name, st.ip, type.name(), "");
        Bukkit.getPluginManager().callEvent(new PixelAuthLoginEvent(p, type));
    }

    void quit(Player p) {
        State st = states.remove(p.getUniqueId());
        if (st == null) return;
        stopTasks(p, st);
        if (st.limbo != null) {
            st.limbo.restore(plugin, p);
            st.limbo = null;
        }
        Account a = st.account;
        if (st.authed && a != null && s().sessionEnabled) {
            // сессия считается от момента выхода
            long now = System.currentTimeMillis();
            db(() -> {
                try {
                    plugin.storage().updateLogin(a.name(), st.ip, now, st.uuid.toString());
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка базы при выходе " + a.name(), ex);
                }
            });
        }
    }

    /** Выключение плагина: вернуть всем игрокам нормальное состояние. */
    void shutdown() {
        for (State st : states.values()) {
            Player p = Bukkit.getPlayer(st.uuid);
            if (p == null) continue;
            stopTasks(p, st);
            if (st.limbo != null) {
                st.limbo.restore(plugin, p);
                st.limbo = null;
            }
            if (!st.authed) screens().closeAuth(p);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Player o : Bukkit.getOnlinePlayers()) if (o != p) p.showPlayer(plugin, o);
        }
        states.clear();
        hasher.shutdownNow();
    }

    // ================================================================ вспомогательное

    /** Проверка нового пароля. @return текст ошибки или null */
    String validate(String name, String pw, String repeat) {
        if (pw == null) pw = "";
        if (repeat != null && !pw.equals(repeat)) return msg().raw("passwords-mismatch");
        if (pw.chars().anyMatch(Character::isWhitespace)) return msg().raw("password-spaces");
        if (pw.length() < s().passwordMin) return msg().raw("password-short", "min", s().passwordMin);
        if (pw.length() > s().passwordMax) return msg().raw("password-long", "max", s().passwordMax);
        if (pw.equalsIgnoreCase(name)) return msg().raw("password-name");
        if (s().unsafePasswords.contains(pw.toLowerCase(Locale.ROOT))) return msg().raw("password-unsafe");
        return null;
    }

    private boolean isBedrockName(String name) {
        String pre = s().bedrockPrefix;
        return pre != null && !pre.isEmpty() && name.startsWith(pre);
    }

    /** Bedrock-игрок: префикс ника или Floodgate API (через рефлексию, без зависимости). */
    private boolean isBedrock(Player p) {
        if (isBedrockName(p.getName())) return true;
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) return false;
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(inst, p.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    private static String ip(Player p) {
        InetSocketAddress a = p.getAddress();
        return a == null || a.getAddress() == null ? "unknown" : a.getAddress().getHostAddress();
    }

    private void sync(Runnable r) {
        if (!plugin.isEnabled()) return;
        try {
            Bukkit.getScheduler().runTask(plugin, r);
        } catch (RuntimeException ignored) {
            // плагин выключается
        }
    }

    private void db(Runnable r) {
        async(plugin.storage().executor(), r);
    }

    private void async(ExecutorService ex, Runnable r) {
        try {
            ex.execute(r);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Задача отклонена: " + e.getMessage());
        }
    }
}
