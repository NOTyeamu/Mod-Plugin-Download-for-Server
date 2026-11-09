package my.yeamu.modgate.util;

/**
 * Сравнение версий вида 1.2.10, 1.0.0-beta.2 и т.п.
 */
public final class VersionUtil {

    private VersionUtil() {}

    /** Отрицательное, если a < b; 0, если равны; положительное, если a > b. */
    public static int compare(String a, String b) {
        if (a == null) a = "0";
        if (b == null) b = "0";
        // Метаданные сборки после '+' не влияют на порядок
        a = a.split("\\+", 2)[0];
        b = b.split("\\+", 2)[0];
        String[] pa = a.split("[.-]");
        String[] pb = b.split("[.-]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String x = i < pa.length ? pa[i] : "0";
            String y = i < pb.length ? pb[i] : "0";
            int c;
            if (isNum(x) && isNum(y)) c = Long.compare(Long.parseLong(x), Long.parseLong(y));
            else if (isNum(x)) c = 1;   // 1.0.0 > 1.0.0-beta
            else if (isNum(y)) c = -1;
            else c = x.compareToIgnoreCase(y);
            if (c != 0) return c;
        }
        return 0;
    }

    private static boolean isNum(String s) {
        if (s.isEmpty() || s.length() > 18) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }
}
