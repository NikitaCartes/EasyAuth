package xyz.nikitacartes.easyauth.storage.database;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.config.deprecated.AuthConfig;
import xyz.nikitacartes.easyauth.storage.PlayerCache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;


public class LevelDB implements DbApi {
    private DB levelDBStore;
    private final StorageConfigV1 config;

    /**
     * Prepares connection to the LevelDB.
     */
    public LevelDB(StorageConfigV1 config) {
        this.config = config;
    }

    /**
     * Creates connection to the LevelDB.
     */
    public void connect() throws DBApiException {
        try {
            File file = new File(EasyAuth.gameDirectory + "/mods/EasyAuth/levelDBStore");
            if (!file.exists() && !file.mkdirs())
                throw new DBApiException("Error creating LevelDB directory", null);
            LogDebug("You are using LevelDB");
            Options options = new Options();
            levelDBStore = factory.open(new File(EasyAuth.gameDirectory + "/mods/" + (config.useSimpleAuthDb ? "SimpleAuth" : "EasyAuth") + "/levelDBStore"), options);
        } catch (IOException e) {
            throw new DBApiException("Failed setting up LevelDB", e);
        }
    }

    /**
     * Closes database connection.
     */
    public void close() {
        if (levelDBStore != null) {
            try {
                levelDBStore.close();
                levelDBStore = null;
                LogInfo("Database connection closed successfully");
            } catch (Error | IOException e) {
                LogError("Database connection not closed", e);
            }
        }
    }

    /**
     * Tells whether DbApi connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public boolean isClosed() {
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
    public boolean registerUser(String uuid, String data) {
        try {
            if (!isUserRegistered(uuid)) {
                levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
                return true;
            }
            return false;
        } catch (Error e) {
            LogError("Register error", e);
            return false;
        }
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public boolean isUserRegistered(String uuid) {
        try {
            return levelDBStore.get(bytes("UUID:" + uuid)) != null;
        } catch (DBException e) {
            LogError("isUserRegistered error", e);
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
            levelDBStore.delete(bytes("UUID:" + uuid));
        } catch (Error e) {
            LogError("deleteUserData error", e);
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
            levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
        } catch (Error e) {
            LogError("updateUserData error", e);
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
            if (isUserRegistered(uuid))  // Gets password from db and removes "data:" prefix from it
                return new String(levelDBStore.get(bytes("UUID:" + uuid))).substring(5);
        } catch (Error e) {
            LogError("getUserData error", e);
        }
        return "";
    }

    public void saveAll(HashMap<String, PlayerCache> playerCacheMap) {
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
            LogError("Error saving player data", e);
        }
    }
}
