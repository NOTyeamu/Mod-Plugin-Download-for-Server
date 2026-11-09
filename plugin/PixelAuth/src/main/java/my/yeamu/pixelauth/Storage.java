package my.yeamu.pixelauth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Хранилище аккаунтов (SQLite или MySQL). Все методы синхронные и блокирующие —
 * вызывать из {@link #executor()} или другого не-главного потока. Драйверы SQLite и MySQL
 * уже есть внутри Paper, отдельно ничего класть не нужно.
 */
public final class Storage {

    private static final String T = "pixelauth_accounts";

    private final File dataFolder;
    private final Settings s;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PixelAuth-DB");
        t.setDaemon(true);
        return t;
    });
    private Connection conn;
    private boolean mysql;

    public Storage(File dataFolder, Settings s) {
        this.dataFolder = dataFolder;
        this.s = s;
    }

    /** Поток для работы с базой (один, чтобы SQLite не дрался за блокировки). */
    public ExecutorService executor() {
        return executor;
    }

    public synchronized void open() throws SQLException {
        mysql = "mysql".equals(s.storageType);
        connection();
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + T + " ("
                    + "name VARCHAR(32) NOT NULL PRIMARY KEY,"
                    + "realname VARCHAR(32) NOT NULL,"
                    + "uuid VARCHAR(36),"
                    + "hash VARCHAR(255) NOT NULL,"
                    + "reg_ip VARCHAR(64),"
                    + "reg_date BIGINT NOT NULL DEFAULT 0,"
                    + "last_ip VARCHAR(64),"
                    + "last_login BIGINT NOT NULL DEFAULT 0)");
            if (!mysql) st.executeUpdate("CREATE INDEX IF NOT EXISTS " + T + "_ip ON " + T + " (reg_ip)");
        }
    }

    private Connection connection() throws SQLException {
        if (conn != null && conn.isValid(2)) return conn;
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) { }
        }
        if (mysql) {
            String url = "jdbc:mysql://" + s.mysqlHost + ":" + s.mysqlPort + "/" + s.mysqlDatabase + "?" + s.mysqlParams;
            conn = DriverManager.getConnection(url, s.mysqlUser, s.mysqlPassword);
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Драйвер SQLite не найден", e);
            }
            dataFolder.mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
            }
        }
        return conn;
    }

    public synchronized Account find(String name) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM " + T + " WHERE name = ?")) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        }
    }

    /** @return false, если такой ник уже занят */
    public synchronized boolean insert(Account a) throws SQLException {
        String sql = (mysql ? "INSERT IGNORE INTO " : "INSERT OR IGNORE INTO ") + T
                + " (name, realname, uuid, hash, reg_ip, reg_date, last_ip, last_login) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            fill(ps, a);
            return ps.executeUpdate() > 0;
        }
    }

    /** Массовая вставка для импорта. Существующие ники пропускает. @return сколько добавлено */
    public synchronized int insertAll(List<Account> list) throws SQLException {
        Connection c = connection();
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        int added = 0;
        String sql = (mysql ? "INSERT IGNORE INTO " : "INSERT OR IGNORE INTO ") + T
                + " (name, realname, uuid, hash, reg_ip, reg_date, last_ip, last_login) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Account a : list) {
                fill(ps, a);
                added += ps.executeUpdate() > 0 ? 1 : 0;
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(auto);
        }
        return added;
    }

    public synchronized void updateHash(String name, String hash) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("UPDATE " + T + " SET hash = ? WHERE name = ?")) {
            ps.setString(1, hash);
            ps.setString(2, name.toLowerCase(Locale.ROOT));
            ps.executeUpdate();
        }
    }

    public synchronized void updateLogin(String name, String ip, long time, String uuid) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "UPDATE " + T + " SET last_ip = ?, last_login = ?, uuid = COALESCE(?, uuid) WHERE name = ?")) {
            ps.setString(1, ip);
            ps.setLong(2, time);
            ps.setString(3, uuid);
            ps.setString(4, name.toLowerCase(Locale.ROOT));
            ps.executeUpdate();
        }
    }

    public synchronized boolean delete(String name) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("DELETE FROM " + T + " WHERE name = ?")) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            return ps.executeUpdate() > 0;
        }
    }

    public synchronized int countByIp(String ip) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT COUNT(*) FROM " + T + " WHERE reg_ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public synchronized int count() throws SQLException {
        try (Statement st = connection().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + T)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) { }
                conn = null;
            }
        }
    }

    private static void fill(PreparedStatement ps, Account a) throws SQLException {
        ps.setString(1, a.name().toLowerCase(Locale.ROOT));
        ps.setString(2, a.realName());
        ps.setString(3, a.uuid());
        ps.setString(4, a.hash());
        ps.setString(5, a.regIp());
        ps.setLong(6, a.regDate());
        ps.setString(7, a.lastIp());
        ps.setLong(8, a.lastLogin());
    }

    private static Account read(ResultSet rs) throws SQLException {
        return new Account(rs.getString("name"), rs.getString("realname"), rs.getString("uuid"), rs.getString("hash"),
                rs.getString("reg_ip"), rs.getLong("reg_date"), rs.getString("last_ip"), rs.getLong("last_login"));
    }
}
