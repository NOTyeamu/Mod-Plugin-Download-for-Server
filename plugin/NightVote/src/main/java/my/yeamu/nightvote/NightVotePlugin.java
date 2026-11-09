package my.yeamu.nightvote;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NightVote — голосование за пропуск ночи. Вопрос задаётся плашкой «Да (Y) / Нет (N)» из PixelUI;
 * игроки без мода отвечают кнопками в чате (это делает сам PixelUI).
 * Если большинство за — время сразу перематывается на утро, иначе ничего не происходит.
 */
public final class NightVotePlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private VoteManager votes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        votes = new VoteManager(this);
        getServer().getPluginManager().registerEvents(votes, this);
        NightVoteCommand cmd = new NightVoteCommand(this, votes);
        var pc = getCommand("nightvote");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        votes.startWatcher();
        getLogger().info("NightVote " + getPluginMeta().getVersion() + " запущен");
    }

    @Override
    public void onDisable() {
        if (votes != null) votes.shutdown();
    }

    /** Текст сообщения из messages.* (с запасным вариантом, если ключа нет). */
    public String msg(String key, String def) {
        String s = getConfig().getString("messages." + key);
        return s == null ? def : s;
    }

    public static Component color(String s) {
        return LEGACY.deserialize(s == null ? "" : s);
    }

    public void send(CommandSender to, String key, String def, Object... kv) {
        String s = msg(key, def);
        if (s.isEmpty()) return;
        to.sendMessage(color(msg("prefix", "&8[&9Ночь&8] ") + fill(s, kv)));
    }

    /** Подстановка {ключ} → значение, аргументы парами: "player", "Steve", ... */
    public static String fill(String s, Object... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return s;
    }
}
