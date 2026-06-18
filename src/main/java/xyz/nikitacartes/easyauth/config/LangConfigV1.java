package xyz.nikitacartes.easyauth.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import xyz.nikitacartes.easyauth.utils.EasyLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.network.chat.Component.translatable;
import static net.minecraft.network.chat.Component.translatableWithFallback;
import static net.minecraft.network.chat.contents.TranslatableContents.NO_ARGS;
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
    public Password password = new Password();
    public Session session = new Session();
    public Registration registration = new Registration();
    public Account account = new Account();
    public Uuid uuid = new Uuid();
    public Admin admin = new Admin();
    public Errors error = new Errors();

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
        loadTranslations(config);
        return config;
    }

    public static LangConfigV1 load() {
        LangConfigV1 config = loadConfig(LangConfigV1.class, "translation.conf");
        if (config == null) {
            LogError("translation.conf was not found, creating new one with default values");
            config = new LangConfigV1();
            config.save();
        }

        loadTranslations(config);
        return config;
    }

    private static void loadTranslations(LangConfigV1 config) {
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

        public void send(CommandSourceStack commandOutput) {
            if (enabled && commandOutput != null) {
                commandOutput.sendSystemMessage(getTranslation());
            }
        }

        public void send(ServerPlayer commandOutput) {
            if (enabled && commandOutput != null) {
                commandOutput.sendSystemMessage(getTranslation());
            }
        }

        public <T extends CommandSource> void send(T commandOutput) {
            if (enabled && commandOutput != null) {
                commandOutput.sendSystemMessage(getTranslation());
            }
        }

        public void send(CommandSourceStack commandOutput, Object... args) {
            if (enabled && commandOutput != null) {
                commandOutput.sendSystemMessage(getTranslation(args));
            }
        }

        public MutableComponent get() {
            if (enabled) {
                return getTranslation();
            } else {
                return Component.literal("");
            }
        }

        public MutableComponent get(Object... args) {
            if (enabled) {
                return getTranslation(args);
            } else {
                return Component.literal("");
            }
        }

        public MutableComponent getNonTranslatable() {
            return getNonTranslatable(NO_ARGS);
        }

        public MutableComponent getNonTranslatable(Object... args) {
            if (enabled) {
                if (!fallback.isEmpty()) {
                    return translatableWithFallback(key, fallback, args);
                } else {
                    return translatableWithFallback(key, translations.get(key), args);
                }
            } else {
                return Component.literal("");
            }
        }

        private MutableComponent getTranslation() {
            return getTranslation(NO_ARGS);
        }

        private MutableComponent getTranslation(Object... args) {
            //? if neoforge {
            /*// server-translations-api has no NeoForge port
            if (!fallback.isEmpty()) {
                return translatableWithFallback(key, fallback, args);
            }
            return translatableWithFallback(key, translations.get(key), args);
            *///?} else {
            if (langConfig.enableServerSideTranslation && serverSide) {
                return translatable(key, args);
            } if (!fallback.isEmpty()) {
                return translatableWithFallback(key, fallback, args);
            } else {
                return translatableWithFallback(key, translations.get(key), args);
            }
            //?}
        }
    }


    @ConfigSerializable
    public static final class Password {
        public TranslatableText enter = new TranslatableText("password.enter");
        public TranslatableText enterNew = new TranslatableText("password.enterNew");
        public TranslatableText incorrect = new TranslatableText("password.incorrect");
        public TranslatableText mismatch = new TranslatableText("password.mismatch");
        public TranslatableText changed = new TranslatableText("password.changed");
        public TranslatableText tooLong = new TranslatableText("password.tooLong");
        public TranslatableText tooShort = new TranslatableText("password.tooShort");
        public TranslatableText cannotChange = new TranslatableText("password.cannotChange");
        public TranslatableText globalSet = new TranslatableText("password.globalSet");
        public TranslatableText globalIncorrect = new TranslatableText("password.globalIncorrect");
    }

    @ConfigSerializable
    public static final class Session {
        public TranslatableText loginRequired = new TranslatableText("session.loginRequired");
        public TranslatableText tooManyAttempts = new TranslatableText("session.tooManyAttempts");
        public TranslatableText notAuthenticated = new TranslatableText("session.notAuthenticated");
        public TranslatableText alreadyAuthenticated = new TranslatableText("session.alreadyAuthenticated");
        public TranslatableText loginSuccess = new TranslatableText("session.loginSuccess");
        public TranslatableText logoutSuccess = new TranslatableText("session.logoutSuccess");
        public TranslatableText cannotLogout = new TranslatableText("session.cannotLogout");
        public TranslatableText timeExpired = new TranslatableText("session.timeExpired");
        public TranslatableText valid = new TranslatableText("session.valid");
        public TranslatableText onlineAccount = new TranslatableText("session.onlineAccount");
    }

    @ConfigSerializable
    public static final class Registration {
        public TranslatableText required = new TranslatableText("registration.required");
        public TranslatableText requiredWithGlobalPassword = new TranslatableText("registration.requiredWithGlobalPassword");
        public TranslatableText alreadyRegistered = new TranslatableText("registration.alreadyRegistered");
        public TranslatableText success = new TranslatableText("registration.success");
        public TranslatableText notRegistered = new TranslatableText("registration.notRegistered");
    }

    @ConfigSerializable
    public static final class Account {
        public TranslatableText cannotUnregister = new TranslatableText("account.cannotUnregister");
        public TranslatableText deleted = new TranslatableText("account.deleted");
        public TranslatableText dataDeleted = new TranslatableText("account.dataDeleted");
        public TranslatableText dataUpdated = new TranslatableText("account.dataUpdated");
        public TranslatableText dataCorrupted = new TranslatableText("account.dataCorrupted");
        public TranslatableText usernameInvalid = new TranslatableText("account.usernameInvalid");
        public TranslatableText usernameCaseMismatch = new TranslatableText("account.usernameCaseMismatch");
        public TranslatableText alreadyOnline = new TranslatableText("account.alreadyOnline");
        public TranslatableText markedSelfOnline = new TranslatableText("account.markedSelfOnline");
        public TranslatableText markSelfOnlineWarning = new TranslatableText("account.markSelfOnlineWarning");
        public TranslatableText onlineNotFound = new TranslatableText("account.onlineNotFound");
    }

    @ConfigSerializable
    public static final class Uuid {
        public TranslatableText offline = new TranslatableText("uuid.offline");
        public TranslatableText forcedSet = new TranslatableText("uuid.forcedSet");
        public TranslatableText forcedCleared = new TranslatableText("uuid.forcedCleared");
        public TranslatableText changed = new TranslatableText("uuid.changed");
        public TranslatableText invalidFormat = new TranslatableText("uuid.invalidFormat");
        public TranslatableText noForced = new TranslatableText("uuid.noForced");
    }

    @ConfigSerializable
    public static final class Admin {
        public TranslatableText configReloaded = new TranslatableText("admin.configReloaded");
        public TranslatableText spawnSet = new TranslatableText("admin.spawnSet");
        public TranslatableText registeredPlayers = new TranslatableText("admin.registeredPlayers");
        public TranslatableText markedOffline = new TranslatableText("admin.markedOffline");
        public TranslatableText markedOnline = new TranslatableText("admin.markedOnline");
        public TranslatableText ipLimitNotify = new TranslatableText("admin.ipLimitNotify");
    }

    @ConfigSerializable
    public static final class Errors {
        public TranslatableText unknown = new TranslatableText("error.unknown");
        public TranslatableText database = new TranslatableText("error.database");
        public TranslatableText mojangUnavailable = new TranslatableText("error.mojangUnavailable");
        public TranslatableText ipLimitExceeded = new TranslatableText("error.ipLimitExceeded");
        public TranslatableText sessionLimitExceeded = new TranslatableText("error.sessionLimitExceeded");
    }
}
