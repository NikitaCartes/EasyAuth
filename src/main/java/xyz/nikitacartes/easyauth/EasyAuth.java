package xyz.nikitacartes.easyauth;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.config.*;
import xyz.nikitacartes.easyauth.storage.database.*;
import xyz.nikitacartes.easyauth.integrations.LuckPermsIntegration;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

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
    public static DialogConfigV1 dialogConfig;


    public static void loadDatabase() {
        DB = getDbApi();
        try {
            DB.connect();
            if (DB.isClosed()) {
                LogError("Database connection is closed right after connect()");
                DB = null;
            }
        } catch (DBApiException e) {
            LogError("Error while setting up database connection", e);
            DB = null;
        }
    }

    public static void createConfigFolder() {
        File file = new File(gameDirectory + "/config/EasyAuth");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("[EasyAuth] Error creating directory for configs");
            }
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
        if (DB == null || DB.isClosed()) {
            LogError("CRITICAL: database unavailable — stopping server to prevent auth bypass");
            server.halt(false);
            return;
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
        if (DB != null) {
            DB.close();
        }
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
            EasyAuth.dialogConfig = DialogConfigV1.create();
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
        EasyAuth.dialogConfig = DialogConfigV1.load();

        configMigration(configVersion);
    }

    public static void saveConfigs() {
        EasyAuth.config.save();
        EasyAuth.technicalConfig.save();
        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.storageConfig.save();
        EasyAuth.dialogConfig.save();
    }

    public static void reloadConfigs(MinecraftServer server) {
        if (DB != null) {
            DB.close();
        }

        boolean regAlias = extendedConfig.aliases.register;
        boolean loginAlias = extendedConfig.aliases.login;

        EasyAuth.loadConfigs();

        if (DB == null) {
            DB = getDbApi();
        }
        try {
            DB.connect();
            if (DB.isClosed()) {
                LogError("Database reconnection failed; auth features unavailable");
            }
        } catch (DBApiException e) {
            LogError("Database reconnection error: ", e);
        }

        if (DB == null || DB.isClosed()) {
            LogError("CRITICAL: database unavailable after reload — logins/registrations are locked until it is restored");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (StoneCutterUtils.isOperator(server.getPlayerList(), player)) {
                    langConfig.admin.databaseUnavailable.send(player);
                }
            }
        }

        Commands serverCommandManager = server.getCommands();
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

            CommandNode<CommandSourceStack> rootNode = serverCommandManager.getDispatcher().getRoot();

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

        if (server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            serverCommandManager.sendCommands(player);
        }
    }

    public static ZonedDateTime getUnixZero() {
        return ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }
}
