package xyz.nikitacartes.easyauth.storage.database;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import net.minecraft.util.Uuids;
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


public class MySQL implements DbApi {
    private final StorageConfigV1 config;
    private Connection MySQLConnection;

    /**
     * Connects to the MySQL.
     */
    public MySQL(StorageConfigV1 config) {
        this.config = config;
    }

    public void connect() throws DBApiException {
        try {
            LogDebug("You are using MySQL DB");
            Class.forName("com.mysql.cj.jdbc.Driver");
            String uri = "jdbc:mysql://" + config.mysql.mysqlHost + "/" + config.mysql.mysqlDatabase + "?autoReconnect=true";
            LogDebug(String.format("connecting to %s", uri));
            MySQLConnection = DriverManager.getConnection(uri, config.mysql.mysqlUser, config.mysql.mysqlPassword);
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?;");
            preparedStatement.setString(1, config.mysql.mysqlTable);
            if (!preparedStatement.executeQuery().next()) {
                Statement createTableStatement = MySQLConnection.createStatement();
                createTableStatement.executeUpdate(
                        String.format("""
                                        CREATE TABLE `%s`.`%s` (
                                            `id` INT NOT NULL AUTO_INCREMENT,
                                            `username` VARCHAR(255) NOT NULL,
                                            `username_lower` VARCHAR(255) NOT NULL,
                                            `uuid` VARCHAR(255) NULL,
                                            `data` JSON NOT NULL,
                                            `last_ip` VARCHAR(45) NULL,
                                            PRIMARY KEY (`id`), UNIQUE (`username`)
                                        ) ENGINE = InnoDB;""",
                                config.mysql.mysqlDatabase,
                                config.mysql.mysqlTable
                        )
                );
                createTableStatement.close();
            } else {
                // Check if the 'username' column exists. If not, add new columns
                DatabaseMetaData metaData = MySQLConnection.getMetaData();
                ResultSet columns = metaData.getColumns(null, null, config.mysql.mysqlTable, "username");
                if (!columns.next()) {
                    try (Statement alterTableStatement = MySQLConnection.createStatement()) {
                        alterTableStatement.executeUpdate(String.format("ALTER TABLE `%s`.`%s` ADD COLUMN `username` VARCHAR(255) NULL;", config.mysql.mysqlDatabase, config.mysql.mysqlTable));
                        LogDebug("Added column 'username' to the existing table.");
                        alterTableStatement.executeUpdate(String.format("ALTER TABLE `%s`.`%s` ADD COLUMN `username_lower` VARCHAR(255) NULL;", config.mysql.mysqlDatabase, config.mysql.mysqlTable));
                        LogDebug("Added column 'username_lower' to the existing table.");
                        alterTableStatement.executeUpdate(String.format("ALTER TABLE `%s`.`%s` DROP INDEX `uuid`;", config.mysql.mysqlDatabase, config.mysql.mysqlTable));
                        LogDebug("Dropped index 'uuid'.");
                        alterTableStatement.executeUpdate(String.format("ALTER TABLE `%s`.`%s` MODIFY COLUMN `uuid` VARCHAR(255) NULL;", config.mysql.mysqlDatabase, config.mysql.mysqlTable));
                        LogDebug("Changed column 'uuid' to nullable.");
                    } catch (SQLException e) {
                        MySQLConnection = null;
                        throw new DBApiException("Error adding username, username_lower columns or changing uuid column", e);
                    }
                }
                columns.close();
                // Check if 'last_ip' column exists
                columns = metaData.getColumns(null, null, config.mysql.mysqlTable, "last_ip");
                if (!columns.next()) {
                    try (Statement alterTableStatement = MySQLConnection.createStatement()) {
                        alterTableStatement.executeUpdate(String.format("ALTER TABLE `%s`.`%s` ADD COLUMN `last_ip` VARCHAR(45) NULL;", config.mysql.mysqlDatabase, config.mysql.mysqlTable));
                        LogDebug("Added column 'last_ip' to the existing table.");
                    } catch (SQLException e) {
                        MySQLConnection = null;
                        throw new DBApiException("Error adding last_ip column", e);
                    }
                }
                columns.close();
            }
            preparedStatement.close();
        } catch (ClassNotFoundException | SQLException e) {
            MySQLConnection = null;
            throw new DBApiException("Failed setting up mysql DB", e);
        }
    }

    private void reConnect() {
        try {
            if (MySQLConnection == null || !MySQLConnection.isValid(5)) {
                LogDebug("Reconnecting to MySQL");
                if (MySQLConnection != null) {
                    MySQLConnection.close();
                }
                connect();
            }
        } catch (DBApiException | SQLException e) {
            LogError("Mysql reconnect failed", e);
        }
    }

    /**
     * Closes database connection.
     */
    public void close() {
        try {
            if (MySQLConnection != null) {
                MySQLConnection.close();
                MySQLConnection = null;
                LogInfo("Database connection closed successfully.");
            }
        } catch (CommunicationsException e) {
            LogError("Can't connect to database while closing", e);
        } catch (SQLException e) {
            LogError("Database connection not closed", e);
        }
    }

    /**
     * Tells whether DbApi connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public boolean isClosed() {
        return MySQLConnection == null;
    }


    /**
     * Inserts the data for the player.
     *
     * @param data data to put inside database
     */
    @Override
    public void registerUser(PlayerEntryV1 data) {
        LogDebug("Registering new player " + data.username + ": " + data.toJson());
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO  " + config.mysql.mysqlTable + " (username, username_lower, uuid, data, last_ip) VALUES (?, ?, ?, ?, ?);");
            preparedStatement.setString(1, data.username);
            preparedStatement.setString(2, data.usernameLowerCase);
            preparedStatement.setString(3, data.uuid == null ? null : data.uuid.toString());
            preparedStatement.setString(4, data.toJson());
            preparedStatement.setString(5, data.lastIp);
            if (preparedStatement.executeUpdate() == 0) {
                LogError("Failed to register user " + data.username + ": " + data.toJson());
            }
            preparedStatement.close();
        } catch (SQLException e) {
            LogError("Register error: " + data.toJson(), e);
        }
    }

    /**
     * Gets data for the provided username.
     *
     * @param username username of the player to get data for
     * @return data if player is registered, otherwise empty PlayerEntryV1
     */
    public @Nullable PlayerEntryV1 getUserData(String username) {
        try {
            reConnect();
            PreparedStatement statement;
            if (extendedConfig.allowCaseInsensitiveUsername) {
                statement = MySQLConnection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.mysql.mysqlTable + " WHERE username = ?;");
                statement.setString(1, username);
            } else {
                statement = MySQLConnection.prepareStatement("SELECT username, username_lower, uuid, data FROM " + config.mysql.mysqlTable + " WHERE username_lower = ?;");
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

    /**
     * Deletes data for the provided username.
     *
     * @param username username of player to delete data for
     */
    public boolean deleteUserData(String username) {
        LogDebug("Deleting player data for " + username);
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("DELETE FROM " + config.mysql.mysqlTable + " WHERE username = ?;");
            preparedStatement.setString(1, username);
            int deletedRows = preparedStatement.executeUpdate();
            preparedStatement.close();
            if (deletedRows == 0) {
                LogError("Failed to delete user " + username);
            }
            return deletedRows > 0;
        } catch (SQLException e) {
            LogError("deleteUserData error", e);
            return false;
        }
    }

    /**
     * Updates player's data.
     *
     * @param data data of the player to update data for
     */
    public boolean updateUserData(PlayerEntryV1 data) {
        LogDebug("Updating player data for " + data.username + ": " + data.toJson());
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("UPDATE " + config.mysql.mysqlTable + " SET uuid = ?, data = ?, last_ip = ? WHERE username = ?;");
            preparedStatement.setString(1, data.uuid == null ? null : data.uuid.toString());
            preparedStatement.setString(2, data.toJson());
            preparedStatement.setString(3, data.lastIp);
            preparedStatement.setString(4, data.username);
            int updatedRows = preparedStatement.executeUpdate();
            preparedStatement.close();
            if (updatedRows == 0) {
                LogError("Failed to update user " + data.username + ": " + data.toJson());
            }
            return updatedRows > 0;
        } catch (SQLException e) {
            LogError("updateUserData error: " + data.toJson(), e);
            return false;
        }
    }

    @Override
    public HashMap<String, PlayerEntryV1> getAllData() {
        HashMap<String, PlayerEntryV1> registeredPlayers = new HashMap<>();
        try {
            reConnect();
            Statement statement = MySQLConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + config.mysql.mysqlTable + ";");
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                if (username == null) continue;
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
            reConnect();
            PreparedStatement statement = MySQLConnection.prepareStatement(
                    "SELECT COUNT(*) FROM " + config.mysql.mysqlTable + " WHERE last_ip = ?;"
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
            reConnect();
            PreparedStatement statement = MySQLConnection.prepareStatement(
                    "SELECT username FROM " + config.mysql.mysqlTable + " WHERE last_ip = ?;"
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
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO " + config.mysql.mysqlTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE data = ?;");
            userCache.forEach((username, uuid) -> {
                try {
                    PreparedStatement statement = MySQLConnection.prepareStatement("SELECT data FROM " + config.mysql.mysqlTable + " WHERE uuid = ?;");
                    statement.setString(1, uuid);
                    ResultSet resultSet = statement.executeQuery();

                    String data = null;
                    if (resultSet.next()) {
                        data = resultSet.getString("data");
                    } else {
                        String lowerCaseUsername = username.toLowerCase(Locale.ENGLISH);
                        String lowerCaseUuid = Uuids.getOfflinePlayerUuid(lowerCaseUsername).toString();
                        statement.setString(1,lowerCaseUuid);
                        resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            data = resultSet.getString("data");
                        }
                    }
                    statement.close();
                    resultSet.close();

                    if (data != null) {
                        PlayerEntryV1 playerEntry = migrateFromV1(data, username);
                        preparedStatement.setString(1, playerEntry.username);
                        preparedStatement.setString(2, playerEntry.usernameLowerCase);
                        preparedStatement.setString(3, playerEntry.uuid == null ? null : playerEntry.uuid.toString());
                        preparedStatement.setString(4, playerEntry.toJson());
                        preparedStatement.setString(5, playerEntry.toJson());
                        preparedStatement.addBatch();
                    }
                } catch (SQLException e) {
                    LogError("Error migrating player " + username, e);
                }
            });
            preparedStatement.executeBatch();
            preparedStatement.close();
        } catch (SQLException e) {
            LogError("Error migrating players data", e);
        }
    }

    /**
     * Migrates IP addresses from JSON to column.
     */
    @Override
    public void migrateFromV4() {
        try {
            reConnect();
            Statement statement = MySQLConnection.createStatement();
            statement.executeUpdate("UPDATE " + config.mysql.mysqlTable + " SET last_ip = JSON_UNQUOTE(JSON_EXTRACT(data, '$.last_ip'));");
            statement.close();
            LogInfo("Migrated IPs successfully.");
        } catch (SQLException e) {
            LogError("Error migrating IPs using SQL", e);
            LogInfo("Falling back to Java migration...");
            // Fallback
            try {
                HashMap<String, PlayerEntryV1> allData = getAllData();
                PreparedStatement preparedStatement = MySQLConnection.prepareStatement("UPDATE " + config.mysql.mysqlTable + " SET last_ip = ? WHERE username = ?;");
                for (PlayerEntryV1 entry : allData.values()) {
                    preparedStatement.setString(1, entry.lastIp);
                    preparedStatement.setString(2, entry.username);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                preparedStatement.close();
                LogInfo("Migrated IPs successfully using Java fallback.");
            } catch (SQLException ex) {
                LogError("Error migrating IPs using Java fallback", ex);
            }
        }
    }
}