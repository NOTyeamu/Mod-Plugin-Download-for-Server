package my.yeamu.pixelui.paper.command;

import my.yeamu.pixelui.paper.PixelUIPlugin;
import my.yeamu.pixelui.paper.api.PixelAnswer;
import my.yeamu.pixelui.paper.api.PixelBanner;
import my.yeamu.pixelui.paper.api.PixelPrompt;
import my.yeamu.pixelui.paper.api.PixelButtonStyle;
import my.yeamu.pixelui.paper.api.PixelIcon;
import my.yeamu.pixelui.paper.api.PixelMenu;
import my.yeamu.pixelui.paper.api.PixelNotification;
import my.yeamu.pixelui.paper.api.PixelPosition;
import my.yeamu.pixelui.paper.api.PixelTheme;
import my.yeamu.pixelui.paper.api.PixelUI;
import my.yeamu.pixelui.paper.internal.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * /pixelui notify|banner|unbanner|prompt|clear|demo|status|reload
 * /pixelui click и /pixelui answer — служебные, их вызывают кнопки в чате (доступны всем).
 */
public final class PixelUICommand implements TabExecutor {

    private static final List<String> SUBS = List.of("notify", "banner", "unbanner", "prompt", "clear", "demo", "status", "reload");
    private static final List<String> THEMES = List.of("dark", "stone", "success", "error", "warning", "info", "gold", "ender", "nether");

    private final PixelUIPlugin plugin;

    public PixelUICommand(PixelUIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("click")) {
            if (s instanceof Player p && a.length >= 3) plugin.service().clickByToken(p, a[1], a[2]);
            return true;
        }
        if (a.length >= 1 && a[0].equalsIgnoreCase("answer")) {
            if (s instanceof Player p && a.length >= 3) {
                if (!plugin.service().answerByToken(p, a[1], a[2].equalsIgnoreCase("yes"))) {
                    msg(s, "&7Этот вопрос уже неактуален");
                }
            }
            return true;
        }
        if (!s.hasPermission("pixelui.admin")) {
            msg(s, "&cНет права pixelui.admin");
            return true;
        }
        if (a.length == 0) {
            help(s, label);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "notify" -> notifyCmd(s, a);
            case "banner" -> bannerCmd(s, a);
            case "prompt" -> promptCmd(s, a);
            case "unbanner" -> {
                if (a.length < 2) { msg(s, "&e/pixelui unbanner <игрок|*> [id]"); return true; }
                for (Player p : targets(s, a[1])) PixelUI.removeBanner(p, a.length >= 3 ? a[2] : null);
            }
            case "clear" -> {
                if (a.length < 2) { msg(s, "&e/pixelui clear <игрок|*>"); return true; }
                for (Player p : targets(s, a[1])) PixelUI.clear(p);
            }
            case "demo" -> {
                Player p = a.length >= 2 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player pl ? pl : null);
                if (p == null) { msg(s, "&cУкажите игрока онлайн"); return true; }
                demo(p);
                if (p != s) msg(s, "&aДемо отправлено " + p.getName());
            }
            case "status" -> {
                if (a.length >= 2) {
                    Player p = Bukkit.getPlayerExact(a[1]);
                    if (p == null) { msg(s, "&cИгрок не в сети"); return true; }
                    String v = PixelUI.modVersion(p);
                    msg(s, "&7" + p.getName() + ": " + (PixelUI.hasMod(p)
                            ? "&aмод PixelUI " + (v == null ? "(версия ещё не пришла)" : v)
                            : "&cбез мода &7(запасной вариант)"));
                } else {
                    msg(s, "&aPixelUI " + plugin.getPluginMeta().getVersion() + "&7: с модом &f"
                            + plugin.service().moddedOnline() + "&7 из &f" + Bukkit.getOnlinePlayers().size() + "&7 онлайн");
                }
            }
            case "reload" -> {
                plugin.reloadSettings();
                msg(s, "&aКонфиг PixelUI перезагружен");
            }
            default -> help(s, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        msg(s, "&6PixelUI &7— команды:");
        msg(s, "&e/" + label + " notify <игрок|*> <тема> <заголовок | текст>");
        msg(s, "&e/" + label + " banner <игрок|*> <id> <секунд, 0 = навсегда> <текст>");
        msg(s, "&e/" + label + " unbanner <игрок|*> [id]&7, &e/" + label + " clear <игрок|*>");
        msg(s, "&e/" + label + " prompt <игрок|*> <секунд> <вопрос | пояснение>&7 — вопрос Да/Нет");
        msg(s, "&e/" + label + " demo [игрок]&7, &e/" + label + " status [игрок]&7, &e/" + label + " reload");
    }

    private void notifyCmd(CommandSender s, String[] a) {
        if (a.length < 4) { msg(s, "&e/pixelui notify <игрок|*> <тема> <заголовок | текст>"); return; }
        String all = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
        int bar = all.indexOf('|');
        String title = bar < 0 ? all : all.substring(0, bar).trim();
        String text = bar < 0 ? "" : all.substring(bar + 1).trim().replace("\\n", "\n");
        PixelTheme theme = PixelTheme.preset(a[2]);
        PixelNotification n = PixelNotification.builder().title(title).text(text)
                .theme(theme).icon(iconFor(theme.presetName())).build();
        Collection<Player> t = targets(s, a[1]);
        for (Player p : t) PixelUI.notify(p, n);
        msg(s, "&aОтправлено: " + t.size());
    }

    private void bannerCmd(CommandSender s, String[] a) {
        if (a.length < 5) { msg(s, "&e/pixelui banner <игрок|*> <id> <секунд> <текст>"); return; }
        long sec;
        try {
            sec = Long.parseLong(a[3]);
        } catch (NumberFormatException e) {
            msg(s, "&cСекунды — число");
            return;
        }
        PixelBanner b = PixelBanner.builder(a[2]).text(String.join(" ", Arrays.copyOfRange(a, 4, a.length)))
                .theme(PixelTheme.INFO).icon(PixelIcon.BELL).duration(sec * 1000L).build();
        Collection<Player> t = targets(s, a[1]);
        for (Player p : t) PixelUI.banner(p, b);
        msg(s, "&aОтправлено: " + t.size());
    }

    /** Тестовый вопрос: ответ придёт отправителю команды. */
    private void promptCmd(CommandSender s, String[] a) {
        if (a.length < 4) { msg(s, "&e/pixelui prompt <игрок|*> <секунд, 0 = без лимита> <вопрос | пояснение>"); return; }
        long sec;
        try {
            sec = Long.parseLong(a[2]);
        } catch (NumberFormatException e) {
            msg(s, "&cСекунды — число");
            return;
        }
        String all = String.join(" ", Arrays.copyOfRange(a, 3, a.length));
        int bar = all.indexOf('|');
        String q = bar < 0 ? all : all.substring(0, bar).trim();
        String text = bar < 0 ? "" : all.substring(bar + 1).trim().replace("\\n", "\n");
        PixelPrompt pr = PixelPrompt.builder("admin-test").question(q).text(text).theme(PixelTheme.INFO)
                .icon(PixelIcon.BELL).timeout(sec * 1000L)
                .onAnswer((pl, ans) -> {
                    if (ans != PixelAnswer.CANCELLED) msg(s, "&7" + pl.getName() + " ответил: " + switch (ans) {
                        case YES -> "&aДа";
                        case NO -> "&cНет";
                        default -> "&eвремя вышло";
                    });
                }).build();
        Collection<Player> t = targets(s, a[1]);
        for (Player p : t) PixelUI.prompt(p, pr);
        msg(s, "&aВопрос отправлен: " + t.size());
    }

    private static String iconFor(String theme) {
        return switch (theme) {
            case "success" -> PixelIcon.CHECK;
            case "error", "nether" -> PixelIcon.CROSS;
            case "warning" -> PixelIcon.WARN;
            case "gold" -> PixelIcon.COIN;
            case "ender" -> PixelIcon.STAR;
            default -> PixelIcon.INFO;
        };
    }

    /** Показ всех возможностей: тосты по очереди, плашка с прогрессом, меню. */
    private void demo(Player p) {
        PixelNotification[] toasts = {
                PixelNotification.builder().title("&aДобро пожаловать!").text("Это кастомное уведомление PixelUI")
                        .icon(PixelIcon.CHECK).theme(PixelTheme.SUCCESS).sound("minecraft:entity.experience_orb.pickup").build(),
                PixelNotification.builder().title("&6+50 монет").text("За ежедневный вход")
                        .icon(PixelIcon.COIN).theme(PixelTheme.GOLD).build(),
                PixelNotification.builder().title("Найден алмаз").text("Иконкой может быть любой предмет")
                        .icon(PixelIcon.item(Material.DIAMOND)).theme(PixelTheme.INFO).position(PixelPosition.TOP_LEFT).build(),
                PixelNotification.builder().title("&cВнимание").text("Рестарт через 5 минут")
                        .icon(PixelIcon.WARN).theme(PixelTheme.ERROR).position(PixelPosition.BOTTOM_RIGHT).build(),
                PixelNotification.builder().title("Своя тема").text("Цвета можно задать вручную")
                        .icon(PixelIcon.HEART).theme(PixelTheme.ENDER.accent(0xFF55FF).title(0xFFAAFF)).position(PixelPosition.BOTTOM_LEFT).build()
        };
        for (int i = 0; i < toasts.length; i++) {
            PixelNotification n = toasts[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) PixelUI.notify(p, n); }, 1L + i * 12L);
        }

        // Плашка с анимированным прогрессом (~10 секунд)
        new BukkitRunnable() {
            int step;

            @Override
            public void run() {
                if (!p.isOnline() || step > 20) {
                    if (p.isOnline()) PixelUI.removeBanner(p, "demo");
                    cancel();
                    return;
                }
                PixelBanner.builder("demo").text("&eИвент «Дракон» &7— загрузка " + (step * 5) + "%")
                        .icon(PixelIcon.CLOCK).theme(PixelTheme.WARNING).progress(step / 20f).send(p);
                step++;
            }
        }.runTaskTimer(plugin, 20L, 10L);

        // Вопрос Да/Нет: «Да» откроет меню
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            PixelPrompt.builder("pixelui-demo").question("&eОткрыть демо-меню?")
                    .text("&7Ответьте клавишей прямо в игре")
                    .icon(PixelIcon.STAR).theme(PixelTheme.ENDER).timeout(15_000)
                    .onYes(this::demoMenu)
                    .onNo(pl -> PixelUI.notify(pl, PixelNotification.builder().title("Хорошо")
                            .text("Меню можно открыть позже: /pixelui demo").icon(PixelIcon.INFO).theme(PixelTheme.INFO).build()))
                    .onTimeout(pl -> PixelUI.notify(pl, PixelNotification.builder().title("&eВремя вышло")
                            .icon(PixelIcon.CLOCK).theme(PixelTheme.WARNING).build()))
                    .send(p);
        }, 70L);
    }

    private void demoMenu(Player p) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            PixelMenu.builder("pixelui-demo").title("&6Демо PixelUI").icon(PixelIcon.STAR).theme(PixelTheme.DARK)
                    .columns(2).width(240)
                    .lines("Это кастомное пиксельное меню.", "&7Кнопки вызывают код на сервере.")
                    .button("ok", "Получить награду", b -> b.style(PixelButtonStyle.SUCCESS).icon(PixelIcon.GIFT)
                            .tooltip("Пришлёт уведомление")
                            .onClick(pl -> PixelUI.notify(pl, PixelNotification.builder().title("&aНаграда получена")
                                    .text("Кнопка отработала").icon(PixelIcon.GIFT).theme(PixelTheme.SUCCESS).build())))
                    .button("info", "Инфо", b -> b.style(PixelButtonStyle.INFO).icon(PixelIcon.INFO)
                            .onClick(pl -> PixelUI.notify(pl, PixelNotification.builder().title("PixelUI")
                                    .text("Версия мода: " + PixelUI.modVersion(pl)).theme(PixelTheme.INFO).build())))
                    .button("locked", "Закрыто", b -> b.enabled(false).icon(PixelIcon.SHIELD))
                    .button("close", "Закрыть", b -> b.style(PixelButtonStyle.DANGER).icon(PixelIcon.CROSS).closes())
                    .open(p);
        });
    }

    private static Collection<Player> targets(CommandSender s, String arg) {
        if (arg.equals("*") || arg.equalsIgnoreCase("@a")) return new ArrayList<>(Bukkit.getOnlinePlayers());
        Player p = Bukkit.getPlayerExact(arg);
        if (p == null) {
            msg(s, "&cИгрок " + arg + " не в сети");
            return List.of();
        }
        return List.of(p);
    }

    private static void msg(CommandSender s, String text) {
        s.sendMessage(Txt.component(text));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("pixelui.admin")) return List.of();
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(SUBS);
        } else if (a.length == 2 && !a[0].equalsIgnoreCase("reload")) {
            if (!a[0].equalsIgnoreCase("demo") && !a[0].equalsIgnoreCase("status")) out.add("*");
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("notify")) {
            out.addAll(THEMES);
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
