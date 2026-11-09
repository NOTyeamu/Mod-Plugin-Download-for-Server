package my.yeamu.pixelauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Тексты из раздела messages. Цвета — через &amp;-коды. Плейсхолдеры {ключ}. */
public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final FileConfiguration cfg;

    public Msg(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    /** Сырая строка с &amp;-кодами (для PixelUI). pairs: "name", "Steve", … */
    public String raw(String key, Object... pairs) {
        return fill(cfg.getString("messages." + key, key), pairs);
    }

    public List<String> rawList(String key, Object... pairs) {
        List<String> out = new ArrayList<>();
        for (String s : cfg.getStringList("messages." + key)) out.add(fill(s, pairs));
        return out;
    }

    public Component comp(String key, Object... pairs) {
        return LEGACY.deserialize(raw(key, pairs));
    }

    public static Component color(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : legacy);
    }

    /** Сообщение в чат с префиксом. */
    public void send(Player p, String key, Object... pairs) {
        p.sendMessage(color(raw("prefix") + raw(key, pairs)));
    }

    /** Как send, но со своим текстом, если ключа нет в config.yml (старые конфиги). */
    public void sendOr(Player p, String key, String def, Object... pairs) {
        String s = cfg.getString("messages." + key);
        p.sendMessage(color(raw("prefix") + fill(s == null ? def : s, pairs)));
    }

    private static String fill(String s, Object... pairs) {
        if (s == null) return "";
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return s.replace("\\n", "\n");
    }
}
