package xyz.nikitacartes.easyauth.storage.database;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class LevelDB implements DbApi {
    private DB levelDBStore;

    /**
     * Prepares connection to the LevelDB.
     */
    public LevelDB() {
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
            File levelDbFolder = new File(EasyAuth.gameDirectory + "/mods/EasyAuth/levelDBStore");
            if (!levelDbFolder.exists()) {
                levelDbFolder = new File(EasyAuth.gameDirectory + "/mods/SimpleAuth/levelDBStore");
            }
            levelDBStore = factory.open(levelDbFolder, options);
        } catch (IOException e) {
            throw new DBApiException("Failed open up LevelDB", e);
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

    public String getPlayerCache0(String uuid) {
        try {
            byte[] data = levelDBStore.get(bytes("UUID:" + uuid));
            if (data != null) {
                return new String(data).substring(5);
            }
        } catch (Error e) {
            LogError("getUserData error", e);
        }
        return null;
    }

    public boolean isClosed() {
        return levelDBStore == null;
    }

    @Override
    public void registerUser(PlayerEntryV1 data) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public @Nullable PlayerEntryV1 getUserData(String username) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public @NotNull PlayerEntryV1 getUserDataOrCreate(String username) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public boolean deleteUserData(String uuid) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public boolean updateUserData(PlayerEntryV1 data) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public HashMap<String, PlayerEntryV1> getAllData() {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    @Override
    public int countAccountsByIp(String ipAddress) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    @Override
    public List<String> getUsernamesByIp(String ipAddress) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    @Override
    public void migrateFromV1(HashMap<String, String> userCache) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    @Override
    public void migrateFromV4() {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }
}