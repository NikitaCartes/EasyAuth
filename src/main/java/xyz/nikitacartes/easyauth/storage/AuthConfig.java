/**
 * This class has been adapted from old Lithium's config file
 *
 * @author jellysquid https://github.com/jellysquid3/Lithium/blob/1.15.x/fabric/src/main/java/me/jellysquid/mods/lithium/common/config/LithiumConfig.java
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.nikitacartes.easyauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.storage.database.LevelDB;
import xyz.nikitacartes.easyauth.storage.database.MongoDB;
import xyz.nikitacartes.easyauth.storage.database.MySQL;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.serverProp;
import static xyz.nikitacartes.easyauth.EasyAuth.DB;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class AuthConfig {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    public MainConfig main = new MainConfig();
    public MainConfig.WorldSpawn worldSpawn = new MainConfig.WorldSpawn();
    public LangConfig lang = new LangConfig();
    public ExperimentalConfig experimental = new ExperimentalConfig();

    /**
     * Loads EasyAuth's config file.
     *
     * @param file file to load config from
     * @return AuthConfig config object
     */
    public static AuthConfig load(File file) {
        AuthConfig config;
        if (file.exists()) {
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )) {
                config = gson.fromJson(fileReader, AuthConfig.class);
                if (!Boolean.parseBoolean(serverProp.getProperty("online-mode"))) {
                    if (config.experimental.forcedOfflineUuids) {
                        LogInfo("Server is in offline mode, forcedOfflineUuids option is irrelevant. Setting it to false.");
                        config.experimental.forcedOfflineUuids = false;
                    }
                    if (config.main.premiumAutologin) {
                        LogWarn("You cannot use server in offline mode and premiumAutologin! Disabling the latter.");
                        config.main.premiumAutologin = false;
                    }
                } else {
                    if (!config.main.premiumAutologin) {
                        LogWarn("With online-mode enabled and premiumAutoLogin disabled, offline players will not be able to join.");
                    }
                }
                if (config.experimental.enableServerSideTranslation && !FabricLoader.getInstance().isModLoaded("server_translations_api")) {
                    config.experimental.enableServerSideTranslation = false;
                }
                AuthEventHandler.usernamePattern = Pattern.compile(config.main.usernameRegex);
            } catch (IOException e) {
                throw new RuntimeException("[EasyAuth] Problem occurred when trying to load config: ", e);
            }
        } else {
            config = new AuthConfig();
        }
        if (FabricLoader.getInstance().isModLoaded("floodgate")) {
            config.experimental.floodgateLoaded = true;
        }
        config.save(file);
        if (DB != null && !DB.isClosed()) {
            DB.close();
        }
        // Connecting to db
        if (config.main.databaseType.equalsIgnoreCase("mysql")) {
            DB = new MySQL(config);
        } else if (config.main.databaseType.equalsIgnoreCase("mongodb")) {
            DB = new MongoDB(config);
        } else {
            DB = new LevelDB(config);
        }
        return config;
    }

    /**
     * Saves the config to the given file.
     *
     * @param file file to save config to
     */
    public void save(File file) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            LogError("Problem occurred when saving config", e);
        }
    }

    // If player is not authenticated, following conditions apply
    public static class MainConfig {
        public boolean allowEntityInteract = false;
        public int maxLoginTries = 1;
        public int kickTime = 60;
        public boolean enableGlobalPassword = false;
        public String globalPassword;
        public boolean tryPortalRescue = true;
        public int minPasswordChars = 4;
        public int maxPasswordChars = -1;
        public String usernameRegex = "^[a-zA-Z0-9_]{3,16}$";
        public int sessionTimeoutTime = 3600;
        public boolean spawnOnJoin = false;
        public String databaseType = "";
        public String MySQLHost = "localhost";
        public String MySQLUser = "root";
        public String MySQLPassword = "";
        public String MySQLDatabase = "easyauth";
        public String MySQLTableName = "easyauth";
        public String MongoDBConnectionString = "mongodb://username:password@host:port/?options";
        public String MongoDBDatabase = "easyauth";
        public boolean premiumAutologin = true;
        public boolean floodgateAutologin = false;
        public ArrayList<String> forcedOfflinePlayers = new ArrayList<>();
        public boolean hideUnauthenticatedPLayersFromPlayerList = false;

        public static class WorldSpawn {
            public String dimension = "minecraft:overworld";
            public double x = 0;
            public double y = 64;
            public double z = 0;
            public float yaw;
            public float pitch;
        }

    }

    public static class LangConfig {
        public String enterPassword = "§6You need to enter your password!";
        public String enterNewPassword = "§4You need to enter new password!";
        public String wrongPassword = "§4Wrong password!";
        public String matchPassword = "§6Passwords must match!";
        public String passwordUpdated = "§aYour password was updated successfully!";
        public String loginRequired = "§cYou are not authenticated!\n§6Use /login, /l to authenticate!";
        public String loginTriesExceeded = "§4Too many login tries. Please wait a few minutes and try again."; // Todo: add how much time to wait
        public String globalPasswordSet = "§aGlobal password was successfully set!";
        public String cannotChangePassword = "§cYou cannot change password!";
        public String cannotUnregister = "§cYou cannot unregister this account!";
        public String notAuthenticated = "§cYou are not authenticated!\n§6Try with /login, /l or /register.";
        public String alreadyAuthenticated = "§6You are already authenticated.";
        public String successfullyAuthenticated = "§aYou are now authenticated.";
        public String successfulLogout = "§aLogged out successfully.";
        public String timeExpired = "§cTime for authentication has expired.";
        public String registerRequired = "§6Type /register \u003cpassword\u003e \u003cpassword\u003e to claim this account.";
        public String alreadyRegistered = "§6This account name is already registered!";
        public String registerSuccess = "§aYou are now authenticated.";
        public String userdataDeleted = "§aUserdata deleted.";
        public String userdataUpdated = "§aUserdata updated.";
        public String accountDeleted = "§aYour account was successfully deleted!";
        public String configurationReloaded = "§aConfiguration file was reloaded successfully.";
        public String maxPasswordChars = "§6Password can be at most %d characters long!";
        public String minPasswordChars = "§6Password needs to be at least %d characters long!";
        public String disallowedUsername = "§6Invalid username characters! Allowed character regex: %s";
        public String playerAlreadyOnline = "§cPlayer %s is already online!";
        public String worldSpawnSet = "§aSpawn for logging in was set successfully.";
        public String corruptedPlayerData = "§cYour data is probably corrupted. Please contact admin.";
        public String userNotRegistered = "§cThis player is not registered!";
        public String cannotLogout = "§cYou cannot logout!";
        public String offlineUuid = "Offline UUID for %s is %s";
        public String registeredPlayers = "List of registered players:";
        public String addToForcedOffline = "Player successfully added into forcedOfflinePlayers list";
    }

    public static class ExperimentalConfig {
        public boolean preventAnotherLocationKick = true;
        public boolean playerInvulnerable = true;
        public boolean playerInvisible = true;
        public boolean allowChat = false;
        public boolean allowCommands = false;
        public ArrayList<String> allowedCommands = new ArrayList<>();
        public boolean allowMovement = false;
        public boolean allowBlockUse = false;
        public boolean allowBlockPunch = false;
        public boolean allowItemDrop = false;
        public boolean allowItemMoving = false;
        public boolean allowItemUse = false;
        public boolean allowEntityPunch = false;
        public boolean debugMode = false;
        public boolean useBCryptLibrary = false;
        public boolean forcedOfflineUuids = false;
        public boolean useSimpleAuthDatabase = false;
        public long teleportationTimeoutInMs = 5;
        public boolean enableAliases = true;
        public boolean enableServerSideTranslation = true;
        public long resetLoginAttemptsTime = 120;
        public boolean floodgateLoaded = false;

        public boolean floodgateBypassUsernameRegex = false;

        public ArrayList<String> verifiedOnlinePlayer = new ArrayList<>();

    }
}
