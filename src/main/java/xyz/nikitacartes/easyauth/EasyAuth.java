package xyz.nikitacartes.easyauth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.config.*;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.storage.database.*;

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
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class EasyAuth implements ModInitializer {
    public static DbApi DB = null;

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

    public static MainConfigV1 config;
    public static ExtendedConfigV1 extendedConfig;
    public static LangConfigV1 langConfig;
    public static TechnicalConfigV1 technicalConfig;
    public static StorageConfigV1 storageConfig;


    @Override
    public void onInitialize() {
        gameDirectory = FabricLoader.getInstance().getGameDir();
        LogInfo("EasyAuth mod by NikitaCartes");

        try {
            serverProp.load(new FileReader(gameDirectory + "/server.properties"));
            if (Boolean.parseBoolean(serverProp.getProperty("enforce-secure-profile"))) {
                LogWarn("Disable enforce-secure-profile to allow offline players to join the server");
                LogWarn("For more info, see https://github.com/NikitaCartes/EasyAuth/issues/68");
            }
        } catch (IOException e) {
            LogError("Error while reading server properties: ", e);
        }

        File file = new File(gameDirectory + "/config/EasyAuth");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("[EasyAuth] Error creating directory for configs");
            }
            ConfigMigration.migrateFromV0();
        }

        loadConfigs();

        if (DB != null && !DB.isClosed()) {
            DB.close();
        }

        if (EasyAuth.storageConfig.databaseType.equalsIgnoreCase("mysql")) {
            DB = new MySQL(EasyAuth.storageConfig);
        } else if (EasyAuth.storageConfig.databaseType.equalsIgnoreCase("mongodb")) {
            DB = new MongoDB(EasyAuth.storageConfig);
        } else {
            DB = new LevelDB(EasyAuth.storageConfig);
        }
        try {
            DB.connect();
        } catch (DBApiException e) {
            LogError("onInitialize error: ", e);
        }

        // Registering the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
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
        ServerLifecycleEvents.SERVER_STARTED.register(this::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopServer);

        Identifier earlyPhase = new Identifier(MOD_ID, "early");
        ServerLoginConnectionEvents.QUERY_START.addPhaseOrdering(earlyPhase, Event.DEFAULT_PHASE);
        ServerLoginConnectionEvents.QUERY_START.register(earlyPhase, AuthEventHandler::onPreLogin);
    }

    private void onStartServer(MinecraftServer server) {
        if (DB.isClosed()) {
            LogError("Couldn't connect to database. Stopping server");
            server.stop(false);
        }
    }

    private void onStopServer(MinecraftServer server) {
        LogInfo("Shutting down EasyAuth.");
        DB.saveAll(playerCacheMap);

        // Closing threads
        try {
            THREADPOOL.shutdownNow();
            if (!THREADPOOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                Thread.currentThread().interrupt();
            }
        } catch (InterruptedException e) {
            LogError("Error on stop", e);
            THREADPOOL.shutdownNow();
        }

        // Closing DbApi connection
        DB.close();
    }

    public static void loadConfigs() {
        VersionConfig version = VersionConfig.load();

        switch (version.configVersion) {
            case -1:
            case 1: {
                EasyAuth.config = MainConfigV1.load();
                EasyAuth.technicalConfig = TechnicalConfigV1.load();
                EasyAuth.langConfig = LangConfigV1.load();
                EasyAuth.extendedConfig = ExtendedConfigV1.load();
                EasyAuth.storageConfig = StorageConfigV1.load();
                break;
            }
            default: {
                LogError("Unknown config version: " + version.configVersion + "\n Using last known version");
                EasyAuth.config = MainConfigV1.load();
                EasyAuth.technicalConfig = TechnicalConfigV1.load();
                EasyAuth.langConfig = LangConfigV1.load();
                EasyAuth.extendedConfig = ExtendedConfigV1.load();
                EasyAuth.storageConfig = StorageConfigV1.load();
                break;
            }
        }
        AuthEventHandler.usernamePattern = Pattern.compile(EasyAuth.extendedConfig.usernameRegexp);
    }

    public static void saveConfigs() {
        EasyAuth.config.save();
        EasyAuth.technicalConfig.save();
        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.storageConfig.save();
    }
}
