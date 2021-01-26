package org.samo_lego.simpleauth;

import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.DBHelper;
import org.samo_lego.simpleauth.storage.PlayerCache;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class SimpleAuth {
    public static final String MOD_ID = "simpleauth";


    public static DBHelper DB = new DBHelper();

    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    /**
     * HashMap of players that have joined the server.
     * It's cleared on server stop in order to save some interactions with database during runtime.
     * Stores their data as {@link org.samo_lego.simpleauth.storage.PlayerCache PlayerCache} object.
     */
    public static final HashMap<String, PlayerCache> playerCacheMap = new HashMap<>();

    /**
     * HashSet of player names that have Mojang accounts.
     * If player is saved in here, they will be treated as online-mode ones.
     */
    public static final HashSet<String> mojangAccountNamesCache = new HashSet<>();

    // Getting game directory
    public static Path gameDirectory;

    // Server properties
    public static final Properties serverProp = new Properties();

    /**
     * Config of the SimpleAuth mod.
     */
    public static AuthConfig config;


    public static void init(Path gameDir) {
        gameDirectory = gameDir;
        logInfo("SimpleAuth mod by samo_lego.");
        // The support on discord was great! I really appreciate your help.
        logInfo("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

        try {
            serverProp.load(new FileReader(gameDirectory + "/server.properties"));
        } catch (IOException e) {
            logError("Error while reading server properties: " + e.getMessage());
        }

        // Creating data directory (database and config files are stored there)
        File file = new File(gameDirectory + "/mods/SimpleAuth/leveldbStore");
        if (!file.exists() && !file.mkdirs())
            throw new RuntimeException("[SimpleAuth] Error creating directory!");
        // Loading config
        config = AuthConfig.load(new File(gameDirectory + "/mods/SimpleAuth/config.json"));
        // Connecting to db
        DB.openConnection();
    }

    /**
     * Called on server stop.
     */
    public static void stop() {
        logInfo("Shutting down SimpleAuth.");
        DB.saveAll(playerCacheMap);

        // Closing threads
        try {
            THREADPOOL.shutdownNow();
            if (!THREADPOOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                Thread.currentThread().interrupt();
            }
        } catch (InterruptedException e) {
            logError(e.getMessage());
            THREADPOOL.shutdownNow();
        }

        // Closing DB connection
        DB.close();
    }
}
