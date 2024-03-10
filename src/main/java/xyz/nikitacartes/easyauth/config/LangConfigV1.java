package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.text.Text.translatable;
import static net.minecraft.text.Text.translatableWithFallback;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

@ConfigSerializable
public class LangConfigV1 extends ConfigTemplate {

    public boolean enableServerSideTranslation = true;
    public TranslatableText enterPassword = new TranslatableText("text.easyauth.enterPassword", "§6You need to enter your password!");
    public TranslatableText enterNewPassword = new TranslatableText("text.easyauth.enterNewPassword", "§4You need to enter new password!");
    public TranslatableText wrongPassword = new TranslatableText("text.easyauth.wrongPassword", "§4Wrong password!");
    public TranslatableText matchPassword = new TranslatableText("text.easyauth.matchPassword", "§6Passwords must match!");
    public TranslatableText passwordUpdated = new TranslatableText("text.easyauth.passwordUpdated", "§aYour password was updated successfully!");
    public TranslatableText loginRequired = new TranslatableText("text.easyauth.loginRequired", "§cYou are not authenticated!\n§6Use /login, /l to authenticate!");
    public TranslatableText loginTriesExceeded = new TranslatableText("text.easyauth.loginTriesExceeded", "§4Too many login tries. Please wait a few minutes and try again.");
    public TranslatableText globalPasswordSet = new TranslatableText("text.easyauth.globalPasswordSet", "§aGlobal password was successfully set!");
    public TranslatableText cannotChangePassword = new TranslatableText("text.easyauth.cannotChangePassword", "§cYou cannot change password!");
    public TranslatableText cannotUnregister = new TranslatableText("text.easyauth.cannotUnregister", "§cYou cannot unregister this account!");
    public TranslatableText notAuthenticated = new TranslatableText("text.easyauth.notAuthenticated", "§cYou are not authenticated!\n§6Try with /login, /l or /register.");
    public TranslatableText alreadyAuthenticated = new TranslatableText("text.easyauth.alreadyAuthenticated", "§6You are already authenticated.");
    public TranslatableText successfullyAuthenticated = new TranslatableText("text.easyauth.successfullyAuthenticated", "§aYou are now authenticated.");
    public TranslatableText successfulLogout = new TranslatableText("text.easyauth.successfulLogout", "§aLogged out successfully.");
    public TranslatableText timeExpired = new TranslatableText("text.easyauth.timeExpired", "§cTime for authentication has expired.");
    public TranslatableText registerRequired = new TranslatableText("text.easyauth.registerRequired", "§6Type /register <password> <password> to claim this account.");
    public TranslatableText alreadyRegistered = new TranslatableText("text.easyauth.alreadyRegistered", "§6This account name is already registered!");
    public TranslatableText registerSuccess = new TranslatableText("text.easyauth.registerSuccess", "§aYou are now authenticated.");
    public TranslatableText userdataDeleted = new TranslatableText("text.easyauth.userdataDeleted", "§aUserdata deleted.");
    public TranslatableText userdataUpdated = new TranslatableText("text.easyauth.userdataUpdated", "§aUserdata updated.");
    public TranslatableText accountDeleted = new TranslatableText("text.easyauth.accountDeleted", "§aYour account was successfully deleted!");
    public TranslatableText configurationReloaded = new TranslatableText("text.easyauth.configurationReloaded", "§aConfiguration file was reloaded successfully.");
    public TranslatableText maxPasswordChars = new TranslatableText("text.easyauth.maxPasswordChars", "§6Password can be at most %d characters long!");
    public TranslatableText minPasswordChars = new TranslatableText("text.easyauth.minPasswordChars", "§6Password needs to be at least %d characters long!");
    public TranslatableText disallowedUsername = new TranslatableText("text.easyauth.disallowedUsername", "§6Invalid username characters! Allowed character regex: %s");
    public TranslatableText playerAlreadyOnline = new TranslatableText("text.easyauth.playerAlreadyOnline", "§cPlayer %s is already online!");
    public TranslatableText worldSpawnSet = new TranslatableText("text.easyauth.worldSpawnSet", "§aSpawn for logging in was set successfully.");
    public TranslatableText corruptedPlayerData = new TranslatableText("text.easyauth.corruptedPlayerData", "§cYour data is probably corrupted. Please contact admin.");
    public TranslatableText userNotRegistered = new TranslatableText("text.easyauth.userNotRegistered", "§cThis player is not registered!");
    public TranslatableText cannotLogout = new TranslatableText("text.easyauth.cannotLogout", "§cYou cannot logout!");
    public TranslatableText offlineUuid = new TranslatableText("text.easyauth.offlineUuid", "Offline UUID for %s is %s");
    public TranslatableText registeredPlayers = new TranslatableText("text.easyauth.registeredPlayers", "List of registered players:");
    public TranslatableText addToForcedOffline = new TranslatableText("text.easyauth.addToForcedOffline", "Player successfully added into forcedOfflinePlayers list");
    public TranslatableText validSession = new TranslatableText("text.easyauth.validSession", "§aYou have a valid session. No need to log in.");
    public TranslatableText onlinePlayerLogin = new TranslatableText("text.easyauth.onlinePlayerLogin", "§aYou are using an online account. No need to log in.");
    public TranslatableText playerNotOnline = new TranslatableText("text.easyauth.playerNotOnline", "§cThe player you have specified is not online at the moment, cannot force auth.");
    public TranslatableText forceAuthenticatedPlayer = new TranslatableText("text.easyauth.forceAuthenticatedPlayer", "§aThe player was successfully force authenticated.");

    public LangConfigV1() {
        super("translation.conf");
    }

    public static LangConfigV1 load() {
        LangConfigV1 config = loadConfig(LangConfigV1.class, "translation.conf");
        if (config == null) {
            config = new LangConfigV1();
            config.save();
        }
        return config;
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
        configValues.put("validSession", wrapIfNecessary(validSession));
        configValues.put("onlinePlayerLogin", wrapIfNecessary(onlinePlayerLogin));
        configValues.put("playerNotOnline", wrapIfNecessary(playerNotOnline));
        configValues.put("forceAuthenticatedPlayer", wrapIfNecessary(forceAuthenticatedPlayer));

        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }

    public static final class TranslatableText {
        private final String key;
        public final String fallback;
        public final boolean enabled;
        public final boolean serverSide;

        public TranslatableText(String key, String fallback) {
            this.key = key;
            this.fallback = fallback;
            this.enabled = true;
            this.serverSide = true;
        }

        public TranslatableText(String key, String fallback, boolean enabled, boolean serverSide) {
            this.key = key;
            this.fallback = fallback;
            this.enabled = enabled;
            this.serverSide = serverSide;
        }

        public void send(ServerCommandSource commandOutput) {
            if (enabled && commandOutput != null) {
                if (langConfig.enableServerSideTranslation && serverSide) {
                    commandOutput.sendMessage(translatableWithFallback(key, fallback));
                } else {
                    commandOutput.sendMessage(Text.literal(fallback));
                }
            }
        }

        public <T extends CommandOutput> void send(T commandOutput) {
            if (enabled && commandOutput != null) {
                if (langConfig.enableServerSideTranslation && serverSide) {
                    commandOutput.sendMessage(translatableWithFallback(key, fallback));
                } else {
                    commandOutput.sendMessage(Text.literal(fallback));
                }
            }
        }

        public void send(ServerCommandSource commandOutput, Object... args) {
            if (enabled && commandOutput != null) {
                if (langConfig.enableServerSideTranslation && serverSide) {
                    commandOutput.sendMessage(translatableWithFallback(key, fallback, args));
                } else {
                    commandOutput.sendMessage(translatable(fallback, args));
                }
            }
        }

        public MutableText get() {
            if (enabled) {
                if (langConfig.enableServerSideTranslation && serverSide) {
                    return translatableWithFallback(key, fallback);
                } else {
                    return Text.literal(fallback);
                }
            } else {
                return Text.literal("");
            }
        }

        public MutableText get(Object... args) {
            if (enabled) {
                if (langConfig.enableServerSideTranslation && serverSide) {
                    return translatableWithFallback(key, fallback, args);
                } else {
                    return translatable(fallback, args);
                }
            } else {
                return Text.literal("");
            }
        }
    }

}
