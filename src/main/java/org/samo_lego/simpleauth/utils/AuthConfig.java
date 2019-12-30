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
package org.samo_lego.simpleauth.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthConfig {
    public static class MainConfig {
        public boolean allowChat = false;
        public boolean allowMovement = false;
        public boolean allowBlockUse = false;
        public boolean allowBlockPunch = false;
        public boolean allowItemDrop = false;
        public boolean allowItemUse = false;
        public boolean allowEntityPunch = false;
        public boolean allowEntityInteract = false;
        public boolean playerInvulnerable = true;
        public boolean playerInvisible = true;
    }
    public static class LangConfig {
        public String enterPassword = "§6You need to enter your password!";
        public String enterNewPassword = "§4You need to enter new password!";
        public String wrongPassword = "§4Wrong password!";
        public String matchPassword = "§6Passwords must match!";
        public String passwordUpdated = "§4Your password was updated successfully!";
        public String notAuthenticated = "§cYou are not authenticated!\n§6Try with /login or /register.";
        public String alreadyAuthenticated = "§4You are already authenticated.";
        public String successfullyAuthenticated = "§aYou are now authenticated.";
        public String alreadyRegistered = "§6This account name is already registered!";
        public String registerSuccess = "§aYou are now authenticated.";
        public String userdataDeleted = "§aUserdata deleted.";
        public String userdataUpdated = "§aUserdata updated.";
        public String configurationReloaded = "§aConfiguration file was reloaded successfully.";

    }
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public MainConfig main = new MainConfig();
    public LangConfig lang = new LangConfig();

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
    private void save(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("[SimpleAuth] Problem occurred when saving config: ", e);
        }
    }
}