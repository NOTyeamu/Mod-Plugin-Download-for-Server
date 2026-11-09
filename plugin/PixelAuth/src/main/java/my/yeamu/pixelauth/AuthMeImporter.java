package my.yeamu.pixelauth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Импорт аккаунтов из AuthMe (SQLite: plugins/AuthMe/authme.db, таблица authme).
 * Пароли переносятся как есть: SHA256 из AuthMe проверяется и при первом входе перехешируется.
 * Вызывать не из главного потока.
 */
final class AuthMeImporter {

    record Result(int read, int added, int unsupported) {
    }

    private AuthMeImporter() {
    }

    static Result run(File pluginsDir, String fileName, String table, Storage storage) throws SQLException {
        File db = new File(new File(pluginsDir, "AuthMe"), fileName);
        if (!db.isFile()) throw new SQLException("Не найден файл " + db.getPath());
        if (!table.matches("[A-Za-z0-9_]+")) throw new SQLException("Недопустимое имя таблицы");
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер SQLite не найден", e);
        }
        List<Account> list = new ArrayList<>();
        int unsupported = 0;
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData md = rs.getMetaData();
            Set<String> cols = new HashSet<>();
            for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnName(i).toLowerCase(Locale.ROOT));
            long now = System.currentTimeMillis();
            while (rs.next()) {
                String name = str(rs, cols, "username");
                String hash = str(rs, cols, "password");
                if (name == null || name.isBlank() || hash == null || hash.isBlank()) continue;
                String real = str(rs, cols, "realname");
                if (real == null || real.isBlank()) real = name;
                String regIp = str(rs, cols, "regip");
                if (regIp == null) regIp = str(rs, cols, "ip");
                long regDate = num(rs, cols, "regdate");
                if (Passwords.format(hash).startsWith("BCrypt") || Passwords.format(hash).equals("неизвестный")) unsupported++;
                // lastLogin = 0: сессии AuthMe не переносим, первый вход — по паролю
                list.add(new Account(name.toLowerCase(Locale.ROOT), real, null, hash, regIp,
                        regDate > 0 ? regDate : now, str(rs, cols, "ip"), 0));
            }
        }
        int added = storage.insertAll(list);
        return new Result(list.size(), added, unsupported);
    }

    private static String str(ResultSet rs, Set<String> cols, String col) throws SQLException {
        return cols.contains(col) ? rs.getString(col) : null;
    }

    private static long num(ResultSet rs, Set<String> cols, String col) {
        if (!cols.contains(col)) return 0;
        try {
            return rs.getLong(col);
        } catch (SQLException e) {
            return 0;
        }
    }
}
