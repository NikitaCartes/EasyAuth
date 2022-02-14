package xyz.nikitacartes.easyauth.storage.database;

import xyz.nikitacartes.easyauth.storage.PlayerCache;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.logError;
import static xyz.nikitacartes.easyauth.EasyAuth.config;


public class MySQL {
    private static Connection MySQLConnection;

    /**
     * Connects to the MySQL.
     */
    public static void initialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            MySQLConnection = DriverManager.getConnection(config.main.MySQLConnectionString);
        } catch (SQLException | ClassNotFoundException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Closes database connection.
     */
    public static boolean close() {
        try {
            if (MySQLConnection.isClosed()) {
                MySQLConnection.close();
                return true;
            }
        } catch (SQLException e) {
            logError(e.getMessage());
        }

        return false;
    }

    /**
     * Tells whether DB connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public static boolean isClosed() {
        try {
            return MySQLConnection.isClosed();
        } catch (SQLException e) {
            logError(e.getMessage());
        }

        return true;
    }


    /**
     * Inserts the data for the player.
     *
     * @param uuid uuid of the player to insert data for
     * @param data data to put inside database
     * @return true if operation was successful, otherwise false
     */
    @Deprecated
    public static boolean registerUser(String uuid, String data) {
        try {
            if (!isUserRegistered(uuid)) {
                MySQLConnection.createStatement().executeUpdate("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES ('" + uuid + "', '" + data + "')");
                return true;
            }
        } catch (SQLException e) {
            logError("Register error: " + e.getMessage());
        }

        return false;
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public static boolean isUserRegistered(String uuid) {
        try {
            return MySQLConnection.createStatement().executeQuery("SELECT * FROM " + config.main.MySQLTableName + " WHERE uuid = '" + uuid + "'").next();
        } catch (SQLException e) {
            logError(e.getMessage());
        }

        return false;
    }

    /**
     * Deletes data for the provided uuid.
     *
     * @param uuid uuid of player to delete data for
     */
    public static void deleteUserData(String uuid) {
        try {
            MySQLConnection.createStatement().executeUpdate("DELETE FROM " + config.main.MySQLTableName + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Updates player's data.
     *
     * @param uuid uuid of the player to update data for
     * @param data data to put inside database
     */
    @Deprecated
    public static void updateUserData(String uuid, String data) {
        try {
            MySQLConnection.createStatement().executeUpdate("UPDATE " + config.main.MySQLTableName + " SET data = '" + data + "' WHERE uuid = '" + uuid + "'");
        } catch (SQLException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Gets the hashed password from DB.
     *
     * @param uuid uuid of the player to get data for.
     * @return data as string if player has it, otherwise empty string.
     */
    public static String getUserData(String uuid) {
        try {
            if (isUserRegistered(uuid)) { // Gets password from db and removes "data:" prefix from it
                ResultSet query = MySQLConnection.createStatement().executeQuery("SELECT data FROM " + config.main.MySQLTableName + " WHERE uuid = '" + uuid + "'");
                query.next();
                return query.getString(3);
            }
        } catch (SQLException e) {
            logError("Error getting data: " + e.getMessage());
        }
        return "";
    }

    public static void saveFromCache(HashMap<String, PlayerCache> playerCacheMap) {
        // Updating player data.
        playerCacheMap.forEach((uuid, playerCache) -> {
            String data = playerCache.toJson();
            try {
                if (MySQLConnection.createStatement().executeQuery("SELECT * FROM " + config.main.MySQLTableName + " WHERE uuid = '" + uuid + "'").next()) {
                    MySQLConnection.createStatement().executeUpdate("UPDATE " + config.main.MySQLTableName + " SET data = '" + data + "' WHERE uuid = '" + uuid + "'");
                } else {
                    MySQLConnection.createStatement().executeUpdate("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES ('" + uuid + "', '" + data + "')");
                }
            } catch (SQLException e) {
                logError("Error saving player data! " + e.getMessage());
            }
        });
    }
}
