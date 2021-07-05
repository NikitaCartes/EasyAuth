package org.samo_lego.simpleauth.storage;

import org.samo_lego.simpleauth.storage.database.LevelDB;
import org.samo_lego.simpleauth.storage.database.MongoDB;

import java.util.HashMap;

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class DBHelper {

    /**
     * Connects to the DB.
     */
    public void openConnection() {
        if(config.main.useMongoDB)
            MongoDB.initialize();
        else
            LevelDB.initialize();
    }

    /**
     * Closes database connection.
     */
    public void close() {
        if(config.main.useMongoDB && MongoDB.close() || LevelDB.close())
            logInfo("Database connection closed successfully.");
    }

    /**
     * Tells whether DB connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public boolean isClosed() {
        return config.main.useMongoDB ? MongoDB.isClosed() : LevelDB.isClosed();
    }


    /**
     * Inserts the data for the player.
     *
     * @param uuid uuid of the player to insert data for
     * @param data data to put inside database
     * @return true if operation was successful, otherwise false
     */
    public boolean registerUser(String uuid, String data) {
        if(config.main.useMongoDB)
            //return MongoDB.registerUser(uuid, data);
            System.out.println("Not implemented yet.");
        return LevelDB.registerUser(uuid, data);
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public boolean isUserRegistered(String uuid) {
        return config.main.useMongoDB ? MongoDB.isUserRegistered(uuid) : LevelDB.isUserRegistered(uuid);
    }

    /**
     * Deletes data for the provided uuid.
     *
     * @param uuid uuid of player to delete data for
     */
    public void deleteUserData(String uuid) {
        if(config.main.useMongoDB)
            MongoDB.deleteUserData(uuid);
        else
            LevelDB.deleteUserData(uuid);
    }

    /**
     * Updates player's data.
     *
     * @param uuid uuid of the player to update data for
     * @param data data to put inside database
     */
    public void updateUserData(String uuid, String data) {
        if(config.main.useMongoDB)
            //MongoDB.updateUserData(uuid, data);
            System.out.println("Not implemented yet.");
        else
            LevelDB.updateUserData(uuid, data);
    }

    /**
     * Gets the hashed password from DB.
     *
     * @param uuid uuid of the player to get data for.
     * @return data as string if player has it, otherwise empty string.
     */
    public String getUserData(String uuid){
        return config.main.useMongoDB ? MongoDB.getUserData(uuid) : LevelDB.getUserData(uuid);
    }

    public void saveAll(HashMap<String, PlayerCache> playerCacheMap) {
        // Saving player data.
        if(config.main.useMongoDB)
            MongoDB.saveFromCache(playerCacheMap);
        else
            LevelDB.saveFromCache(playerCacheMap);
    }
}