package xyz.nikitacartes.easyauth.config;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public abstract class Config {

    public static <T extends Config> T loadConfig(Class<T> configClass, String configPath) {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(configPath);
        if (Files.exists(path)) {
            final HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(path).build();
            try {
                return loader.load().get(configClass);
            } catch (ConfigurateException e) {
                throw new RuntimeException("[EasyAuth] Failed to load config file", e);
            }
        } else {
            return null;
        }
    }

    public static <T extends Config> T createConfig(Class<T> configClass) {
        try {
            T config = configClass.getDeclaredConstructor().newInstance();
            config.save();
            return config;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("[EasyAuth] Failed to create config file", e);
        }
    }

    public static void loadConfigs() {
        File file = new File(gameDirectory + "/config/EasyAuth");
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("[EasyAuth] Error creating directory for configs");
        }
        EasyAuth.config = MainConfigV1.load();
        if (EasyAuth.config == null) {
            EasyAuth.config = MainConfigV1.create();
            EasyAuth.technicalConfig = TechnicalConfigV1.create();
            EasyAuth.langConfig = LangConfigV1.create();
            EasyAuth.extendedConfig = ExtendedConfigV1.create();
            EasyAuth.storageConfig = StorageConfigV1.create();

            ConfigMigration.migrateFromV0();
        } else {
            EasyAuth.technicalConfig = new TechnicalConfigV1();
            EasyAuth.langConfig = LangConfigV1.load();
            EasyAuth.extendedConfig = ExtendedConfigV1.load();
            EasyAuth.storageConfig = StorageConfigV1.load();
        }

        if (EasyAuth.langConfig.enableServerSideTranslation && !FabricLoader.getInstance().isModLoaded("server_translations_api")) {
            EasyAuth.langConfig.enableServerSideTranslation = false;
        }
        AuthEventHandler.usernamePattern = Pattern.compile(EasyAuth.extendedConfig.usernameRegexp);
    }

    public static void saveConfigs() {
        EasyAuth.config.save();
        EasyAuth.technicalConfig.save();
        EasyAuth.langConfig.save();
        EasyAuth.extendedConfig.save();
        EasyAuth.storageConfig.save();
    }

    public void save() {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(getConfigPath());
        try {
            Files.writeString(path, handleTemplate());
        } catch (IOException e) {
            LogError("Failed to save config file", e);
        }
    }

    protected String handleArray(ArrayList<String> strings) {
        return strings.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
    }

    public String escapeString(String string) {
        return string
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\"", "\\\"")
                .replace("'", "\\'");
    }

    protected abstract String getConfigPath();

    protected abstract String handleTemplate() throws IOException;

}
