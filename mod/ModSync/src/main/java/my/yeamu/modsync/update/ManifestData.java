package my.yeamu.modsync.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Разобранный и проверенный manifest.json.
 */
public record ManifestData(int version, String minecraft, String modsyncVersion, String modsyncMinVersion,
                           List<ModEntry> mods, List<RemoveEntry> remove, Set<String> optional) {

    /** Мод из манифеста. */
    public record ModEntry(String id, String name, String version, String file, String url, String sha256, long size) {}

    /** Что удалить: по имени файла и/или по SHA-256. */
    public record RemoveEntry(String file, String sha256) {}

    private static final Pattern SHA = Pattern.compile("^[0-9a-f]{64}$");
    /** Безопасное имя файла: без путей, только .jar */
    private static final Pattern FILE = Pattern.compile("^[A-Za-z0-9._+\\-\\[\\]() ]{1,120}\\.jar$");

    public static ManifestData parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int format = root.has("format") ? root.get("format").getAsInt() : 1;
        if (format != 1) throw new IllegalArgumentException("неподдерживаемый формат манифеста: " + format);

        int version = root.get("version").getAsInt();
        String mc = root.has("minecraft") ? root.get("minecraft").getAsString() : "";
        JsonObject ms = root.getAsJsonObject("modsync");
        String msv = ms.get("version").getAsString();
        String msmin = ms.has("minVersion") ? ms.get("minVersion").getAsString() : msv;

        List<ModEntry> mods = new ArrayList<>();
        Set<String> files = new HashSet<>();
        for (JsonElement el : root.getAsJsonArray("mods")) {
            JsonObject o = el.getAsJsonObject();
            String file = o.get("file").getAsString();
            String sha = o.get("sha256").getAsString().toLowerCase(Locale.ROOT);
            long size = o.get("size").getAsLong();
            if (!FILE.matcher(file).matches()) throw new IllegalArgumentException("недопустимое имя файла: " + file);
            if (!SHA.matcher(sha).matches()) throw new IllegalArgumentException("недопустимый SHA-256 у " + file);
            if (size <= 0) throw new IllegalArgumentException("недопустимый размер у " + file);
            if (!files.add(file.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("дубликат файла: " + file);
            mods.add(new ModEntry(
                    o.has("id") ? o.get("id").getAsString() : file,
                    o.has("name") ? o.get("name").getAsString() : file,
                    o.has("version") ? o.get("version").getAsString() : "?",
                    file, o.get("url").getAsString(), sha, size));
        }

        List<RemoveEntry> remove = new ArrayList<>();
        if (root.has("remove")) {
            for (JsonElement el : root.getAsJsonArray("remove")) {
                JsonObject o = el.getAsJsonObject();
                String file = o.has("file") ? o.get("file").getAsString() : null;
                String sha = o.has("sha256") ? o.get("sha256").getAsString().toLowerCase(Locale.ROOT) : null;
                if (file != null && !FILE.matcher(file).matches()) continue; // игнорируем опасные пути
                if (file != null || sha != null) remove.add(new RemoveEntry(file, sha));
            }
        }

        Set<String> optional = new HashSet<>();
        if (root.has("optional")) {
            JsonArray arr = root.getAsJsonArray("optional");
            for (JsonElement el : arr) optional.add(el.getAsString().toLowerCase(Locale.ROOT));
        }
        return new ManifestData(version, mc, msv, msmin, List.copyOf(mods), List.copyOf(remove), Set.copyOf(optional));
    }
}
