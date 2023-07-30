package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class LangConfigV1 extends Config {

    public boolean enableServerSideTranslation = true;
    public String enterPassword = "§6You need to enter your password!";
    public String enterNewPassword = "§4You need to enter new password!";
    public String wrongPassword = "§4Wrong password!";
    public String matchPassword = "§6Passwords must match!";
    public String passwordUpdated = "§aYour password was updated successfully!";
    public String loginRequired = "§cYou are not authenticated!\n§6Use /login, /l to authenticate!";
    public String loginTriesExceeded = "§4Too many login tries. Please wait a few minutes and try again.";
    public String globalPasswordSet = "§aGlobal password was successfully set!";
    public String cannotChangePassword = "§cYou cannot change password!";
    public String cannotUnregister = "§cYou cannot unregister this account!";
    public String notAuthenticated = "§cYou are not authenticated!\n§6Try with /login, /l or /register.";
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
    public String cannotLogout = "§cYou cannot logout!";
    public String offlineUuid = "Offline UUID for %s is %s";
    public String registeredPlayers = "List of registered players:";
    public String addToForcedOffline = "Player successfully added into forcedOfflinePlayers list";

    public static LangConfigV1 load() {
        return loadConfig(LangConfigV1.class, "translation.conf");
    }

    public static LangConfigV1 create() {
        return createConfig(LangConfigV1.class);
    }

    protected String getConfigPath() {
        return "translation.conf";
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("enableServerSideTranslation", enableServerSideTranslation);
        configValues.put("enterPassword", escapeString(enterPassword));
        configValues.put("enterNewPassword", escapeString(enterNewPassword));
        configValues.put("wrongPassword", escapeString(wrongPassword));
        configValues.put("matchPassword", escapeString(matchPassword));
        configValues.put("passwordUpdated", escapeString(passwordUpdated));
        configValues.put("loginRequired", escapeString(loginRequired));
        configValues.put("loginTriesExceeded", escapeString(loginTriesExceeded));
        configValues.put("globalPasswordSet", escapeString(globalPasswordSet));
        configValues.put("cannotChangePassword", escapeString(cannotChangePassword));
        configValues.put("cannotUnregister", escapeString(cannotUnregister));
        configValues.put("notAuthenticated", escapeString(notAuthenticated));
        configValues.put("alreadyAuthenticated", escapeString(alreadyAuthenticated));
        configValues.put("successfullyAuthenticated", escapeString(successfullyAuthenticated));
        configValues.put("successfulLogout", escapeString(successfulLogout));
        configValues.put("timeExpired", escapeString(timeExpired));
        configValues.put("registerRequired", escapeString(registerRequired));
        configValues.put("alreadyRegistered", escapeString(alreadyRegistered));
        configValues.put("registerSuccess", escapeString(registerSuccess));
        configValues.put("userdataDeleted", escapeString(userdataDeleted));
        configValues.put("userdataUpdated", escapeString(userdataUpdated));
        configValues.put("accountDeleted", escapeString(accountDeleted));
        configValues.put("configurationReloaded", escapeString(configurationReloaded));
        configValues.put("maxPasswordChars", escapeString(maxPasswordChars));
        configValues.put("minPasswordChars", escapeString(minPasswordChars));
        configValues.put("disallowedUsername", escapeString(disallowedUsername));
        configValues.put("playerAlreadyOnline", escapeString(playerAlreadyOnline));
        configValues.put("worldSpawnSet", escapeString(worldSpawnSet));
        configValues.put("corruptedPlayerData", escapeString(corruptedPlayerData));
        configValues.put("userNotRegistered", escapeString(userNotRegistered));
        configValues.put("cannotLogout", escapeString(cannotLogout));
        configValues.put("offlineUuid", escapeString(offlineUuid));
        configValues.put("registeredPlayers", escapeString(registeredPlayers));
        configValues.put("addToForcedOffline", escapeString(addToForcedOffline));

        String configTemplate = Resources.toString(getResource("config/" + getConfigPath()), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }
}
