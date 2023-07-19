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
                        LogInfo("You have premiumAutologin enabled. Enable online-mode so that online player can not register.");
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
        /**
         * Allows "right-clicking" on an entity (e.g. clicking on villagers).
         */
        public boolean allowEntityInteract = false;
        /**
         * Maximum login tries before kicking the player from server.
         * Set to -1 to allow unlimited, not recommended, however.
         */
        public int maxLoginTries = 1;
        /**
         * Time after which player will be kicked if not authenticated - in seconds
         */
        public int kickTime = 60;
        /**
         * Disables registering and forces logging in with global password.
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Global-password" target="_blank">wiki</a>
         */
        public boolean enableGlobalPassword = false;
        /**
         * Hashed global password.
         */
        public String globalPassword;
        /**
         * Tries to rescue players if they are stuck inside a portal on logging in.
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Portal-Rescue" target="_blank">wiki</a>
         */
        public boolean tryPortalRescue = true;
        /**
         * Minimum length of password.
         */
        public int minPasswordChars = 4;
        /**
         * Maximum length of password.
         * Set -1 to disable.
         */
        public int maxPasswordChars = -1;
        /**
         * Regex of valid playername characters. You probably don't want to change this.
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Username-Restriction" target="_blank">wiki</a>
         */
        public String usernameRegex = "^[a-zA-Z0-9_]{3,16}$";
        /**
         * How long to keep session (auto-logging in the player), in seconds
         * Set to -1 to disable
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Sessions" target="_blank">wiki</a>
         */
        public int sessionTimeoutTime = 3600;

        /**
         * Whether to tp player to spawn when joining (to hide original player coordinates).
         */
        public boolean spawnOnJoin = false;

        /**
         * Database type. Can be "mysql" or "mongodb". LevelDB is set by default.
         */
        public String databaseType = "";
        /**
         * MySQL host.
         */
        public String MySQLHost = "localhost";
        /**
         * MySQL user.
         */
        public String MySQLUser = "root";
        /**
         * MySQL password.
         */
        public String MySQLPassword = "";
        /**
         * MySQL database name.
         */
        public String MySQLDatabase = "easyauth";
        /**
         * MySQL table name.
         */
        public String MySQLTableName = "easyauth";
        /**
         * MongoDB connection string.
         */
        public String MongoDBConnectionString = "mongodb://username:password@host:port/?options";
        /**
         * MongoDB database name.
         */
        public String MongoDBDatabase = "easyauth";

        /**
         * Whether players who have a valid session should skip the authentication process.
         * You have to set online-mode to true in server.properties!
         * (cracked players will still be able to enter, but they'll need to log in)
         * <p>
         * This protects premium usernames from being stolen, since cracked players
         * with name that is found in Mojang database, are kicked.
         */
        public boolean premiumAutologin = true;
        /**
         * Whether bedrock players should skip the authentication process.
         * You have to set online-mode to true in server.properties!
         */
        public boolean floodgateAutologin = false;
        /**
         * Contains a list of lower case (!) player names
         * that should always be treated as offline.
         * <p>
         * Used when  AuthConfig#premiumAutoLogin is enabled,
         * and you have some players that want to use username,
         * that is already taken.
         */
        public ArrayList<String> forcedOfflinePlayers = new ArrayList<>();
        /**
         * Hide unauthenticated pLayers from player list
         */
        public boolean hideUnauthenticatedPLayersFromPlayerList = false;

        /**
         * Data for spawn (where deauthenticated players are teleported temporarily).
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Coordinate-Hiding" target="_blank">wiki</a>
         */
        public static class WorldSpawn {
            /**
             * Dimension id, e.g. "minecraft:overworld"
             */
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
        /**
         * Prevents player being kicked because another player with the same name has joined the server.
         */
        public boolean preventAnotherLocationKick = true;
        /**
         * If player should be invulnerable before authentication.
         */
        public boolean playerInvulnerable = true;
        /**
         * If player should be invisible to mobs before authentication.
         */
        public boolean playerInvisible = true;
        /**
         * Allows chat (but not commands, except for /login and /register).
         */
        public boolean allowChat = false;
        public boolean allowCommands = false;
        public ArrayList<String> allowedCommands = new ArrayList<>();
        /**
         * Allows player movement.
         */
        public boolean allowMovement = false;
        /**
         * Allows block "use" - right-clicking (e.g. opening a chest).
         */
        public boolean allowBlockUse = false;
        /**
         * Allows mining or punching blocks.
         */
        public boolean allowBlockPunch = false;
        /**
         * Allows dropping items from inventory.
         */
        public boolean allowItemDrop = false;
        /**
         * Allows moving item through inventory.
         */
        public boolean allowItemMoving = false;
        /**
         * Allows item "use" - right click function (e.g. using a bow).
         */
        public boolean allowItemUse = false;
        /**
         * Allows attacking mobs.
         */
        public boolean allowEntityPunch = false;
        /**
         * Debug mode. Expect much spam in console.
         */
        public boolean debugMode = false;
        /**
         * Whether to use BCrypt instead of Argon2 (GLIBC_2.25 error).
         *
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/GLIBC-problems" target="_blank">wiki</a>
         */
        public boolean useBCryptLibrary = false;
        /**
         * Whether to modify player uuids to offline style.
         * Note: this should be used only if you had your server
         * running in offline mode, and you made the switch to use
         * AuthConfig#premiumAutoLogin AND your players already
         * have e.g. villager discounts, which are based on uuid.
         * Other things (advancements, playerdata) are migrated
         * automatically, so think before enabling this. In case
         * an online-mode player changes username, they'll lose all
         * their stuff, unless you migrate it manually.
         */
        public boolean forcedOfflineUuids = false;
        /**
         * LevelDB database is hard to move,
         * so the best solution for now would be to use old DB
         * from SimpleAuth folder
         */
        public boolean useSimpleAuthDatabase = false;
        /**
         * Cancellation of packets with player's movement and
         * teleportation back leads to an increase number of these packets.
         * That setting limits players teleportation.
         * This setting is server-wide so maximum rate would be
         * (1000/teleportationTimeoutInMs) per seconds for all unauthorised players.
         * Value 0 would effectively disable this setting
         * so players will be teleported after each packet.
         */
        public long teleportationTimeoutInMs = 5;
        /**
         * Enabling or disabling aliases.
         * For now, it's only affects `\l` as alias for `\login`
         */
        public boolean enableAliases = true;
        /**
         * Enabling or disabling server-side translation.
         * When this parameter is false the translation from `config.json` is used.
         */
        public boolean enableServerSideTranslation = true;
        /**
         * How long it takes (seconds) after a player gets kicked
         * for too many logins for the player to be allowed back in.
         */
        public long resetLoginAttemptsTime = 120;
        /**
         * Temporary rules
         */
        public boolean floodgateLoaded = false;

        public boolean floodgateBypassUsernameRegex = false;

        public ArrayList<String> verifiedOnlinePlayer = new ArrayList<>();

        /**
         * Skip all authentication checks for all players. Should be used if authentication is handled by another plugin/proxy/etc.
         */
        public boolean skipAllAuthChecks = false;
    }
}
