package my.yeamu.pixelauth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Хеширование паролей. Вызывать ТОЛЬКО вне главного потока (PBKDF2 специально медленный).
 * Форматы:
 * <ul>
 *   <li>{@code $PA$pbkdf2-sha256$итерации$сольB64$хешB64} — свой, основной;</li>
 *   <li>{@code $SHA$соль$хеш} — AuthMe SHA256 (по умолчанию у AuthMe). После входа перехешируется в PBKDF2.</li>
 * </ul>
 * BCrypt / Argon2 из AuthMe не поддерживаются (нужна сторонняя библиотека) — таким игрокам нужен сброс пароля.
 */
public final class Passwords {

    public enum Result { OK, WRONG, UNSUPPORTED }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "$PA$pbkdf2-sha256$";

    private Passwords() {
    }

    public static String hash(String password, int iterations) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] h = pbkdf2(password, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIX + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(h);
    }

    public static Result verify(String password, String stored) {
        if (stored == null || stored.isEmpty()) return Result.UNSUPPORTED;
        try {
            if (stored.startsWith(PREFIX)) {
                String[] p = stored.substring(PREFIX.length()).split("\\$");
                if (p.length != 3) return Result.UNSUPPORTED;
                int iter = Integer.parseInt(p[0]);
                byte[] salt = Base64.getDecoder().decode(p[1]);
                byte[] expected = Base64.getDecoder().decode(p[2]);
                return MessageDigest.isEqual(expected, pbkdf2(password, salt, iter)) ? Result.OK : Result.WRONG;
            }
            if (stored.startsWith("$SHA$")) {
                // AuthMe: $SHA$соль$sha256(sha256(пароль) + соль)
                String[] p = stored.split("\\$");
                if (p.length != 4) return Result.UNSUPPORTED;
                String calc = sha256hex(sha256hex(password) + p[2]);
                return MessageDigest.isEqual(calc.getBytes(StandardCharsets.US_ASCII),
                        p[3].toLowerCase().getBytes(StandardCharsets.US_ASCII)) ? Result.OK : Result.WRONG;
            }
        } catch (RuntimeException e) {
            return Result.UNSUPPORTED;
        }
        return Result.UNSUPPORTED;
    }

    /** Нужно ли перехешировать (старый формат или меньше итераций, чем в конфиге). */
    public static boolean needsRehash(String stored, int iterations) {
        if (stored == null || !stored.startsWith(PREFIX)) return true;
        try {
            return Integer.parseInt(stored.substring(PREFIX.length()).split("\\$")[0]) < iterations;
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** Короткое название формата для /pixelauth info. */
    public static String format(String stored) {
        if (stored == null) return "-";
        if (stored.startsWith(PREFIX)) return "PBKDF2-SHA256";
        if (stored.startsWith("$SHA$")) return "AuthMe SHA256";
        if (stored.startsWith("$2")) return "BCrypt (не поддерживается)";
        return "неизвестный";
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 недоступен", e);
        }
    }

    private static String sha256hex(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
