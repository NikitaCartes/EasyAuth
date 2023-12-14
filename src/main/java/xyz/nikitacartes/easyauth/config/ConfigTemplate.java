package xyz.nikitacartes.easyauth.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.config.LangConfigV1.TranslatableText;

public abstract class ConfigTemplate {
    private transient final Pattern pattern = Pattern.compile("^[^$\"{}\\[\\]:=,+#`^?!@*&\\\\\\s/]+");
    transient final String configPath;

    ConfigTemplate(String configPath) {
        this.configPath = configPath;
    }

    public static <Config extends ConfigTemplate> Config loadConfig(Class<Config> configClass, String configPath) {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(configPath);
        if (Files.exists(path)) {
            final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .defaultOptions(configurationOptions ->
                            configurationOptions.serializers(builder ->
                                    builder.register(TranslatableText.class, TranslatableTextSerializer.INSTANCE)))
                    .path(path).build();
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
        return String.format(Locale.US, "%.4f", string);
    }

    protected String wrapIfNecessary(long string) {
        return String.valueOf(string);
    }

    protected <T extends List<String>> String wrapIfNecessary(T strings) {
        return "[" + strings
                .stream()
                .map(this::wrapIfNecessary)
                .collect(Collectors.joining(",\n  ")) + "]";
    }

    protected String wrapIfNecessary(TranslatableText text) {
        return "{\n\t\"text\": " + wrapIfNecessary(text.fallback) +
                "\n\t\"enabled\": " + text.enabled +
                "\n\t\"serverSide\": " + text.serverSide +
                "\n}";
    }

    protected abstract String handleTemplate() throws IOException;


    static final class TranslatableTextSerializer implements TypeSerializer<TranslatableText> {
        static final TranslatableTextSerializer INSTANCE = new TranslatableTextSerializer();
        private static final String TEXT = "text";
        private static final String ENABLED = "enabled";
        private static final String SERVER_SIDE = "serverSide";

        private <T> String camelCase(T input) throws SerializationException {
            if (!(input instanceof String string)) {
                throw new SerializationException("Key " + input + " should be a string");
            }
            String[] parts = string.split("-");
            StringBuilder result = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                result.append(parts[i].substring(1));
            }
            return result.toString();
        }

        @Override
        public TranslatableText deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final String text = node.node(TEXT).getString("");
            final boolean enabled = node.node(ENABLED).getBoolean(true);
            final boolean serverSide = node.node(SERVER_SIDE).getBoolean(true);

            if (text == null || text.isEmpty()) {
                return new TranslatableText("text.easyauth." + camelCase(node.key()), "", false, serverSide);
            }

            return new TranslatableText("text.easyauth." + camelCase(node.key()), text, enabled, serverSide);
        }

        @Override
        public void serialize(Type type, @Nullable TranslatableText obj, ConfigurationNode node) throws SerializationException {
            if (obj == null || obj.fallback.isEmpty()) {
                node.node(TEXT).set("");
                node.node(ENABLED).set(false);
                node.node(SERVER_SIDE).set(true);
                return;
            }
            node.node(TEXT).set(obj.fallback);
            node.node(ENABLED).set(obj.enabled);
            node.node(SERVER_SIDE).set(obj.serverSide);
        }
    }

}
