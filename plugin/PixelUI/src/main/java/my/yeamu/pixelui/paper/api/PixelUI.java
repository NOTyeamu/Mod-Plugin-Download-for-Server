package my.yeamu.pixelui.paper.api;

import my.yeamu.pixelui.paper.PixelUIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Главный вход для других плагинов. Можно вызывать из любого потока —
 * работа сама перенесётся в главный поток. Игрокам без мода показывается
 * запасной вариант из config.yml (чат / actionbar / title / bossbar).
 */
public final class PixelUI {

    private PixelUI() {}

    /** У игрока установлен клиентский мод PixelUI. */
    public static boolean hasMod(Player player) {
        return PixelUIPlugin.get().service().hasMod(player);
    }

    /** Версия мода или null. */
    public static String modVersion(Player player) {
        return PixelUIPlugin.get().service().version(player);
    }

    public static void notify(Player player, PixelNotification n) {
        PixelUIPlugin.get().service().notify(player, n);
    }

    public static void notify(Player player, String title, String text) {
        notify(player, PixelNotification.of(title, text));
    }

    public static void notify(Collection<? extends Player> players, PixelNotification n) {
        for (Player p : players) notify(p, n);
    }

    /** Всем игрокам онлайн. */
    public static void notifyAll(PixelNotification n) {
        notify(Bukkit.getOnlinePlayers(), n);
    }

    public static void banner(Player player, PixelBanner b) {
        PixelUIPlugin.get().service().banner(player, b);
    }

    /** id = null — убрать все плашки игрока. */
    public static void removeBanner(Player player, String id) {
        PixelUIPlugin.get().service().removeBanner(player, id);
    }

    public static void openMenu(Player player, PixelMenu menu) {
        PixelUIPlugin.get().service().openMenu(player, menu);
    }

    public static void closeMenu(Player player) {
        PixelUIPlugin.get().service().closeMenu(player);
    }

    /** Открытое сейчас меню игрока или null. */
    public static PixelMenu openedMenu(Player player) {
        return PixelUIPlugin.get().service().openedMenu(player);
    }

    /** Задать вопрос «Да (Y) / Нет (N)». Тот же id заменяет предыдущий вопрос (он получит CANCELLED). */
    public static void prompt(Player player, PixelPrompt prompt) {
        PixelUIPlugin.get().service().prompt(player, prompt);
    }

    /** Быстрый вопрос: ответ придёт в callback (YES / NO / TIMEOUT / CANCELLED). */
    public static void ask(Player player, String id, String question, java.util.function.Consumer<PixelAnswer> callback) {
        prompt(player, PixelPrompt.builder(id).question(question)
                .onAnswer((p, a) -> callback.accept(a)).build());
    }

    /** id = null — отменить все вопросы игрока. */
    public static void cancelPrompt(Player player, String id) {
        PixelUIPlugin.get().service().cancelPrompt(player, id);
    }

    /** Ждёт ли игрок ответа на вопрос (id = null — на любой). */
    public static boolean hasPrompt(Player player, String id) {
        return PixelUIPlugin.get().service().hasPrompt(player, id);
    }

    /** Убрать все уведомления, плашки и вопросы игрока. */
    /**
     * Открыть окно ввода (пароль, имя, код…). Работает только с модом:
     * false — мода нет, ничего не показано.
     */
    public static boolean input(Player player, PixelInput input) {
        return PixelUIPlugin.get().service().input(player, input);
    }

    /** Показать ошибку в окне ввода (окно трясётся, поля-пароли очищаются). */
    public static void inputError(Player player, String id, String error) {
        PixelUIPlugin.get().service().inputError(player, id, error, true);
    }

    public static void inputError(Player player, String id, String error, boolean clearPasswords) {
        PixelUIPlugin.get().service().inputError(player, id, error, clearPasswords);
    }

    /** Зелёное сообщение в открытом окне ввода (например «Запрос отправлен»). Поля не очищаются. */
    public static void inputInfo(Player player, String id, String text) {
        PixelUIPlugin.get().service().inputInfo(player, id, text);
    }

    /** Закрыть окно ввода. id = null — любое открытое. */
    public static void closeInput(Player player, String id) {
        PixelUIPlugin.get().service().closeInput(player, id);
    }

    public static boolean hasInput(Player player, String id) {
        return PixelUIPlugin.get().service().hasInput(player, id);
    }

    public static void clear(Player player) {
        PixelUIPlugin.get().service().clear(player);
    }
}
