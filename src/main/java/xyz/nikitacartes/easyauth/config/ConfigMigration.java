package xyz.nikitacartes.easyauth.config;

import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.deprecated.AuthConfig;

import java.io.File;

import static net.minecraft.text.Text.translatableWithFallback;
import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

public class ConfigMigration {

    public static void migrateFromV0() {
        AuthConfig oldConfig = AuthConfig.load(new File(gameDirectory + "/mods/EasyAuth/config.json"));
        if (oldConfig == null) {
            return;
        }

        LogInfo("Migrating config from v0 to v1");

        EasyAuth.config = new MainConfigV1();
        EasyAuth.config.premiumAutologin = oldConfig.main.premiumAutologin;
        EasyAuth.config.floodgateAutoLogin = oldConfig.main.floodgateAutologin;
        EasyAuth.config.maxLoginTries = oldConfig.main.maxLoginTries;
        EasyAuth.config.kickTimeout = oldConfig.main.kickTime;
        EasyAuth.config.resetLoginAttemptsTimeout = oldConfig.experimental.resetLoginAttemptsTime;
        EasyAuth.config.sessionTimeout = oldConfig.main.sessionTimeoutTime;
        EasyAuth.config.enableGlobalPassword = oldConfig.main.enableGlobalPassword;
        EasyAuth.config.hidePlayerCoords = oldConfig.main.spawnOnJoin;
        EasyAuth.config.debug = oldConfig.experimental.debugMode;
        EasyAuth.config.configVersion = 1;
        EasyAuth.config.worldSpawn.dimension = oldConfig.worldSpawn.dimension;
        EasyAuth.config.worldSpawn.x = oldConfig.worldSpawn.x;
        EasyAuth.config.worldSpawn.y = oldConfig.worldSpawn.y;
        EasyAuth.config.worldSpawn.z = oldConfig.worldSpawn.z;
        EasyAuth.config.worldSpawn.yaw = oldConfig.worldSpawn.yaw;
        EasyAuth.config.worldSpawn.pitch = oldConfig.worldSpawn.pitch;
        EasyAuth.config.save();

        EasyAuth.extendedConfig = new ExtendedConfigV1();
        EasyAuth.extendedConfig.allowChat = oldConfig.experimental.allowChat;
        EasyAuth.extendedConfig.allowCommands = oldConfig.experimental.allowCommands;
        EasyAuth.extendedConfig.allowedCommands = oldConfig.experimental.allowedCommands;
        EasyAuth.extendedConfig.allowMovement = oldConfig.experimental.allowMovement;
        EasyAuth.extendedConfig.allowBlockInteraction = oldConfig.experimental.allowBlockUse;
        EasyAuth.extendedConfig.allowEntityInteraction = oldConfig.main.allowEntityInteract;
        EasyAuth.extendedConfig.allowBlockBreaking = oldConfig.experimental.allowBlockPunch;
        EasyAuth.extendedConfig.allowEntityAttacking = oldConfig.experimental.allowEntityPunch;
        EasyAuth.extendedConfig.allowItemDropping = oldConfig.experimental.allowItemDrop;
        EasyAuth.extendedConfig.allowItemMoving = oldConfig.experimental.allowItemMoving;
        EasyAuth.extendedConfig.allowItemUsing = oldConfig.experimental.allowItemUse;
        EasyAuth.extendedConfig.playerInvulnerable = oldConfig.experimental.playerInvulnerable;
        EasyAuth.extendedConfig.playerIgnored = oldConfig.experimental.playerInvisible;
        EasyAuth.extendedConfig.teleportationTimeoutMs = oldConfig.experimental.teleportationTimeoutInMs;
        EasyAuth.extendedConfig.enableAliases = oldConfig.experimental.enableAliases;
        EasyAuth.extendedConfig.tryPortalRescue = oldConfig.main.tryPortalRescue;
        EasyAuth.extendedConfig.minPasswordLength = oldConfig.main.minPasswordChars;
        EasyAuth.extendedConfig.maxPasswordLength = oldConfig.main.maxPasswordChars;
        EasyAuth.extendedConfig.usernameRegexp = oldConfig.main.usernameRegex;
        EasyAuth.extendedConfig.floodgateBypassRegex = oldConfig.experimental.floodgateBypassUsernameRegex;
        EasyAuth.extendedConfig.hidePlayersFromPlayerList = oldConfig.main.hideUnauthenticatedPLayersFromPlayerList;
        EasyAuth.extendedConfig.preventAnotherLocationKick = oldConfig.experimental.preventAnotherLocationKick;
        EasyAuth.extendedConfig.useBcrypt = oldConfig.experimental.useBCryptLibrary;
        EasyAuth.extendedConfig.forcedOfflineUuid = oldConfig.experimental.forcedOfflineUuids;
        EasyAuth.extendedConfig.skipAllAuthChecks = oldConfig.experimental.skipAllAuthChecks;
        EasyAuth.extendedConfig.save();

        EasyAuth.langConfig = new LangConfigV1();
        EasyAuth.langConfig.enableServerSideTranslation = oldConfig.experimental.enableServerSideTranslation;
        EasyAuth.langConfig.enterPassword = translatableWithFallback("text.easyauth.enterPassword", oldConfig.lang.enterPassword);
        EasyAuth.langConfig.enterNewPassword = translatableWithFallback("text.easyauth.enterNewPassword", oldConfig.lang.enterNewPassword);
        EasyAuth.langConfig.wrongPassword = translatableWithFallback("text.easyauth.wrongPassword", oldConfig.lang.wrongPassword);
        EasyAuth.langConfig.matchPassword = translatableWithFallback("text.easyauth.matchPassword", oldConfig.lang.matchPassword);
        EasyAuth.langConfig.passwordUpdated = translatableWithFallback("text.easyauth.passwordUpdated", oldConfig.lang.passwordUpdated);
        EasyAuth.langConfig.loginRequired = translatableWithFallback("text.easyauth.loginRequired", oldConfig.lang.loginRequired);
        EasyAuth.langConfig.loginTriesExceeded = translatableWithFallback("text.easyauth.loginTriesExceeded", oldConfig.lang.loginTriesExceeded);
        EasyAuth.langConfig.globalPasswordSet = translatableWithFallback("text.easyauth.globalPasswordSet", oldConfig.lang.globalPasswordSet);
        EasyAuth.langConfig.cannotChangePassword = translatableWithFallback("text.easyauth.cannotChangePassword", oldConfig.lang.cannotChangePassword);
        EasyAuth.langConfig.cannotUnregister = translatableWithFallback("text.easyauth.cannotUnregister", oldConfig.lang.cannotUnregister);
        EasyAuth.langConfig.notAuthenticated = translatableWithFallback("text.easyauth.notAuthenticated", oldConfig.lang.notAuthenticated);
        EasyAuth.langConfig.alreadyAuthenticated = translatableWithFallback("text.easyauth.alreadyAuthenticated", oldConfig.lang.alreadyAuthenticated);
        EasyAuth.langConfig.successfullyAuthenticated = translatableWithFallback("text.easyauth.successfullyAuthenticated", oldConfig.lang.successfullyAuthenticated);
        EasyAuth.langConfig.successfulLogout = translatableWithFallback("text.easyauth.successfulLogout", oldConfig.lang.successfulLogout);
        EasyAuth.langConfig.timeExpired = translatableWithFallback("text.easyauth.timeExpired", oldConfig.lang.timeExpired);
        EasyAuth.langConfig.registerRequired = translatableWithFallback("text.easyauth.registerRequired", oldConfig.lang.registerRequired);
        EasyAuth.langConfig.alreadyRegistered = translatableWithFallback("text.easyauth.alreadyRegistered", oldConfig.lang.alreadyRegistered);
        EasyAuth.langConfig.registerSuccess = translatableWithFallback("text.easyauth.registerSuccess", oldConfig.lang.registerSuccess);
        EasyAuth.langConfig.userdataDeleted = translatableWithFallback("text.easyauth.userdataDeleted", oldConfig.lang.userdataDeleted);
        EasyAuth.langConfig.userdataUpdated = translatableWithFallback("text.easyauth.userdataUpdated", oldConfig.lang.userdataUpdated);
        EasyAuth.langConfig.accountDeleted = translatableWithFallback("text.easyauth.accountDeleted", oldConfig.lang.accountDeleted);
        EasyAuth.langConfig.configurationReloaded = translatableWithFallback("text.easyauth.configurationReloaded", oldConfig.lang.configurationReloaded);
        EasyAuth.langConfig.maxPasswordChars = translatableWithFallback("text.easyauth.maxPasswordChars", oldConfig.lang.maxPasswordChars);
        EasyAuth.langConfig.minPasswordChars = translatableWithFallback("text.easyauth.minPasswordChars", oldConfig.lang.minPasswordChars);
        EasyAuth.langConfig.disallowedUsername = translatableWithFallback("text.easyauth.disallowedUsername", oldConfig.lang.disallowedUsername);
        EasyAuth.langConfig.playerAlreadyOnline = translatableWithFallback("text.easyauth.playerAlreadyOnline", oldConfig.lang.playerAlreadyOnline);
        EasyAuth.langConfig.worldSpawnSet = translatableWithFallback("text.easyauth.worldSpawnSet", oldConfig.lang.worldSpawnSet);
        EasyAuth.langConfig.corruptedPlayerData = translatableWithFallback("text.easyauth.corruptedPlayerData", oldConfig.lang.corruptedPlayerData);
        EasyAuth.langConfig.userNotRegistered = translatableWithFallback("text.easyauth.userNotRegistered", oldConfig.lang.userNotRegistered);
        EasyAuth.langConfig.cannotLogout = translatableWithFallback("text.easyauth.cannotLogout", oldConfig.lang.cannotLogout);
        EasyAuth.langConfig.offlineUuid = translatableWithFallback("text.easyauth.offlineUuid", oldConfig.lang.offlineUuid);
        EasyAuth.langConfig.registeredPlayers = translatableWithFallback("text.easyauth.registeredPlayers", oldConfig.lang.registeredPlayers);
        EasyAuth.langConfig.addToForcedOffline = translatableWithFallback("text.easyauth.addToForcedOffline", oldConfig.lang.addToForcedOffline);
        EasyAuth.langConfig.save();

        EasyAuth.technicalConfig = new TechnicalConfigV1();
        EasyAuth.technicalConfig.globalPassword = oldConfig.main.globalPassword;
        EasyAuth.technicalConfig.forcedOfflinePlayers = oldConfig.main.forcedOfflinePlayers;
        EasyAuth.technicalConfig.confirmedOnlinePlayers = oldConfig.experimental.verifiedOnlinePlayer;
        EasyAuth.technicalConfig.save();

        EasyAuth.storageConfig = new StorageConfigV1();
        EasyAuth.storageConfig.databaseType = oldConfig.main.databaseType;
        EasyAuth.storageConfig.mySqlConfig.mysqlHost = oldConfig.main.MySQLHost;
        EasyAuth.storageConfig.mySqlConfig.mysqlUser = oldConfig.main.MySQLUser;
        EasyAuth.storageConfig.mySqlConfig.mysqlPassword = oldConfig.main.MySQLPassword;
        EasyAuth.storageConfig.mySqlConfig.mysqlDatabase = oldConfig.main.MySQLDatabase;
        EasyAuth.storageConfig.mySqlConfig.mysqlTable = oldConfig.main.MySQLTableName;
        EasyAuth.storageConfig.mongoDBConfig.mongodbConnectionString = oldConfig.main.MongoDBConnectionString;
        EasyAuth.storageConfig.mongoDBConfig.mongodbDatabase = oldConfig.main.MongoDBDatabase;
        EasyAuth.storageConfig.useSimpleAuthDb = oldConfig.experimental.useSimpleAuthDatabase;
        EasyAuth.storageConfig.save();
    }
}
