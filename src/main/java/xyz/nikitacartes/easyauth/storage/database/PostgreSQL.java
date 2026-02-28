package xyz.nikitacartes.easyauth.storage.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;


public class PostgreSQL implements DbApi {
    private final StorageConfigV1 config;
    private Connection connection;

    public PostgreSQL(StorageConfigV1 config) {
        this.config = config;
    }

    @Override
    public void connect() throws DBApiException {
        try {
            LogDebug("You are using PostgreSQL DB");
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + config.postgresql.pgHost + "/" + config.postgresql.pgDatabase;
            connection = DriverManager.getConnection(url, config.postgresql.pgUser, config.postgresql.pgPassword);

            Statement statement = connection.createStatement();
            statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id SERIAL PRIMARY KEY,
                        username VARCHAR(255) UNIQUE NOT NULL,
                        username_lower VARCHAR(255) NOT NULL,
                        uuid VARCHAR(255),
                        last_ip VARCHAR(45),
                        data JSONB NOT NULL
                    );
                    """, config.postgresql.pgTable));
            statement.close();
        } catch (ClassNotFoundException | SQLException e) {
            connection = null;
            throw new DBApiException("Failed setting up PostgreSQL DB", e);
        }
    }

    private void reconnect() {
        try {
            if (connection == null || !connection.isValid(5)) {
                LogDebug("Reconnecting to PostgreSQL");
                if (connection != null) {
                    connection.close();
                }
                connect();
            }
        } catch (DBApiException | SQLException e) {
            LogError("PostgreSQL reconnect failed", e);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
                LogInfo("PostgreSQL database connection closed successfully.");
            }
        } catch (SQLException e) {
            LogError("Error closing PostgreSQL connection", e);
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
            reconnect();
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO " + config.postgresql.pgTable + " (username, username_lower, uuid, data, last_ip) VALUES (?, ?, ?, ?::jsonb, ?);");
            stmt.setString(1, data.username);
            stmt.setString(2, data.usernameLowerCase);
            stmt.setString(3, data.uuid == null ? null : data.uuid.toString());
            stmt.setString(4, data.toJson());
            stmt.setString(5, data.lastIp);
            if (stmt.executeUpdate() == 0) {
                LogError("Failed to register user " + data.username + ": " + data.toJson());
            }
            stmt.close();
        } catch (SQLException e) {
            LogError("Register error: " + data.toJson(), e);
        }
    }

    @Override
    public @Nullable PlayerEntryV1 getUserData(String username) {
        try {
            reconnect();
            PreparedStatement statement;
            if (extendedConfig.allowCaseInsensitiveUsername) {
                statement = connection.prepareStatement(
                        "SELECT username, username_lower, uuid, data FROM " + config.postgresql.pgTable + " WHERE username = ?;");
                statement.setString(1, username);
            } else {
                statement = connection.prepareStatement(
                        "SELECT username, username_lower, uuid, data FROM " + config.postgresql.pgTable + " WHERE username_lower = ?;");
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
            LogError("Error checking user in PostgreSQL DB", e);
        }
        return null;
    }

    @Override
    public @NotNull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 data = getUserData(username);
        if (data == null) {
            data = new PlayerEntryV1(username);
            registerUser(data);
        }
        return data;
    }

    @Override
    public boolean deleteUserData(String username) {
        LogDebug("Deleting player data for " + username);
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM " + config.postgresql.pgTable + " WHERE username = ?;");
            stmt.setString(1, username);
            int deletedRows = stmt.executeUpdate();
            stmt.close();
            if (deletedRows == 0) {
                LogError("Failed to delete user " + username);
            }
            return deletedRows > 0;
        } catch (SQLException e) {
            LogError("Error deleting user data from PostgreSQL DB", e);
            return false;
        }
    }

    @Override
    public boolean updateUserData(PlayerEntryV1 data) {
        LogDebug("Updating player data for " + data.username + ": " + data.toJson());
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE " + config.postgresql.pgTable + " SET uuid = ?, data = ?::jsonb, last_ip = ? WHERE username = ?;");
            stmt.setString(1, data.uuid == null ? null : data.uuid.toString());
            stmt.setString(2, data.toJson());
            stmt.setString(3, data.lastIp);
            stmt.setString(4, data.username);
            int updatedRows = stmt.executeUpdate();
            stmt.close();
            if (updatedRows == 0) {
                LogError("Failed to update user " + data.username + ": " + data.toJson());
            }
            return updatedRows > 0;
        } catch (SQLException e) {
            LogError("Error updating user data in PostgreSQL DB", e);
            return false;
        }
    }

    @Override
    public HashMap<String, PlayerEntryV1> getAllData() {
        HashMap<String, PlayerEntryV1> players = new HashMap<>();
        try {
            reconnect();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + config.postgresql.pgTable + ";");
            while (rs.next()) {
                String username = rs.getString("username");
                if (username == null) continue;
                String usernameLowerCase = rs.getString("username_lower");
                String uuid = rs.getString("uuid");
                String data = rs.getString("data");
                players.put(username, new PlayerEntryV1(username, usernameLowerCase, uuid, data));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LogError("Error getting all data from PostgreSQL DB", e);
        }
        return players;
    }

    @Override
    public int countAccountsByIp(String ipAddress) {
        try {
            reconnect();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM " + config.postgresql.pgTable + " WHERE last_ip = ?;"
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
            reconnect();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT username FROM " + config.postgresql.pgTable + " WHERE last_ip = ?;"
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
    public void migrateFromV1(HashMap<String, String> userCache) {
        throw new UnsupportedOperationException("PostgreSQL does not support migrateFromV1");
    }

    @Override
    public void migrateFromV4() {
        throw new UnsupportedOperationException("PostgreSQL does not support migrateFromV4");
    }
}
