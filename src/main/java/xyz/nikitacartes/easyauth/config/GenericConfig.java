package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public abstract class GenericConfig<Config> {

    private transient final Pattern pattern = Pattern.compile("^[^$\"{}\\[\\]:=,+#`^?!@*&\\\\\\s/]+");
    transient final String configPath;
    private transient final Class<Config> configClass;

    GenericConfig(Class<Config> configClass, String configPath) {
        this.configPath = configPath;
        this.configClass = configClass;
    }

    public Config loadConfig() {
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

    public void save() {
        Path path = gameDirectory.resolve("config/EasyAuth/" + configPath);
        try {
            Files.writeString(path, handleTemplate());
        } catch (IOException e) {
            LogError("Failed to save config file", e);
        }
    }

    private String escapeString(String string) {
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

    protected <T> String wrapIfNecessary(T string) {
        String escapeString = escapeString(String.valueOf(string));
        if (!pattern.matcher(escapeString).matches()) {
            return "\"" + escapeString + "\"";
        } else {
            return escapeString;
        }
    }

    protected String wrapIfNecessary(double string) {
        return String.format("%.4f", string);
    }

    protected <T extends List<String>> String wrapIfNecessary(T strings) {
        return strings
                .stream()
                .map(this::wrapIfNecessary)
                .collect(Collectors.joining(",\n"));
    }

    protected abstract String handleTemplate() throws IOException;

}
