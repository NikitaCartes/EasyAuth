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

    public boolean allowCommands = false;
    public ArrayList<String> allowedCommands = new ArrayList<>();

}
