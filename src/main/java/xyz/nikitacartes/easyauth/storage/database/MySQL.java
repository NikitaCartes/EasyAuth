package xyz.nikitacartes.easyauth.storage.database;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
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
    @Override
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
    @Override
    public boolean isClosed() {
        return MySQLConnection == null;
    }

    @Override
    public int executeRawUpdate(String sql) throws DBApiException {
        try {
            if (MySQLConnection == null || MySQLConnection.isClosed()) {
                connect();
            }
            
            try (Statement statement = MySQLConnection.createStatement()) {
                return statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            LogError("Error executing raw SQL update", e);
            throw new DBApiException("Error executing raw SQL update", e);
        }
    }
    
    @Override
    public Connection getConnection() throws DBApiException {
        try {
            if (MySQLConnection == null || MySQLConnection.isClosed()) {
                connect();
            }
            return MySQLConnection;
        } catch (SQLException e) {
            LogError("Error getting database connection", e);
            throw new DBApiException("Error getting database connection", e);
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "mysql";
    }

    /**
     * Inserts the data for the player.
     *
     * @param data data to put inside database
     */
    @Override
    public void registerUser(PlayerEntryV1 data) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO  " + config.mysql.mysqlTable + " (username, username_lower, uuid, data) VALUES (?, ?, ?, ?);");
            preparedStatement.setString(1, data.username);
            preparedStatement.setString(2, data.usernameLowerCase);
            preparedStatement.setString(3, data.uuid == null ? null : data.uuid.toString());
            preparedStatement.setString(4, data.toJson());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            LogError("Register error: " + data, e);
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
            return playerEntry;
        } catch (SQLException e) {
            LogError("Error checking user registration in MySQL database", e);
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

    /**
     * Deletes data for the provided username.
     *
     * @param username username of player to delete data for
     */
    public void deleteUserData(String username) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("DELETE FROM " + config.mysql.mysqlTable + " WHERE username = ?;");
            preparedStatement.setString(1, username);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            LogError("deleteUserData error", e);
        }
    }

    /**
     * Updates player's data.
     *
     * @param data data of the player to update data for
     */
    @Override
    public void updateUserData(PlayerEntryV1 data) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement(
                "UPDATE " + config.mysql.mysqlTable + 
                " SET username = ?, username_lower = ?, uuid = ?, data = ? " +
                "WHERE username = ?;"
            );
            preparedStatement.setString(1, data.username);
            preparedStatement.setString(2, data.usernameLowerCase);
            preparedStatement.setString(3, data.uuid == null ? null : data.uuid.toString());
            preparedStatement.setString(4, data.toJson());
            preparedStatement.setString(5, data.username);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            LogError("updateUserData error: " + data, e);
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
            LogError("Error retrieving all data from MySQL database", e);
        }
        return registeredPlayers;
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
}