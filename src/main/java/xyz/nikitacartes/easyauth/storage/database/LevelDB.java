package xyz.nikitacartes.easyauth.storage.database;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.StorageConfigV1;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
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
            levelDBStore = factory.open(new File(EasyAuth.gameDirectory + "/mods/" + (config.useSimpleauthDb ? "SimpleAuth" : "EasyAuth") + "/levelDBStore"), options);
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

    public @Nonnull PlayerEntryV1 getUserDataOrCreate(String username) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public void deleteUserData(String uuid) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public void updateUserData(PlayerEntryV1 data) {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    public HashMap<String, PlayerEntryV1> getAllData() {
        throw new UnsupportedOperationException("LevelDB is not supported anymore");
    }

    @Override
    public void migrateFromV1(HashMap<String, String> userCache) {
        // LevelDB does not need any migration from v1
    }
    
    @Override
    public int executeRawUpdate(String sql) throws DBApiException {
        throw new DBApiException("LevelDB does not support SQL queries", null);
    }
    
    @Override
    public Connection getConnection() throws DBApiException {
        throw new DBApiException("LevelDB does not provide SQL connections", null);
    }
    
    @Override
    public String getDatabaseType() {
        return "leveldb";
    }
}