package xyz.nikitacartes.easyauth.storage.database;

import net.minecraft.util.dynamic.DynamicSerializableUuid;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
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
                                data TEXT NOT NULL
                            );
                            """.formatted(config.sqlite.sqliteTable)
            );
            statement.close();

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
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + config.sqlite.sqliteTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?);");
            statement.setString(1, data.username);
            statement.setString(2, data.usernameLowerCase);
            statement.setObject(3, data.uuid);
            statement.setString(4, data.toJson());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            LogError("Error registering user in SQLite database: " + data, e);
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
            return playerEntry;
        } catch (SQLException e) {
            LogError("Error checking user registration in SQLite database", e);
        }
        return null;
    }

    public @Nonnull PlayerEntryV1 getUserDataOrCreate(String username) {
        PlayerEntryV1 playerEntry = getUserData(username);
        if (playerEntry == null) {
            playerEntry = new PlayerEntryV1(username);
            registerUser(playerEntry);
        }
        return playerEntry;
    }

    @Override
    public void deleteUserData(String username) {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM " + config.sqlite.sqliteTable + " WHERE username = ?;");
            statement.setString(1, username);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            LogError("Error deleting user data in SQLite database", e);
        }
    }

    @Override
    public void updateUserData(PlayerEntryV1 data) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE " + config.sqlite.sqliteTable + " SET uuid = ?, data = ? WHERE username = ?;");
            statement.setObject(1, data.uuid);
            statement.setString(2, data.toJson());
            statement.setString(3, data.username);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            LogError("Error updating user data in SQLite database: " + data, e);
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
            LogError("Error retrieving all data from SQLite database", e);
        }
        return registeredPlayers;
    }

    @Override
    public void migrateFromV1(HashMap<String, String> userCache) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + config.sqlite.sqliteTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?);");
            LevelDB levelDB = new LevelDB(EasyAuth.storageConfig);
            levelDB.connect();
            userCache.forEach((username, uuid) -> {
                try {
                    String data = levelDB.getPlayerCache0(uuid);
                    if (data == null) {
                        String lowerCaseUsername = username.toLowerCase(Locale.ENGLISH);
                        String lowerCaseUuid = DynamicSerializableUuid.getOfflinePlayerUuid(lowerCaseUsername).toString();
                        data = levelDB.getPlayerCache0(lowerCaseUuid);
                    }
                    if (data != null) {
                        PlayerEntryV1 playerEntry = migrateFromV1(data, username);
                        preparedStatement.setString(1, playerEntry.username);
                        preparedStatement.setString(2, playerEntry.usernameLowerCase);
                        preparedStatement.setObject(3, playerEntry.uuid);
                        preparedStatement.setString(4, playerEntry.toJson());
                        preparedStatement.addBatch();
                    }
                } catch (SQLException e) {
                    LogError("Error migrating players data", e);
                }
            });
            preparedStatement.executeBatch();
            preparedStatement.close();
            levelDB.close();
        } catch (SQLException e) {
            LogError("Error migrating players data", e);
        } catch (DBApiException e) {
            LogError("Error migrating players data", e);
            connection = null;
            throw new RuntimeException(e);
        }
    }
}