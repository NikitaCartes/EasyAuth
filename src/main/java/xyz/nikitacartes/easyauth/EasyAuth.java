package xyz.nikitacartes.easyauth;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.config.*;
import xyz.nikitacartes.easyauth.storage.database.*;
import xyz.nikitacartes.easyauth.integrations.LuckPermsIntegration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static xyz.nikitacartes.easyauth.config.ConfigMigration.*;
import static xyz.nikitacartes.easyauth.config.MainConfigV1.CURRENT_CONFIG_VERSION;
import static xyz.nikitacartes.easyauth.config.StorageConfigV1.getDbApi;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class EasyAuth {
    public static DbApi DB = null;

    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    // Getting game directory
    public static Path gameDirectory;

    // Server properties
    public static final Properties serverProp = new Properties();

    public static MainConfigV1 config;
    public static ExtendedConfigV1 extendedConfig;
    public static LangConfigV1 langConfig;
    public static TechnicalConfigV1 technicalConfig;
    public static StorageConfigV1 storageConfig;


    public static void loadDatabase() {
        DB = getDbApi();
        try {
            DB.connect();
        } catch (DBApiException e) {
            LogError("Error while set up database connection", e);
        }
    }

    public static void migrateConfigs() {
        File file = new File(gameDirectory + "/config/EasyAuth");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("[EasyAuth] Error creating directory for configs");
            }
            ConfigMigration.migrateFromV0();
        }
    }

    static void onStartServer(MinecraftServer server) {
        try {
            serverProp.load(new FileReader(gameDirectory + "/server.properties"));
            if (Boolean.parseBoolean(serverProp.getProperty("enforce-secure-profile"))) {
                LogWarn("Disable enforce-secure-profile to allow offline players to join the server");
                LogWarn("For more info, see https://github.com/NikitaCartes/EasyAuth/issues/68");
            }
        } catch (IOException e) {
            LogError("Error while reading server properties: ", e);
        }
        if (DB.isClosed()) {
            LogError("Couldn't connect to database. Stopping server");
            server.stop(false);
        }

        // Register LuckPerms integration if it's loaded
        if (technicalConfig.luckPermsLoaded) {
            LuckPermsIntegration.register();
        }
    }

    static void onStopServer(MinecraftServer server) {
        LogInfo("Shutting down EasyAuth.");

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
        int configVersion = VersionConfig.load().configVersion;

        if (configVersion == -1) {
            // Fresh install - create default configs
            EasyAuth.config = MainConfigV1.create();
            EasyAuth.technicalConfig = TechnicalConfigV1.create();
            EasyAuth.langConfig = LangConfigV1.create();
            EasyAuth.extendedConfig = ExtendedConfigV1.create();
            EasyAuth.storageConfig = StorageConfigV1.create();
            return;
        }

        if (configVersion > CURRENT_CONFIG_VERSION) {
            LogError("Unknown config version: " + configVersion + "\n Using last known version");
        }

        // Load existing configs
        EasyAuth.config = MainConfigV1.load();
        EasyAuth.technicalConfig = TechnicalConfigV1.load();
        EasyAuth.langConfig = LangConfigV1.load();
        EasyAuth.extendedConfig = ExtendedConfigV1.load();
        EasyAuth.storageConfig = StorageConfigV1.load();

        configMigration(configVersion);
    }

    public static void saveConfigs() {
        EasyAuth.config.save();
        EasyAuth.technicalConfig.save();
        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.storageConfig.save();
    }

    public static void reloadConfigs(MinecraftServer server) {
        DB.close();

        boolean regAlias = extendedConfig.aliases.register;
        boolean loginAlias = extendedConfig.aliases.login;

        EasyAuth.loadConfigs();

        try {
            DB.connect();
        } catch (DBApiException e) {
            LogError("onInitialize error: ", e);
        }

        CommandManager serverCommandManager = server.getCommandManager();
        try {
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);

            // noinspection unchecked
            Map<String, ?> literals = (Map<String, ?>) literalsField.get(serverCommandManager.getDispatcher().getRoot());
            literals.remove("register");
            literals.remove("login");
            if (regAlias) {
                literals.remove("reg");
            }
            if (loginAlias) {
                literals.remove("log");
            }

            CommandNode<ServerCommandSource> rootNode = serverCommandManager.getDispatcher().getRoot();

            rootNode.getChildren().removeIf(node ->
                    node.getName().equals("register") ||
                    node.getName().equals("login") ||
                    (regAlias && node.getName().equals("reg")) ||
                    (loginAlias && node.getName().equals("log")));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LogError("Error while reloading commands: ", e);
            return;
        }

        RegisterCommand.registerCommand(serverCommandManager.getDispatcher());
        LoginCommand.registerCommand(serverCommandManager.getDispatcher());

        if (server.getPlayerManager() == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            serverCommandManager.sendCommandTree(player);
        }
    }

    public static ZonedDateTime getUnixZero() {
        return ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }
}
