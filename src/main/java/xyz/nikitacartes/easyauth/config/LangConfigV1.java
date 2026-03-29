package xyz.nikitacartes.easyauth.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import xyz.nikitacartes.easyauth.utils.EasyLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.text.Text.translatable;
import static net.minecraft.text.Text.translatableWithFallback;
import static net.minecraft.text.TranslatableTextContent.EMPTY_ARGUMENTS;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@ConfigSerializable
public class LangConfigV1 extends ConfigTemplate {

    @Comment("""
            Enable server-side translation.
            While enabled EasyAuth sends messages, translated to player's client language.
            List of available languages: https://github.com/NikitaCartes/EasyAuth/tree/HEAD/src/main/resources/data/easyauth/lang
            Disabling this option will force EasyAuth to send all messaged from that file.""")
    public boolean enableServerSideTranslation = true;

    @Comment("""
            
            Default language for EasyAuth.
            
            Note: with server-side translation enabled, this language will be used for non-translatable messages
            Note: with disable server-side translation, message from "text" field have higher priority than defaultLanguage.""")
    public String defaultLanguage = "en_us";
    public TranslatableText enterPassword = new TranslatableText("enterPassword");
    public TranslatableText enterNewPassword = new TranslatableText("enterNewPassword");
    public TranslatableText wrongPassword = new TranslatableText("wrongPassword");
    public TranslatableText matchPassword = new TranslatableText("matchPassword");
    public TranslatableText passwordUpdated = new TranslatableText("passwordUpdated");
    public TranslatableText loginRequired = new TranslatableText("loginRequired");
    public TranslatableText loginTriesExceeded = new TranslatableText("loginTriesExceeded");
    public TranslatableText globalPasswordSet = new TranslatableText("globalPasswordSet");
    public TranslatableText cannotChangePassword = new TranslatableText("cannotChangePassword");
    public TranslatableText cannotUnregister = new TranslatableText("cannotUnregister");
    public TranslatableText notAuthenticated = new TranslatableText("notAuthenticated");
    public TranslatableText alreadyAuthenticated = new TranslatableText("alreadyAuthenticated");
    public TranslatableText successfullyAuthenticated = new TranslatableText("successfullyAuthenticated");
    public TranslatableText successfulLogout = new TranslatableText("successfulLogout");
    public TranslatableText timeExpired = new TranslatableText("timeExpired");
    public TranslatableText registerRequired = new TranslatableText("registerRequired");
    public TranslatableText alreadyRegistered = new TranslatableText("alreadyRegistered");
    public TranslatableText registerSuccess = new TranslatableText("registerSuccess");
    public TranslatableText userdataDeleted = new TranslatableText("userdataDeleted");
    public TranslatableText userdataUpdated = new TranslatableText("userdataUpdated");
    public TranslatableText accountDeleted = new TranslatableText("accountDeleted");
    public TranslatableText configurationReloaded = new TranslatableText("configurationReloaded");
    public TranslatableText maxPasswordChars = new TranslatableText("maxPasswordChars");
    public TranslatableText minPasswordChars = new TranslatableText("minPasswordChars");
    public TranslatableText disallowedUsername = new TranslatableText("disallowedUsername");
    public TranslatableText playerAlreadyOnline = new TranslatableText("playerAlreadyOnline");
    public TranslatableText worldSpawnSet = new TranslatableText("worldSpawnSet");
    public TranslatableText corruptedPlayerData = new TranslatableText("corruptedPlayerData");
    public TranslatableText userNotRegistered = new TranslatableText("userNotRegistered");
    public TranslatableText cannotLogout = new TranslatableText("cannotLogout");
    public TranslatableText offlineUuid = new TranslatableText("offlineUuid");
    public TranslatableText registeredPlayers = new TranslatableText("registeredPlayers");
    public TranslatableText validSession = new TranslatableText("validSession");
    public TranslatableText onlinePlayerLogin = new TranslatableText("onlinePlayerLogin");
    public TranslatableText differentUsernameCase = new TranslatableText("differentUsernameCase");
    public TranslatableText wrongGlobalPassword = new TranslatableText("wrongGlobalPassword");
    public TranslatableText registerRequiredWithGlobalPassword = new TranslatableText("registerRequiredWithGlobalPassword");
    public TranslatableText markAsOffline = new TranslatableText("markAsOffline");
    public TranslatableText markAsOnline = new TranslatableText("markAsOnline");
    public TranslatableText selfMarkAsOnline = new TranslatableText("selfMarkAsOnline");
    public TranslatableText selfMarkAsOnlineWarning = new TranslatableText("selfMarkAsOnlineWarning");
    public TranslatableText accountNotFound = new TranslatableText("accountNotFound");
    public TranslatableText accountCheckFailed = new TranslatableText("accountCheckFailed");
    public TranslatableText databaseError = new TranslatableText("databaseError");
    public TranslatableText unknownError = new TranslatableText("unknownError");
    public TranslatableText ipLimitExceeded = new TranslatableText("ipLimitExceeded");
    public TranslatableText ipLimitAdminNotify = new TranslatableText("ipLimitAdminNotify");
    public TranslatableText sessionLimitExceeded = new TranslatableText("sessionLimitExceeded");
    public TranslatableText uuidSet = new TranslatableText("uuidSet");
    public TranslatableText uuidCleared = new TranslatableText("uuidCleared");
    public TranslatableText uuidChanged = new TranslatableText("uuidChanged");
    public TranslatableText invalidUuid = new TranslatableText("invalidUuid");
    public TranslatableText noForcedUuid = new TranslatableText("noForcedUuid");

    private static Map<String, String> translations = new HashMap<>();

    public LangConfigV1() {
        super("translation.conf", """
                ##                             ##
                ##          EasyAuth           ##
                ##  Translation Configuration  ##
                ##                             ##""");
    }

    public static LangConfigV1 create() {
        LangConfigV1 config = loadConfig(LangConfigV1.class, "translation.conf");
        if (config == null) {
            config = new LangConfigV1();
            config.save();
        }
        return config;
    }

    public static LangConfigV1 load() {
        LangConfigV1 config = loadConfig(LangConfigV1.class, "translation.conf");
        if (config == null) {
            LogError("translation.conf was not found, creating new one with default values");
            config = new LangConfigV1();
            config.save();
        }

        ClassLoader classLoader = LangConfigV1.class.getClassLoader();
        InputStream defaultLanguage = classLoader.getResourceAsStream("data/easyauth/lang/" + config.defaultLanguage + ".json");

        if (defaultLanguage == null) {
            LogError("Failed to load default language " + config.defaultLanguage + ".json. Using en_us.json instead.");
            defaultLanguage = classLoader.getResourceAsStream("data/easyauth/lang/en_us.json");
        }

        try (InputStreamReader reader = new InputStreamReader(defaultLanguage)) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();

            translations = gson.fromJson(reader, mapType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load default language", e);
        }

        return config;
    }

    @Override
    public void save() {
        save(LangConfigV1.class, this);
    }

    public static final class TranslatableText {
        private final String key;
        public final String fallback;
        public final boolean enabled;
        public final boolean serverSide;

        public TranslatableText() {
            this.key = null;
            this.fallback = "";
            this.enabled = true;
            this.serverSide = true;
        }

        public TranslatableText(String key) {
            this.key = "text.easyauth." + key;
            this.fallback = "";
            this.enabled = true;
            this.serverSide = true;
        }

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
                commandOutput.sendMessage(getTranslation());
            }
        }

        public void send(ServerPlayerEntity commandOutput) {
            if (enabled && commandOutput != null) {
                commandOutput.sendMessage(getTranslation());
            }
        }

        public <T extends CommandOutput> void send(T commandOutput) {
            if (enabled && commandOutput != null) {
                commandOutput.sendMessage(getTranslation());
            }
        }

        public void send(ServerCommandSource commandOutput, Object... args) {
            if (enabled && commandOutput != null) {
                commandOutput.sendMessage(getTranslation(args));
            }
        }

        public MutableText get() {
            if (enabled) {
                return getTranslation();
            } else {
                return Text.literal("");
            }
        }

        public MutableText get(Object... args) {
            if (enabled) {
                return getTranslation(args);
            } else {
                return Text.literal("");
            }
        }

        public MutableText getNonTranslatable() {
            return getNonTranslatable(EMPTY_ARGUMENTS);
        }

        public MutableText getNonTranslatable(Object... args) {
            if (enabled) {
                if (!fallback.isEmpty()) {
                    return translatableWithFallback(key, fallback, args);
                } else {
                    return translatableWithFallback(key, translations.get(key), args);
                }
            } else {
                return Text.literal("");
            }
        }

        private MutableText getTranslation() {
            return getTranslation(EMPTY_ARGUMENTS);
        }

        private MutableText getTranslation(Object... args) {
            if (langConfig.enableServerSideTranslation && serverSide) {
                return translatable(key, args);
            } if (!fallback.isEmpty()) {
                return translatableWithFallback(key, fallback, args);
            } else {
                return translatableWithFallback(key, translations.get(key), args);
            }
        }
    }

}
