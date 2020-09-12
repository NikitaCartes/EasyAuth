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
package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class AuthConfig {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // If player is not authenticated, following conditions apply
    public static class MainConfig {
        /**
         * Allows "right-clicking" on an entity (e.g. clicking on villagers).
         */
        public boolean allowEntityInteract = false;
        /**
         * Maximum login tries before kicking the player from server.
         * Set to -1 to allow unlimited, not recommended however.
         */
        public int maxLoginTries = 1;
        /**
         * Time after which player will be kicked if not authenticated - in seconds
         */
        public int kickTime = 60;
        /**
         * Disables registering and forces logging in with global password.
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/Global-password" target="_blank">wiki</a>
         */
        public boolean enableGlobalPassword = false;
        /**
         * Hashed global password.
         */
        public String globalPassword;
        /**
         * Tries to rescue players if they are stuck inside a portal on logging in.
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/Portal-Rescue" target="_blank">wiki</a>
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
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/Username-Restriction" target="_blank">wiki</a>
         */
        public String usernameRegex = "^[a-zA-Z0-9_]{3,16}$";
        /**
         * How long to keep session (auto-logging in the player), in seconds
         * Set to -1 to disable
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/Sessions" target="_blank">wiki</a>
         */
        public int sessionTimeoutTime = 60;

        /**
         * Should deauthenticated players fall if the login mid-air?
         */
        public boolean allowFalling = false;

        /**
         * Whether to tp player to spawn when joining (to hide coordinates)
         */
        public boolean spawnOnJoin =  false;

        /**
         * Data for spawn (where deauthenticated players are teleported).
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/Coordinate-Hiding" target="_blank">wiki</a>
         */
        public static class WorldSpawn {
                /**
                 * Dimension id, e.g. "minecraft:overworld"
                 */
                public String dimension;
                public double x;
                public double y;
                public double z;
        }
    }
    public static class LangConfig {
        public String enterPassword = "§6You need to enter your password!";
        public String enterNewPassword = "§4You need to enter new password!";
        public String wrongPassword = "§4Wrong password!";
        public String matchPassword = "§6Passwords must match!";
        public String passwordUpdated = "§aYour password was updated successfully!";
        public String loginRequired = "§6Use /login <password> to authenticate!";
        public String loginTriesExceeded = "§4Too many login tries.";
        public String globalPasswordSet = "§aGlobal password was successfully set!";
        public String cannotChangePassword = "§cYou cannot change password!";
        public String cannotUnregister = "§cYou cannot unregister this account!";
        public String notAuthenticated = "§cYou are not authenticated!";
        public String alreadyAuthenticated = "§6You are already authenticated.";
        public String successfullyAuthenticated = "§aYou are now authenticated.";
		public String successfulLogout = "§aLogged out successfully.";
        public String timeExpired = "§cTime for authentication has expired.";
        public String registerRequired = "§6Type /register <password> <password> to claim this account.";
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
        /**
         * Allows player movement.
         */
        public boolean allowMovement = false;
        /**
         * Allows block "use" - right clicking (e.g. opening a chest).
         */
        public boolean allowBlockUse = false;
        /**
         *  Allows mining or punching blocks.
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
         * @see <a href="https://github.com/samolego/SimpleAuth/wiki/GLIBC-problems" target="_blank">wiki</a>
         */
        public boolean useBCryptLibrary = false;
    }

    public MainConfig main = new MainConfig();
    public MainConfig.WorldSpawn worldSpawn = new MainConfig.WorldSpawn();
    public LangConfig lang = new LangConfig();
    public ExperimentalConfig experimental = new ExperimentalConfig();


    /**
     * Loads SimpleAuth's config file.
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
            } catch (IOException e) {
                throw new RuntimeException("[SimpleAuth] Problem occurred when trying to load config: ", e);
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
            logError("Problem occurred when saving config: " + e.getMessage());
        }
    }
}