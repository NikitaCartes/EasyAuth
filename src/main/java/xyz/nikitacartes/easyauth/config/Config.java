package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

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
            try {
                T config = configClass.getDeclaredConstructor().newInstance();
                config.save();
                return config;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException("[EasyAuth] Failed to create config file", e);
            }
        }
    }

    protected abstract String getConfigPath();

    public void save() {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(getConfigPath());
        try {
            Files.writeString(path, handleTemplate());
        } catch (IOException e) {
            LogError("Failed to save config file", e);
        }
    }

    protected abstract String handleTemplate() throws IOException;

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
}
