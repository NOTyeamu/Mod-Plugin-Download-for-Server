package my.yeamu.pluginsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * plugins.json с сайта:
 * <pre>
 * { "format": 1, "version": 3, "generated": "...",
 *   "plugins": [ { "id", "name", "version", "file", "url", "sha256", "size", "description" } ],
 *   "remove":  [ { "name": "OldPlugin" } | { "file": "Old-1.0.jar" } ] }
 * </pre>
 */
public record PluginManifest(int version, String generated, List<Entry> plugins, List<RemoveRule> remove) {

    /** Имя файла без путей — защита от «../» в манифесте. */
    private static final Pattern SAFE_FILE = Pattern.compile("[A-Za-z0-9._+\\-]{1,128}\\.jar");
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{64}");

    public record Entry(String id, String name, String version, String file, String url,
                        String sha256, long size, String description) { }

    public record RemoveRule(String name, String file) { }

    public static PluginManifest parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        int format = o.has("format") ? o.get("format").getAsInt() : 1;
        if (format != 1) throw new IllegalArgumentException("неизвестный формат plugins.json: " + format);
        List<Entry> list = new ArrayList<>();
        JsonArray arr = o.has("plugins") ? o.getAsJsonArray("plugins") : new JsonArray();
        for (JsonElement el : arr) {
            JsonObject p = el.getAsJsonObject();
            String name = str(p, "name");
            String file = str(p, "file");
            String sha = str(p, "sha256").toLowerCase(Locale.ROOT);
            if (name.isEmpty()) throw new IllegalArgumentException("плагин без name");
            if (!SAFE_FILE.matcher(file).matches()) throw new IllegalArgumentException("плохое имя файла: " + file);
            if (!SHA.matcher(sha).matches()) throw new IllegalArgumentException("плохой sha256 у " + name);
            list.add(new Entry(str(p, "id"), name, str(p, "version"), file, str(p, "url"), sha,
                    p.has("size") ? p.get("size").getAsLong() : -1L, str(p, "description")));
        }
        List<RemoveRule> rm = new ArrayList<>();
        if (o.has("remove")) {
            for (JsonElement el : o.getAsJsonArray("remove")) {
                JsonObject r = el.getAsJsonObject();
                String file = str(r, "file");
                if (!file.isEmpty() && !SAFE_FILE.matcher(file).matches()) continue;
                rm.add(new RemoveRule(str(r, "name"), file));
            }
        }
        return new PluginManifest(o.has("version") ? o.get("version").getAsInt() : 0,
                str(o, "generated"), List.copyOf(list), List.copyOf(rm));
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /**
     * Сравнение версий «1.10.2» > «1.9»: по числам, хвосты вроде «-SNAPSHOT» — строкой.
     * &lt;0 — a старше b, 0 — равны, &gt;0 — a новее.
     */
    public static int compareVersions(String a, String b) {
        String[] pa = a.split("[.\\-+ ]");
        String[] pb = b.split("[.\\-+ ]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String x = i < pa.length ? pa[i] : "0";
            String y = i < pb.length ? pb[i] : "0";
            boolean nx = x.matches("\\d{1,9}");
            boolean ny = y.matches("\\d{1,9}");
            int c;
            if (nx && ny) c = Integer.compare(Integer.parseInt(x), Integer.parseInt(y));
            else if (nx) c = 1;      // «1.0.1» новее «1.0-beta»
            else if (ny) c = -1;
            else c = x.compareToIgnoreCase(y);
            if (c != 0) return c;
        }
        return 0;
    }
}
