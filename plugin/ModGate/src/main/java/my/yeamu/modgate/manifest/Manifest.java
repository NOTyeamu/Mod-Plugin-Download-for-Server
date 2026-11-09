package my.yeamu.modgate.manifest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Разобранный manifest.json — только то, что нужно серверу.
 *
 * @param version       версия манифеста (целое, растёт с каждой сборкой)
 * @param modVersion    актуальная версия ModSync
 * @param minModVersion минимально допустимая версия ModSync
 * @param mods          обязательные моды
 * @param expectedHash  ожидаемый хеш списка модов (см. computeModsHash)
 */
public record Manifest(int version, String modVersion, String minModVersion,
                       List<ModEntry> mods, String expectedHash, String minecraft) {

    public record ModEntry(String id, String name, String version, String sha256) {}

    public static Manifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int version = root.get("version").getAsInt();
        String minecraft = root.has("minecraft") ? root.get("minecraft").getAsString() : "?";

        JsonObject ms = root.getAsJsonObject("modsync");
        String modVersion = ms.get("version").getAsString();
        String minModVersion = ms.has("minVersion") ? ms.get("minVersion").getAsString() : modVersion;

        List<ModEntry> mods = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        JsonArray arr = root.getAsJsonArray("mods");
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            String sha = o.get("sha256").getAsString().toLowerCase(Locale.ROOT);
            mods.add(new ModEntry(
                    o.has("id") ? o.get("id").getAsString() : "?",
                    o.get("name").getAsString(),
                    o.get("version").getAsString(),
                    sha));
            hashes.add(sha);
        }
        return new Manifest(version, modVersion, minModVersion, List.copyOf(mods), computeModsHash(hashes), minecraft);
    }

    /**
     * Хеш списка модов: SHA-256 от отсортированных SHA-256 файлов, соединённых через "\n".
     * Клиент ModSync считает его точно так же по своей папке mods/.
     */
    public static String computeModsHash(List<String> sha256List) {
        try {
            List<String> sorted = new ArrayList<>();
            for (String s : sha256List) sorted.add(s.toLowerCase(Locale.ROOT));
            sorted.sort(String::compareTo);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.join("\n", sorted).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean containsSha(String sha) {
        for (ModEntry m : mods) if (m.sha256().equalsIgnoreCase(sha)) return true;
        return false;
    }
}
