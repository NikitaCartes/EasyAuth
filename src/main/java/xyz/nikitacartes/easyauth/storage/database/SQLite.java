package xyz.nikitacartes.easyauth.storage.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class SQLite implements DbApi {
    private final StorageConfigV1 config;
    private Connection connection;

    public SQLite(StorageConfigV1 config) {
        this.config = config;
    }

    @Override
    public void connect() throws DBApiException {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Connect to the database file
            File dbFile = new File(EasyAuth.gameDirectory + "/" + config.sqlite.sqlitePath);
            if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs()) {
                throw new DBApiException("Failed to create directory for SQLite database", null);
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Create tables if they don't exist
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS %s (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                username TEXT UNIQUE NOT NULL,
                                username_lower TEXT NOT NULL,
                                uuid TEXT NULL,
                                last_ip TEXT NULL,
                                data TEXT NOT NULL
                            );
                            """.formatted(config.sqlite.sqliteTable)
            );
            statement.close();

            // Check if last_ip column exists
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, config.sqlite.sqliteTable, "last_ip");
            if (!columns.next()) {
                Statement alterStatement = connection.createStatement();
                alterStatement.executeUpdate("ALTER TABLE " + config.sqlite.sqliteTable + " ADD COLUMN last_ip TEXT NULL;");
                alterStatement.close();
            }
            columns.close();

            LogDebug("Connected to SQLite database successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            throw new DBApiException("Failed setting up SQLite DB", e);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
                LogInfo("SQLite database connection closed successfully.");
            }
        } catch (SQLException e) {
            LogError("Error closing SQLite database connection", e);
        }
    }

    @Override
    public boolean isClosed() {
        return connection == null;
    }

    @Override
    public void registerUser(PlayerEntryV1 data) {
        LogDebug("Registering new player " + data.username + ": " + data.toJson());
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + config.sqlite.sqliteTable + " (username, username_lower, uuid, data, last_ip) VALUES (?, ?, ?, ?, ?);");
            statement.setString(1, data.username);
            statement.setString(2, data.usernameLowerCase);
            statement.setObject(3, data.uuid);
            statement.setString(4, data.toJson());
            statement.setString(5, data.lastIp);
            if (statement.executeUpdate() == 0) {
                LogError("Failed to register user " + data.username + ": " + data.toJson());
            }
            statement.close();
        } catch (SQLException e) {
            LogError("Error registering user: " + data.toJson(), e);
        }
    }

    @Override
    public @Nullable PlayerEntryV1 getUserData(String username) {
        try {
            PreparedStatement statement;
            if (extendedConfig.allowCaseInsensitiveUsername) {
                statement = connection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.sqlite.sqliteTable + " WHERE username = ?;");
                statement.setString(1, username);
            } else {
                statement = connection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.sqlite.sqliteTable + " WHERE username_lower = ?;");
                statement.setString(1, username.toLowerCase(Locale.ENGLISH));
            }
            ResultSet resultSet = statement.executeQuery();
            PlayerEntryV1 playerEntry = null;

            if (resultSet.next()) {
                playerEntry = new PlayerEntryV1(resultSet.getString("username"),
                                                resultSet.getString("username_lower"),
                                                resultSet.getString("uuid"),
                                                resultSet.getString("data"));
            }
            while (resultSet.next()) {
                String dbUsername = resultSet.getString("username");
                if (dbUsername.equals(username)) {
                    playerEntry = new PlayerEntryV1(dbUsername,
                                                    resultSet.getString("username_lower"),
                                                    resultSet.getString("uuid"),
                                                    resultSet.getString("data"));
                    break;
                }
            }

            resultSet.close();
            statement.close();
            LogDebug("Retrieved player data for " + username + ": " + (playerEntry != null ? playerEntry.toJson() : "null"));
            return playerEntry;
        } catch (SQLException e) {
            LogError("Error checking user registration", e);
        }
        return null;
    }

    public @NotNull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 playerEntry = getUserData(username);
        if (playerEntry == null) {
            playerEntry = new PlayerEntryV1(username);
            registerUser(playerEntry);
        }
        return playerEntry;
    }

    @Override
    public boolean deleteUserData(String username) {
        LogDebug("Deleting player data for " + username);
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM " + config.sqlite.sqliteTable + " WHERE username = ?;");
            statement.setString(1, username);
            int rowsAffected = statement.executeUpdate();
            statement.close();
            if (rowsAffected == 0) {
                LogError("Failed to delete user " + username);
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogError("Error deleting user data", e);
            return false;
        }
    }

    @Override
    public boolean updateUserData(PlayerEntryV1 data) {
        LogDebug("Updating player data for " + data.username + ": " + data.toJson());
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE " + config.sqlite.sqliteTable + " SET uuid = ?, data = ?, last_ip = ? WHERE username = ?;");
            statement.setObject(1, data.uuid);
            statement.setString(2, data.toJson());
            statement.setString(3, data.lastIp);
            statement.setString(4, data.username);
            int rowsAffected = statement.executeUpdate();
            statement.close();
            if (rowsAffected == 0) {
                LogError("Failed to update user " + data.username + ": " + data.toJson());
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            LogError("Error updating user data: " + data.toJson(), e);
            return false;
        }
    }

    @Override
    public HashMap<String, PlayerEntryV1> getAllData() {
        HashMap<String, PlayerEntryV1> registeredPlayers = new HashMap<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + config.sqlite.sqliteTable + ";");
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String usernameLowerCase = resultSet.getString("username_lower");
                String uuid = resultSet.getString("uuid");
                String data = resultSet.getString("data");
                registeredPlayers.put(username, new PlayerEntryV1(username, usernameLowerCase, uuid, data));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            LogError("Error retrieving all data", e);
        }
        return registeredPlayers;
    }

    @Override
    public int countAccountsByIp(String ipAddress) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + config.sqlite.sqliteTable + " WHERE last_ip = ?;"
            );
            statement.setString(1, ipAddress);
            ResultSet resultSet = statement.executeQuery();
            int count = 0;
            if (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
            LogDebug("Counted " + count + " accounts for IP " + ipAddress);
            return count;
        } catch (SQLException e) {
            LogError("Error counting accounts by IP", e);
            return 0;
        }
    }

    @Override
    public List<String> getUsernamesByIp(String ipAddress) {
        List<String> usernames = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT username FROM " + config.sqlite.sqliteTable + " WHERE last_ip = ?;"
            );
            statement.setString(1, ipAddress);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                usernames.add(resultSet.getString("username"));
            }
            resultSet.close();
            statement.close();
            LogDebug("Found " + usernames.size() + " usernames for IP " + ipAddress);
        } catch (SQLException e) {
            LogError("Error getting usernames by IP", e);
        }
        return usernames;
    }

    @Override
    public @Nullable String getUsernameByUuid(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT username FROM " + config.sqlite.sqliteTable + " WHERE uuid = ? LIMIT 1;")) {
            statement.setString(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("username") : null;
            }
        } catch (SQLException e) {
            LogError("Error getting username by UUID", e);
            return null;
        }
    }

    @Override
    public void migrateFromV4() {
        LogInfo("Migrating IPs from JSON to column...");
        try {
            HashMap<String, PlayerEntryV1> allData = getAllData();
            PreparedStatement statement = connection.prepareStatement("UPDATE " + config.sqlite.sqliteTable + " SET last_ip = ? WHERE username = ?;");

            for (PlayerEntryV1 entry : allData.values()) {
                statement.setString(1, entry.lastIp);
                statement.setString(2, entry.username);
                statement.addBatch();
            }
            statement.executeBatch();
            statement.close();
            LogInfo("Migrated IPs successfully.");
        } catch (SQLException e) {
            LogError("Error migrating IPs", e);
        }
    }

}