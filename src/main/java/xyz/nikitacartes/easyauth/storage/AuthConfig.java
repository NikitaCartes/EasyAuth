/**
 * This class has been adapted from old Lithium's config file
 * @author jellysquid https://github.com/jellysquid3/Lithium/blob/1.15.x/fabric/src/main/java/me/jellysquid/mods/lithium/common/config/LithiumConfig.java

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package xyz.nikitacartes.easyauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.nikitacartes.easyauth.utils.EasyLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import static xyz.nikitacartes.easyauth.EasyAuth.serverProp;

public class AuthConfig {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // If player is not authenticated, following conditions apply
    public static class MainConfig {
        /**
         * Allows "right-clicking" on an entity (e.g. clicking on villagers).
         */
        public final boolean allowEntityInteract = false;
        /**
         * Maximum login tries before kicking the player from server.
         * Set to -1 to allow unlimited, not recommended however.
         */
        public final int maxLoginTries = 1;
        /**
         * Time after which player will be kicked if not authenticated - in seconds
         */
        public final int kickTime = 60;
        /**
         * Disables registering and forces logging in with global password.
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Global-password" target="_blank">wiki</a>
         */
        public boolean enableGlobalPassword = false;
        /**
         * Hashed global password.
         */
        public String globalPassword;
        /**
         * Tries to rescue players if they are stuck inside a portal on logging in.
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Portal-Rescue" target="_blank">wiki</a>
         */
        public final boolean tryPortalRescue = true;
        /**
         * Minimum length of password.
         */
        public final int minPasswordChars = 4;
        /**
         * Maximum length of password.
         * Set -1 to disable.
         */
        public final int maxPasswordChars = -1;
        /**
         * Regex of valid playername characters. You probably don't want to change this.
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Username-Restriction" target="_blank">wiki</a>
         */
        public final String usernameRegex = "^[a-zA-Z0-9_]{3,16}$";
        /**
         * How long to keep session (auto-logging in the player), in seconds
         * Set to -1 to disable
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Sessions" target="_blank">wiki</a>
         */
        public final int sessionTimeoutTime = 60;

        /**
         * Whether to tp player to spawn when joining (to hide original player coordinates).
         */
        public boolean spawnOnJoin = false;

        /**
         * Data for spawn (where deauthenticated players are teleported temporarily).
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/Coordinate-Hiding" target="_blank">wiki</a>
         */
        public static class WorldSpawn {
            /**
             * Dimension id, e.g. "minecraft:overworld"
             */
            public String dimension;
            public double x;
            public double y;
            public double z;
            public float yaw;
            public float pitch;
        }

        /**
         * Whether to use MongoDB instead of LevelDB.
         * Note: you need to install MongoDB yourself, as well
         * as create a user (account) that will be used by EasyAuth
         * to manage its database.
         */
        public final boolean useMongoDB = false;

        /**
         * Credentials for MongoDB database.
         * Leave this as-is if you are using LevelDB.
         */
        public static class MongoDBCredentials {
            /**
             * Username for the database access.
             */
            public final String username = "";
            /**
             * Password for the database access.
             */
            public final String password = "";
            /**
             * Database where user with provided credentials
             * is located.
             */
            public final String userSourceDatabase = "";
            /**
             * Database host (address).
             */
            public final String host = "localhost";
            /**
             * Database port.
             * Default: 27017
             */
            public final int port = 27017;
            /**
             * Name of the new database in which EasyAuth should
             * store player data.
             */
            public final String easyAuthDatabase = "EasyAuthPlayerData";
            /**
             * Whether to use ssl connection.
             */
            public final boolean useSsl = true;
        }

        /**
         * Whether players who have a valid session should skip the authentication process.
         * You have to set online-mode to true in server.properties!
         * (cracked players will still be able to enter, but they'll need to login)
         *
         * This protects premium usernames from being stolen, since cracked players
         * with name that is found in Mojang database, are kicked.
         */
        public boolean premiumAutologin = false;

        /**
         * Contains a list of lower case (!) player names
         * that should always be treated as offline.
         * <p>
         * Used when  AuthConfig#premiumAutoLogin is enabled
         * and you have some players that want to use username,
         * that is already taken.
         */
        public final ArrayList<String> forcedOfflinePlayers = new ArrayList<>(Collections.singletonList(""));

    }
    public static class LangConfig {
        public final String enterPassword = "\u00A76You need to enter your password!";
        public final String enterNewPassword = "\u00A74You need to enter new password!";
        public final String wrongPassword = "\u00A74Wrong password!";
        public final String matchPassword = "\u00A76Passwords must match!";
        public final String passwordUpdated = "\u00A7aYour password was updated successfully!";
        public final String loginRequired = "\u00A76Use /login <password> to authenticate!";
        public final String loginTriesExceeded = "\u00A74Too many login tries.";
        public final String globalPasswordSet = "\u00A7aGlobal password was successfully set!";
        public final String cannotChangePassword = "\u00A7cYou cannot change password!";
        public final String cannotUnregister = "\u00A7cYou cannot unregister this account!";
        public final String notAuthenticated = "\u00A7cYou are not authenticated!";
        public final String alreadyAuthenticated = "\u00A76You are already authenticated.";
        public final String successfullyAuthenticated = "\u00A7aYou are now authenticated.";
		public final String successfulLogout = "\u00A7aLogged out successfully.";
        public final String timeExpired = "\u00A7cTime for authentication has expired.";
        public final String registerRequired = "\u00A76Type /register <password> <password> to claim this account.";
        public final String alreadyRegistered = "\u00A76This account name is already registered!";
        public final String registerSuccess = "\u00A7aAccount was created.";
        public final String userdataDeleted = "\u00A7aUserdata deleted.";
        public final String userdataUpdated = "\u00A7aUserdata updated.";
        public final String accountDeleted = "\u00A7aYour account was successfully deleted!";
        public final String configurationReloaded = "\u00A7aConfiguration file was reloaded successfully.";
        public final String maxPasswordChars = "\u00A76Password can be at most %d characters long!";
        public final String minPasswordChars = "\u00A76Password needs to be at least %d characters long!";
        public final String disallowedUsername = "\u00A76Invalid username characters! Allowed character regex: %s";
        public final String playerAlreadyOnline = "\u00A7cPlayer %s is already online!";
        public final String worldSpawnSet = "\u00A7aSpawn for logging in was set successfully.";
        public final String userNotRegistered = "\u00A7cThis player is not registered!";
        public final String cannotLogout = "\u00A7cYou cannot logout!";
    }
    public static class ExperimentalConfig {
        /**
         * Prevents player being kicked because another player with the same name has joined the server.
         */
        public final boolean preventAnotherLocationKick = true;
        /**
         * If player should be invulnerable before authentication.
         */
        public final boolean playerInvulnerable = true;
        /**
         * If player should be invisible to mobs before authentication.
         */
        public final boolean playerInvisible = true;
        /**
         * Allows chat (but not commands, except for /login and /register).
         */
        public final boolean allowChat = false;
        /**
         * Allows player movement.
         */
        public final boolean allowMovement = false;
        /**
         * Allows block "use" - right clicking (e.g. opening a chest).
         */
        public final boolean allowBlockUse = false;
        /**
         *  Allows mining or punching blocks.
         */
        public final boolean allowBlockPunch = false;
        /**
         * Allows dropping items from inventory.
         */
        public final boolean allowItemDrop = false;
        /**
         * Allows moving item through inventory.
         */
        public final boolean allowItemMoving = false;
        /**
         * Allows item "use" - right click function (e.g. using a bow).
         */
        public final boolean allowItemUse = false;
        /**
         * Allows attacking mobs.
         */
        public final boolean allowEntityPunch = false;
        /**
         * Debug mode. Expect much spam in console.
         */
        public final boolean debugMode = false;
        /**
         * Whether to use BCrypt instead of Argon2 (GLIBC_2.25 error).
         * @see <a href="https://github.com/NikitaCartes/EasyAuth/wiki/GLIBC-problems" target="_blank">wiki</a>
         */
        public final boolean useBCryptLibrary = false;
        /**
         * Whether to modify player uuids to offline style.
         * Note: this should be used only if you had your server
         * running in offline mode and you made the switch to use
         * AuthConfig#premiumAutoLogin AND your players already
         * have e.g. villager discounts, which are based on uuid.
         * Other things (advancements, playerdata) are migrated
         * automatically, so think before enabling this. In case
         * an online-mode player changes username, they'll loose all
         * their stuff, unless you migrate it manually.
         */
        public boolean forcedOfflineUuids = false;
        /**
         * To use existing database from SimpleAuth replace this string with "SimpleAuth".
         * Database will be saved in mods/<databaseFolder>
         */
        public final String databaseFolder = "EasyAuth";
    }

    public final MainConfig main = new MainConfig();
    public final MainConfig.WorldSpawn worldSpawn = new MainConfig.WorldSpawn();
    public final MainConfig.MongoDBCredentials mongoDBCredentials = new MainConfig.MongoDBCredentials();
    public final LangConfig lang = new LangConfig();
    public final ExperimentalConfig experimental = new ExperimentalConfig();


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
                if(!Boolean.parseBoolean(serverProp.getProperty("online-mode"))) {
                    if(config.experimental.forcedOfflineUuids) {
                        EasyLogger.logInfo("Server is in offline mode, forcedOfflineUuids option is irrelevant. Setting it to false.");
                        config.experimental.forcedOfflineUuids = false;
                    }
                    if(config.main.premiumAutologin) {
                        EasyLogger.logError("You cannot use server in offline mode and premiumAutologin! Disabling the latter.");
                        config.main.premiumAutologin = false;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("[EasyAuth] Problem occurred when trying to load config: ", e);
            }
        }
        else {
            config = new AuthConfig();
        }
        config.save(file);

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
            EasyLogger.logError("Problem occurred when saving config: " + e.getMessage());
        }
    }
}