package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.text.Text.translatableWithFallback;

@ConfigSerializable
public class LangConfigV1 extends ConfigTemplate {

    public boolean enableServerSideTranslation = true;
    public MutableText enterPassword = translatableWithFallback("text.easyauth.enterPassword", "§6You need to enter your password!");
    public MutableText enterNewPassword = translatableWithFallback("text.easyauth.enterNewPassword", "§4You need to enter new password!");
    public MutableText wrongPassword = translatableWithFallback("text.easyauth.wrongPassword", "§4Wrong password!");
    public MutableText matchPassword = translatableWithFallback("text.easyauth.matchPassword", "§6Passwords must match!");
    public MutableText passwordUpdated = translatableWithFallback("text.easyauth.passwordUpdated", "§aYour password was updated successfully!");
    public MutableText loginRequired = translatableWithFallback("text.easyauth.loginRequired", "§cYou are not authenticated!\n§6Use /login, /l to authenticate!");
    public MutableText loginTriesExceeded = translatableWithFallback("text.easyauth.loginTriesExceeded", "§4Too many login tries. Please wait a few minutes and try again.");
    public MutableText globalPasswordSet = translatableWithFallback("text.easyauth.globalPasswordSet", "§aGlobal password was successfully set!");
    public MutableText cannotChangePassword = translatableWithFallback("text.easyauth.cannotChangePassword", "§cYou cannot change password!");
    public MutableText cannotUnregister = translatableWithFallback("text.easyauth.cannotUnregister", "§cYou cannot unregister this account!");
    public MutableText notAuthenticated = translatableWithFallback("text.easyauth.notAuthenticated", "§cYou are not authenticated!\n§6Try with /login, /l or /register.");
    public MutableText alreadyAuthenticated = translatableWithFallback("text.easyauth.alreadyAuthenticated", "§6You are already authenticated.");
    public MutableText successfullyAuthenticated = translatableWithFallback("text.easyauth.successfullyAuthenticated", "§aYou are now authenticated.");
    public MutableText successfulLogout = translatableWithFallback("text.easyauth.successfulLogout", "§aLogged out successfully.");
    public MutableText timeExpired = translatableWithFallback("text.easyauth.timeExpired", "§cTime for authentication has expired.");
    public MutableText registerRequired = translatableWithFallback("text.easyauth.registerRequired", "§6Type /register <password> <password> to claim this account.");
    public MutableText alreadyRegistered = translatableWithFallback("text.easyauth.alreadyRegistered", "§6This account name is already registered!");
    public MutableText registerSuccess = translatableWithFallback("text.easyauth.registerSuccess", "§aYou are now authenticated.");
    public MutableText userdataDeleted = translatableWithFallback("text.easyauth.userdataDeleted", "§aUserdata deleted.");
    public MutableText userdataUpdated = translatableWithFallback("text.easyauth.userdataUpdated", "§aUserdata updated.");
    public MutableText accountDeleted = translatableWithFallback("text.easyauth.accountDeleted", "§aYour account was successfully deleted!");
    public MutableText configurationReloaded = translatableWithFallback("text.easyauth.configurationReloaded", "§aConfiguration file was reloaded successfully.");
    public MutableText maxPasswordChars = translatableWithFallback("text.easyauth.maxPasswordChars", "§6Password can be at most %d characters long!");
    public MutableText minPasswordChars = translatableWithFallback("text.easyauth.minPasswordChars", "§6Password needs to be at least %d characters long!");
    public MutableText disallowedUsername = translatableWithFallback("text.easyauth.disallowedUsername", "§6Invalid username characters! Allowed character regex: %s");
    public MutableText playerAlreadyOnline = translatableWithFallback("text.easyauth.playerAlreadyOnline", "§cPlayer %s is already online!");
    public MutableText worldSpawnSet = translatableWithFallback("text.easyauth.worldSpawnSet", "§aSpawn for logging in was set successfully.");
    public MutableText corruptedPlayerData = translatableWithFallback("text.easyauth.corruptedPlayerData", "§cYour data is probably corrupted. Please contact admin.");
    public MutableText userNotRegistered = translatableWithFallback("text.easyauth.userNotRegistered", "§cThis player is not registered!");
    public MutableText cannotLogout = translatableWithFallback("text.easyauth.cannotLogout", "§cYou cannot logout!");
    public MutableText offlineUuid = translatableWithFallback("text.easyauth.offlineUuid", "Offline UUID for %s is %s");
    public MutableText registeredPlayers = translatableWithFallback("text.easyauth.registeredPlayers", "List of registered players:");
    public MutableText addToForcedOffline = translatableWithFallback("text.easyauth.addToForcedOffline", "Player successfully added into forcedOfflinePlayers list");


    public LangConfigV1() {
        super("translation.conf");
    }

    public static LangConfigV1 load() {
        LangConfigV1 config = loadConfig(LangConfigV1.class, "translation.conf");
        if (config == null) {
            return new LangConfigV1().disableServerSideTranslation();
        } else {
            if (config.enableServerSideTranslation) {
                return config;
            } else {
                return config.disableServerSideTranslation();
            }
        }
    }

    private LangConfigV1 disableServerSideTranslation() {
        enterPassword = getLiteral(enterPassword);
        enterNewPassword = getLiteral(enterNewPassword);
        wrongPassword = getLiteral(wrongPassword);
        matchPassword = getLiteral(matchPassword);
        passwordUpdated = getLiteral(passwordUpdated);
        loginRequired = getLiteral(loginRequired);
        loginTriesExceeded = getLiteral(loginTriesExceeded);
        globalPasswordSet = getLiteral(globalPasswordSet);
        cannotChangePassword = getLiteral(cannotChangePassword);
        cannotUnregister = getLiteral(cannotUnregister);
        notAuthenticated = getLiteral(notAuthenticated);
        alreadyAuthenticated = getLiteral(alreadyAuthenticated);
        successfullyAuthenticated = getLiteral(successfullyAuthenticated);
        successfulLogout = getLiteral(successfulLogout);
        timeExpired = getLiteral(timeExpired);
        registerRequired = getLiteral(registerRequired);
        alreadyRegistered = getLiteral(alreadyRegistered);
        registerSuccess = getLiteral(registerSuccess);
        userdataDeleted = getLiteral(userdataDeleted);
        userdataUpdated = getLiteral(userdataUpdated);
        accountDeleted = getLiteral(accountDeleted);
        configurationReloaded = getLiteral(configurationReloaded);
        maxPasswordChars = getLiteral(maxPasswordChars);
        minPasswordChars = getLiteral(minPasswordChars);
        disallowedUsername = getLiteral(disallowedUsername);
        playerAlreadyOnline = getLiteral(playerAlreadyOnline);
        worldSpawnSet = getLiteral(worldSpawnSet);
        corruptedPlayerData = getLiteral(corruptedPlayerData);
        userNotRegistered = getLiteral(userNotRegistered);
        cannotLogout = getLiteral(cannotLogout);
        offlineUuid = getLiteral(offlineUuid);
        registeredPlayers = getLiteral(registeredPlayers);
        addToForcedOffline = getLiteral(addToForcedOffline);
        return this;
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

    private MutableText getLiteral(MutableText text) {
        TranslatableTextContent content = (TranslatableTextContent) text.getContent();
        if (content.getFallback() == null || content.getFallback().isEmpty()) {
            return null;
        }
        return Text.literal(content.getFallback());
    }

}
