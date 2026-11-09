package my.yeamu.modsync.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хеши файлов в папке mods/ и общий «хеш списка модов».
 * Тот же алгоритм использует плагин ModGate и скрипт tools/manifest.py:
 *   SHA-256( join("\n", sorted(sha256 каждого .jar)) )
 */
public final class ModsHasher {

    private ModsHasher() {}

    /** Файл мода и его SHA-256. */
    public record ModFile(Path path, String sha256) {}

    /** Кэш хешей: путь → (размер, время изменения, хеш). Чтобы не пересчитывать при каждом входе. */
    private record Cached(long size, long mtime, String sha) {}
    private static final Map<Path, Cached> CACHE = new ConcurrentHashMap<>();

    public static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Cached(Path file) throws IOException {
        BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
        Path key = file.toAbsolutePath().normalize();
        Cached c = CACHE.get(key);
        long mtime = a.lastModifiedTime().toMillis();
        if (c != null && c.size == a.size() && c.mtime == mtime) return c.sha;
        String sha = sha256(file);
        CACHE.put(key, new Cached(a.size(), mtime, sha));
        return sha;
    }

    /** Все .jar непосредственно в mods/ (подпапки и .disabled не учитываются). */
    public static List<ModFile> scan(Path modsDir) throws IOException {
        List<ModFile> out = new ArrayList<>();
        if (!Files.isDirectory(modsDir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) out.add(new ModFile(p, sha256Cached(p)));
            }
        }
        out.sort((x, y) -> x.path().getFileName().toString().compareToIgnoreCase(y.path().getFileName().toString()));
        return out;
    }

    /** Общий хеш списка модов (опциональные моды из манифеста исключаются). */
    public static String listHash(List<ModFile> files, Set<String> optional) {
        List<String> shas = new ArrayList<>();
        for (ModFile f : files) {
            String s = f.sha256().toLowerCase(Locale.ROOT);
            if (!optional.contains(s)) shas.add(s);
        }
        shas.sort(String::compareTo);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(String.join("\n", shas).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
