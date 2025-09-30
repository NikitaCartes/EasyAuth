package xyz.nikitacartes.easyauth.storage.database;

import net.minecraft.util.Uuids;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.*;
import java.util.HashMap;
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
                        data JSONB NOT NULL
                    );
                    """, config.postgresql.pgTable));
            statement.close();
        } catch (ClassNotFoundException | SQLException e) {
            throw new DBApiException("Failed setting up PostgreSQL DB", e);
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

    private void reconnect() {
        try {
            if (connection == null || !connection.isValid(5)) {
                LogDebug("Reconnecting to PostgreSQL");
                if (connection != null) connection.close();
                connect();
            }
        } catch (Exception e) {
            LogError("PostgreSQL reconnect failed", e);
        }
    }

    @Override
    public void registerUser(PlayerEntryV1 data) {
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + config.postgresql.pgTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?::jsonb);");
            stmt.setString(1, data.username);
            stmt.setString(2, data.usernameLowerCase);
            stmt.setString(3, data.uuid == null ? null : data.uuid.toString());
            stmt.setString(4, data.toJson());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            LogError("Register error: " + data, e);
        }
    }

    @Override
    public @Nullable PlayerEntryV1 getUserData(String username) {
        try {
            reconnect();
            PreparedStatement stmt;
            if (extendedConfig.allowCaseInsensitiveUsername) {
                stmt = connection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.postgresql.pgTable + " WHERE username = ?;");
                stmt.setString(1, username);
            } else {
                stmt = connection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.postgresql.pgTable + " WHERE username_lower = ?;");
                stmt.setString(1, username.toLowerCase(Locale.ENGLISH));
            }

            ResultSet rs = stmt.executeQuery();
            PlayerEntryV1 playerEntry = null;
            if (rs.next()) {
                playerEntry = new PlayerEntryV1(rs.getString("username"),
                        rs.getString("username_lower"),
                        rs.getString("uuid"),
                        rs.getString("data"));
            }
            while (rs.next()) {
                if (rs.getString("username").equals(username)) {
                    playerEntry = new PlayerEntryV1(rs.getString("username"),
                            rs.getString("username_lower"),
                            rs.getString("uuid"),
                            rs.getString("data"));
                    break;
                }
            }
            rs.close();
            stmt.close();
            return playerEntry;
        } catch (SQLException e) {
            LogError("Error checking user in PostgreSQL DB", e);
        }
        return null;
    }

    @Override
    public @Nonnull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 data = getUserData(username);
        if (data == null) {
            data = new PlayerEntryV1(username);
            registerUser(data);
        }
        return data;
    }

    @Override
    public void deleteUserData(String username) {
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + config.postgresql.pgTable + " WHERE username = ?;");
            stmt.setString(1, username);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            LogError("Error deleting user data from PostgreSQL DB", e);
        }
    }

    @Override
    public void updateUserData(PlayerEntryV1 data) {
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement("UPDATE " + config.postgresql.pgTable + " SET uuid = ?, data = ?::jsonb WHERE username = ?;");
            stmt.setString(1, data.uuid == null ? null : data.uuid.toString());
            stmt.setString(2, data.toJson());
            stmt.setString(3, data.username);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            LogError("Error updating user data in PostgreSQL DB", e);
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
                players.put(username, new PlayerEntryV1(
                        username,
                        rs.getString("username_lower"),
                        rs.getString("uuid"),
                        rs.getString("data")
                ));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LogError("Error getting all data from PostgreSQL DB", e);
        }
        return players;
    }

    @Override
    public void migrateFromV1(HashMap<String, String> userCache) {
        try {
            reconnect();
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO " + config.postgresql.pgTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?::jsonb) ON CONFLICT (username) DO UPDATE SET data = EXCLUDED.data;");
            userCache.forEach((username, uuid) -> {
                try {
                    PreparedStatement q = connection.prepareStatement("SELECT data FROM " + config.postgresql.pgTable + " WHERE uuid = ?;");
                    q.setString(1, uuid);
                    ResultSet rs = q.executeQuery();

                    String data = null;
                    if (rs.next()) {
                        data = rs.getString("data");
                    } else {
                        String lowerUsername = username.toLowerCase(Locale.ENGLISH);
                        String fallbackUuid = Uuids.getOfflinePlayerUuid(lowerUsername).toString();
                        q.setString(1, fallbackUuid);
                        rs = q.executeQuery();
                        if (rs.next()) {
                            data = rs.getString("data");
                        }
                    }

                    rs.close();
                    q.close();

                    if (data != null) {
                        PlayerEntryV1 entry = migrateFromV1(data, username);
                        stmt.setString(1, entry.username);
                        stmt.setString(2, entry.usernameLowerCase);
                        stmt.setString(3, entry.uuid == null ? null : entry.uuid.toString());
                        stmt.setString(4, entry.toJson());
                        stmt.addBatch();
                    }
                } catch (SQLException e) {
                    LogError("Error migrating player " + username + " in PostgreSQL DB", e);
                }
            });
            stmt.executeBatch();
            stmt.close();
        } catch (SQLException e) {
            LogError("Error running migrateFromV1 in PostgreSQL DB", e);
        }
    }
}
