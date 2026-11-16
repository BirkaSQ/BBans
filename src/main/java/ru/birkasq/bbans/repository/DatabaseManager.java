package ru.birkasq.bbans.repository;

import ru.birkasq.bbans.BBansPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Подключение к SQLite (драйвер входит в состав Paper), создание таблиц, закрытие соединения.
 * Используется одно соединение; доступ к нему сериализован.
 */
public class DatabaseManager {

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final BBansPlugin plugin;
    private final Object lock = new Object();
    private Connection connection;

    public DatabaseManager(BBansPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException, ClassNotFoundException {
        String type = plugin.getConfig().getString("database.type", "SQLITE");
        if (!"SQLITE".equalsIgnoreCase(type)) {
            plugin.getLogger().warning("Database type '" + type + "' is not supported yet, using SQLITE.");
        }
        Class.forName("org.sqlite.JDBC");

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Cannot create plugin data folder");
        }
        File file = new File(dataFolder, plugin.getConfig().getString("database.file", "bbans.db"));

        synchronized (lock) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("CREATE TABLE IF NOT EXISTS punishments ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "player_uuid TEXT NOT NULL,"
                        + "player_name TEXT NOT NULL,"
                        + "type TEXT NOT NULL,"
                        + "reason TEXT NOT NULL,"
                        + "moderator_uuid TEXT NOT NULL,"
                        + "moderator_name TEXT NOT NULL,"
                        + "moderator_priority INTEGER NOT NULL DEFAULT 0,"
                        + "created_at INTEGER NOT NULL,"
                        + "expires_at INTEGER,"
                        + "active INTEGER NOT NULL,"
                        + "status TEXT NOT NULL,"
                        + "removed_by_uuid TEXT,"
                        + "removed_by_name TEXT,"
                        + "removed_at INTEGER,"
                        + "auto INTEGER NOT NULL DEFAULT 0)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_punishments_player ON punishments(player_uuid)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(type, active)");
                st.execute("CREATE TABLE IF NOT EXISTS players ("
                        + "uuid TEXT PRIMARY KEY,"
                        + "name TEXT NOT NULL,"
                        + "priority INTEGER NOT NULL DEFAULT 0,"
                        + "last_seen INTEGER NOT NULL)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON players(name COLLATE NOCASE)");
            }
        }
    }

    public <T> T execute(SqlFunction<T> function) {
        synchronized (lock) {
            if (connection == null) {
                throw new DataAccessException("Database is not connected", null);
            }
            try {
                return function.apply(connection);
            } catch (SQLException e) {
                throw new DataAccessException("Database error: " + e.getMessage(), e);
            }
        }
    }

    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error while closing database: " + e.getMessage());
                }
                connection = null;
            }
        }
    }
}
