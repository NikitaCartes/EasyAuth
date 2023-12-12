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
public class LangConfigV1 extends ConfigTemplate<LangConfigV1> {

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


    public LangConfigV1() {
        super(LangConfigV1.class, "translation.conf");
        LangConfigV1 temp = loadConfig();
        if (temp != null) {
            this.enableServerSideTranslation = temp.enableServerSideTranslation;
            this.enterPassword = temp.enterPassword;
            this.enterNewPassword = temp.enterNewPassword;
            this.wrongPassword = temp.wrongPassword;
            this.matchPassword = temp.matchPassword;
            this.passwordUpdated = temp.passwordUpdated;
            this.loginRequired = temp.loginRequired;
            this.loginTriesExceeded = temp.loginTriesExceeded;
            this.globalPasswordSet = temp.globalPasswordSet;
            this.cannotChangePassword = temp.cannotChangePassword;
            this.cannotUnregister = temp.cannotUnregister;
            this.notAuthenticated = temp.notAuthenticated;
            this.alreadyAuthenticated = temp.alreadyAuthenticated;
            this.successfullyAuthenticated = temp.successfullyAuthenticated;
            this.successfulLogout = temp.successfulLogout;
            this.timeExpired = temp.timeExpired;
            this.registerRequired = temp.registerRequired;
            this.alreadyRegistered = temp.alreadyRegistered;
            this.registerSuccess = temp.registerSuccess;
            this.userdataDeleted = temp.userdataDeleted;
            this.userdataUpdated = temp.userdataUpdated;
            this.accountDeleted = temp.accountDeleted;
            this.configurationReloaded = temp.configurationReloaded;
            this.maxPasswordChars = temp.maxPasswordChars;
            this.minPasswordChars = temp.minPasswordChars;
            this.disallowedUsername = temp.disallowedUsername;
            this.playerAlreadyOnline = temp.playerAlreadyOnline;
            this.worldSpawnSet = temp.worldSpawnSet;
            this.corruptedPlayerData = temp.corruptedPlayerData;
            this.userNotRegistered = temp.userNotRegistered;
            this.cannotLogout = temp.cannotLogout;
            this.offlineUuid = temp.offlineUuid;
            this.registeredPlayers = temp.registeredPlayers;
            this.addToForcedOffline = temp.addToForcedOffline;
        }
    }


    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("enableServerSideTranslation", wrapIfNecessary(enableServerSideTranslation));
        configValues.put("enterPassword", wrapIfNecessary(enterPassword));
        configValues.put("enterNewPassword", wrapIfNecessary(enterNewPassword));
        configValues.put("wrongPassword", wrapIfNecessary(wrongPassword));
        configValues.put("matchPassword", wrapIfNecessary(matchPassword));
        configValues.put("passwordUpdated", wrapIfNecessary(passwordUpdated));
        configValues.put("loginRequired", wrapIfNecessary(loginRequired));
        configValues.put("loginTriesExceeded", wrapIfNecessary(loginTriesExceeded));
        configValues.put("globalPasswordSet", wrapIfNecessary(globalPasswordSet));
        configValues.put("cannotChangePassword", wrapIfNecessary(cannotChangePassword));
        configValues.put("cannotUnregister", wrapIfNecessary(cannotUnregister));
        configValues.put("notAuthenticated", wrapIfNecessary(notAuthenticated));
        configValues.put("alreadyAuthenticated", wrapIfNecessary(alreadyAuthenticated));
        configValues.put("successfullyAuthenticated", wrapIfNecessary(successfullyAuthenticated));
        configValues.put("successfulLogout", wrapIfNecessary(successfulLogout));
        configValues.put("timeExpired", wrapIfNecessary(timeExpired));
        configValues.put("registerRequired", wrapIfNecessary(registerRequired));
        configValues.put("alreadyRegistered", wrapIfNecessary(alreadyRegistered));
        configValues.put("registerSuccess", wrapIfNecessary(registerSuccess));
        configValues.put("userdataDeleted", wrapIfNecessary(userdataDeleted));
        configValues.put("userdataUpdated", wrapIfNecessary(userdataUpdated));
        configValues.put("accountDeleted", wrapIfNecessary(accountDeleted));
        configValues.put("configurationReloaded", wrapIfNecessary(configurationReloaded));
        configValues.put("maxPasswordChars", wrapIfNecessary(maxPasswordChars));
        configValues.put("minPasswordChars", wrapIfNecessary(minPasswordChars));
        configValues.put("disallowedUsername", wrapIfNecessary(disallowedUsername));
        configValues.put("playerAlreadyOnline", wrapIfNecessary(playerAlreadyOnline));
        configValues.put("worldSpawnSet", wrapIfNecessary(worldSpawnSet));
        configValues.put("corruptedPlayerData", wrapIfNecessary(corruptedPlayerData));
        configValues.put("userNotRegistered", wrapIfNecessary(userNotRegistered));
        configValues.put("cannotLogout", wrapIfNecessary(cannotLogout));
        configValues.put("offlineUuid", wrapIfNecessary(offlineUuid));
        configValues.put("registeredPlayers", wrapIfNecessary(registeredPlayers));
        configValues.put("addToForcedOffline", wrapIfNecessary(addToForcedOffline));

        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }
}
