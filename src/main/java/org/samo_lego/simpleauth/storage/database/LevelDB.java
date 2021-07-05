package org.samo_lego.simpleauth.storage.database;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.storage.PlayerCache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class LevelDB {
    private static DB levelDBStore;

    /**
     * Connects to the LevelDB.
     */
    public static void initialize() {
        Options options = new Options();
        try {
            levelDBStore = factory.open(new File(SimpleAuth.gameDirectory + "/mods/SimpleAuth/levelDBStore"), options);
        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Closes database connection.
     */
    public static boolean close() {
        if (levelDBStore != null) {
            try {
                levelDBStore.close();
                return true;
            } catch (Error | IOException e) {
                logError(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Tells whether DB connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public static boolean isClosed() {
        return levelDBStore == null;
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
            if(!isUserRegistered(uuid)) {
                levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
                return true;
            }
            return false;
        } catch (Error e) {
            logError("Register error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public static boolean isUserRegistered(String uuid) {
        try {
            return levelDBStore.get(bytes("UUID:" + uuid)) != null;
        } catch (DBException e) {
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
            levelDBStore.delete(bytes("UUID:" + uuid));
        } catch (Error e) {
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
            levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
        } catch (Error e) {
            logError(e.getMessage());
        }
    }

    /**
     * Gets the hashed password from DB.
     *
     * @param uuid uuid of the player to get data for.
     * @return data as string if player has it, otherwise empty string.
     */
    public static String getUserData(String uuid){
        try {
            if(isUserRegistered(uuid))  // Gets password from db and removes "data:" prefix from it
                return new String(levelDBStore.get(bytes("UUID:" + uuid))).substring(5);
        } catch (Error e) {
            logError("Error getting data: " + e.getMessage());
        }
        return "";
    }

    public static void saveFromCache(HashMap<String, PlayerCache> playerCacheMap) {
        WriteBatch batch = levelDBStore.createWriteBatch();
        // Updating player data.
        playerCacheMap.forEach((uuid, playerCache) -> {
            String data = playerCache.toJson();
            batch.put(bytes("UUID:" + uuid), bytes("data:" + data));
        });
        try {
            // Writing and closing batch
            levelDBStore.write(batch);
            batch.close();
        } catch (IOException e) {
            logError("Error saving player data! " + e.getMessage());
        }
    }
}
