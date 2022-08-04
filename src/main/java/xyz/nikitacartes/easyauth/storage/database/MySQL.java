package xyz.nikitacartes.easyauth.storage.database;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import xyz.nikitacartes.easyauth.storage.PlayerCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;


public class MySQL implements DbApi {
    private Connection MySQLConnection;

    /**
     * Connects to the MySQL.
     */
    public MySQL() {
        if (config.experimental.debugMode) {
            logInfo("You are using MySQL DB");
        }
        try {
            connect();
        } catch (SQLException | ClassNotFoundException e) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        MySQLConnection = DriverManager.getConnection("jdbc:mysql://" + config.main.MySQLHost + "/" + config.main.MySQLDatabase + "?autoReconnect=true", config.main.MySQLUser, config.main.MySQLPassword);
        PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?;");
        preparedStatement.setString(1, config.main.MySQLTableName);
        if (!preparedStatement.executeQuery().next()) {
            MySQLConnection.createStatement().executeUpdate("CREATE TABLE `" + config.main.MySQLDatabase + "`.`" + config.main.MySQLTableName + "` ( `id` INT NOT NULL AUTO_INCREMENT , `uuid` VARCHAR(36) NOT NULL , `data` JSON NOT NULL , PRIMARY KEY (`id`), UNIQUE (`uuid`)) ENGINE = InnoDB;");
        }
    }

    private void reConnect() {
        try {
            if (MySQLConnection == null) {
                logWarn("MySQL DB already closed or hasn't been open yet");
            } else if(!MySQLConnection.isValid(0)) {
                connect();
            }
        } catch (SQLException | ClassNotFoundException e) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes database connection.
     */
    public void close() {
        try {
            if (MySQLConnection != null) {
                MySQLConnection.close();
                logInfo("Database connection closed successfully.");
            }
        } catch (CommunicationsException e) {
            logError(e.getMessage());
            logWarn("Can't connect to database while closing");
        }
        catch (SQLException e) {
            logError(e.getMessage());
            e.printStackTrace();
            logWarn("Database connection not closed");
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
     * @param uuid uuid of the player to insert data for
     * @param data data to put inside database
     * @return true if operation was successful, otherwise false
     */
    @Deprecated
    public boolean registerUser(String uuid, String data) {
        try {
            reConnect();
            if (!isUserRegistered(uuid)) {
                PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES (?, ?);");
                preparedStatement.setString(1, uuid);
                preparedStatement.setString(2, data);
                preparedStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            logError("Register error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public boolean isUserRegistered(String uuid) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT * FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
            preparedStatement.setString(1, uuid);
            return preparedStatement.executeQuery().next();
        } catch (SQLException e) {
            logError(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes data for the provided uuid.
     *
     * @param uuid uuid of player to delete data for
     */
    public void deleteUserData(String uuid) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("DELETE FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
            preparedStatement.setString(1, uuid);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates player's data.
     *
     * @param uuid uuid of the player to update data for
     * @param data data to put inside database
     */
    @Deprecated
    public void updateUserData(String uuid, String data) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("UPDATE " + config.main.MySQLTableName + " SET data = ? WHERE uuid = ?;");
            preparedStatement.setString(1, data);
            preparedStatement.setString(1, uuid);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the hashed password from DbApi.
     *
     * @param uuid uuid of the player to get data for.
     * @return data as string if player has it, otherwise empty string.
     */
    public String getUserData(String uuid) {
        try {
            reConnect();
            if (isUserRegistered(uuid)) {
                PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT data FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
                preparedStatement.setString(1, uuid);
                ResultSet query = preparedStatement.executeQuery();
                query.next();
                return query.getString(1);
            }
        } catch (SQLException e) {
            logError("Error getting data: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public void saveAll(HashMap<String, PlayerCache> playerCacheMap) {
        try {
            reConnect();
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = ?;");
            // Updating player data.
            playerCacheMap.forEach((uuid, playerCache) -> {
                String data = playerCache.toJson();
                try {
                    preparedStatement.setString(1, uuid);
                    preparedStatement.setString(2, data);
                    preparedStatement.setString(3, data);

                    preparedStatement.addBatch();
                } catch (SQLException e) {
                    logError("Error saving player data! (" + uuid + ") " + e.getMessage());
                    e.printStackTrace();
                }
            });
            preparedStatement.executeBatch();
        } catch (SQLException | NullPointerException e) {
            logError("Error saving players data! " + e.getMessage());
            e.printStackTrace();
        }
    }
}
