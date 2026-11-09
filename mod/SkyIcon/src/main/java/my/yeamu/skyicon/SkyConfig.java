package my.yeamu.skyicon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки из config/skyicon.json. Если файла нет — создаётся со значениями по умолчанию. */
public final class SkyConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SkyConfig current = new SkyConfig();

    /** Включить мод. */
    public boolean enabled = true;
    /** Сколько секунд иконка висит на экране (без учёта появления и исчезания). */
    public float seconds = 3.5f;
    /** ��азмер: 1 — 24 пикселя интерфейса, 2 — 48. */
    public int scale = 1;
    /** Отступ сверху. */
    public int y = 6;
    /** Показать иконку при входе в мир. */
    public boolean showOnJoin = true;
    /** Опускать иконку под полоски боссов, если они есть. */
    public boolean avoidBossBars = true;
    /** Тихий звук при смене дня и ночи. */
    public boolean sound = false;
    /** Ночь — время мира от и до (как у кровати). */
    public int nightStart = 12542;
    public int nightEnd = 23460;

    public static SkyConfig get() {
        return current;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("skyicon.json");
        try {
            if (Files.exists(file)) {
                SkyConfig c = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), SkyConfig.class);
                if (c != null) current = c;
            }
            // Перезаписываем, чтобы в файле появились новые поля
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(current), StandardCharsets.UTF_8);
        } catch (Exception e) {
            SkyIconClient.LOG.warn("Не удалось прочитать {}: {}", file, e.toString());
        }
        current.scale = Math.max(1, Math.min(4, current.scale));
        current.seconds = Math.max(0.5f, Math.min(30f, current.seconds));
    }
}
