package xyz.nikitacartes.easyauth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.storage.AuthConfig;
import xyz.nikitacartes.easyauth.storage.DBHelper;
import xyz.nikitacartes.easyauth.storage.PlayerCache;

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

import static xyz.nikitacartes.easyauth.utils.EasyLogger.logInfo;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.logError;

public class EasyAuth implements ModInitializer {
    public static final String MOD_ID = "easyauth";


    public static DBHelper DB = new DBHelper();

    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();
    
    /**
     * HashMap of players that have joined the server.
     * It's cleared on server stop in order to save some interactions with database during runtime.
     * Stores their data as {@link PlayerCache PlayerCache} object.
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
     * Config of the EasyAuth mod.
     */
    public static AuthConfig config;


    public static void init(Path gameDir) {
        gameDirectory = gameDir;
        logInfo("EasyAuth mod by samo_lego, NikitaCartes.");
        // The support on discord was great! I really appreciate your help.
        // logInfo("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

        try {
            serverProp.load(new FileReader(gameDirectory + "/server.properties"));
        } catch (IOException e) {
            logError("Error while reading server properties: " + e.getMessage());
        }

        // Creating data directory (database and config files are stored there)
        File file = new File(gameDirectory + "/mods/EasyAuth/levelDBStore");
        if (!file.exists() && !file.mkdirs())
            throw new RuntimeException("[EasyAuth] Error creating directory!");
        // Loading config
        config = AuthConfig.load(new File(gameDirectory + "/mods/EasyAuth/config.json"));
        // Connecting to db
        DB.openConnection();
    }

    /**
     * Called on server stop.
     */
    public static void stop() {
        logInfo("Shutting down EasyAuth.");
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

    @Override
    public void onInitialize() {
        EasyAuth.init(FabricLoader.getInstance().getGameDir());

        // Registering the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            RegisterCommand.registerCommand(dispatcher);
            LoginCommand.registerCommand(dispatcher);
            LogoutCommand.registerCommand(dispatcher);
            AuthCommand.registerCommand(dispatcher);
            AccountCommand.registerCommand(dispatcher);
        });

        // From Fabric API
        PlayerBlockBreakEvents.BEFORE.register((world, player, blockPos, blockState, blockEntity) -> AuthEventHandler.onBreakBlock(player));
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
        UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
        UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> AuthCommand.reloadConfig(null));
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopServer);
    }

    private void onStopServer(MinecraftServer server) {
        EasyAuth.stop();
    }
}
