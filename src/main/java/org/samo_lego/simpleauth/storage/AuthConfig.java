/*
    Original author:
    https://github.com/jellysquid3/Lithium/blob/1.15.x/fabric/src/main/java/me/jellysquid/mods/lithium/common/config/LithiumConfig.java

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthConfig {
    // If player is not authenticated, following conditions apply
    public static class MainConfig {
        // Allows "right-clicking" on an entity (e.g. clicking on villagers)
        public boolean allowEntityInteract = false;
        // Maximum login tries before kicking the player from server
        // Set to -1 to allow unlimited, not recommended however
        public int maxLoginTries = 1;
        // Time after which player will be kicked if not authenticated - in seconds
        public int delay = 60;
        // Disables registering and forces logging in with global password
        // Visit https://github.com/samolego/SimpleAuth/wiki/Locking-server-with-global-password for more info
        public boolean enableGlobalPassword = false;
        public String globalPassword = null;
        // Tries to rescue players if they are stuck inside a portal on logging in
        // Visit https://github.com/samolego/SimpleAuth/wiki/Portal-Rescue for more info
        public boolean tryPortalRescue = true;
        // Minimum and maximum length of password. Set -1 to disable max chars
        public int minPasswordChars = 4;
        public int maxPasswordChars = -1;
        // Regex of valid playername characters. You probably don't want to change this.
        // Visit https://github.com/samolego/SimpleAuth/wiki/Username-Restriction for more info
        public String usernameRegex = "^[a-zA-Z0-9_]{3,16}$";
        // How long to keep session (auto-logging in the player), in seconds
        // Set to -1 to disable
        // Visit https://github.com/samolego/SimpleAuth/wiki/Sessions for more info
        public int sessionTimeoutTime = 60;
    }
    public static class LangConfig {
        public String enterPassword = "§6You need to enter your password!";
        public String enterNewPassword = "§4You need to enter new password!";
        public String wrongPassword = "§4Wrong password!";
        public String matchPassword = "§6Passwords must match!";
        public String passwordUpdated = "§aYour password was updated successfully!";
        public String loginRequired = "§cYou are not authenticated!\n§6Use /login to authenticate!";
        public String loginTriesExceeded = "§4Too many login tries.";
        public String globalPasswordSet = "§aGlobal password was successfully set!";
        public String cannotChangePassword = "§cYou cannot change password!";
        public String cannotUnregister = "§cYou cannot unregister this account!";
        public String notAuthenticated = "§cYou are not authenticated!\n§6Try with /login or /register.";
        public String alreadyAuthenticated = "§6You are already authenticated.";
        public String successfullyAuthenticated = "§aYou are now authenticated.";
		public String successfulLogout = "§aLogged out successfully.";
        public String timeExpired = "§cTime for authentication has expired.";
        public String notRegistered = "§6This account is not yet registered! Type `/register` first";
        public String alreadyRegistered = "§6This account name is already registered!";
        public String registerSuccess = "§aYou are now authenticated.";
        public String userdataDeleted = "§aUserdata deleted.";
        public String userdataUpdated = "§aUserdata updated.";
        public String accountDeleted = "§aYour account was successfully deleted!";
        public String configurationReloaded = "§aConfiguration file was reloaded successfully.";
        public String successfulPortalRescue = "§aYou were rescued from nether portal successfully!";
        public String maxPasswordChars = "§6Password can be at most %d characters long!";
        public String minPasswordChars = "§6Password needs to be at least %d characters long!";
        public String disallowedUsername = "§6Invalid username characters! Allowed character regex: %s";
        public String playerAlreadyOnline = "§cPlayer %s is already online!";
    }
    public static class ExperimentalConfig {
        // Prevents player being kicked because another player with the same name has joined the server
        public boolean disableAnotherLocationKick = true;
        // If player should be invulnerable before authentication
        public boolean playerInvulnerable = true;
        // If player should be invisible to mobs before authentication
        public boolean playerInvisible = true;
        // Allows chat (but not commands, except for /login and /register)
        public boolean allowChat = false;
        // Allows player movement
        public boolean allowMovement = false;
        // Allows block "use" - right clicking (e.g. opening a chest)
        public boolean allowBlockUse = false;
        // Allows mining || punching blocks
        public boolean allowBlockPunch = false;
        // Allows dropping items from inventory
        public boolean allowItemDrop = false;
        // Allows moving item through inventory
        public boolean allowItemMoving = false;
        // Allows item "use" - right click function (e.g. using a bow)
        public boolean allowItemUse = false;
        // Allows attacking mobs
        public boolean allowEntityPunch = false;
    }
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public MainConfig main = new MainConfig();
    public LangConfig lang = new LangConfig();
    public ExperimentalConfig experimental = new ExperimentalConfig();

    public static AuthConfig load(File file) {
        AuthConfig config;
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
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
    public void save(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("[SimpleAuth] Problem occurred when saving config: ", e);
        }
    }
}