package xyz.nikitacartes.easyauth.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.config.LangConfigV1.TranslatableText;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public abstract class ConfigTemplate {
    transient final String configPath;
    transient final String header;

    ConfigTemplate(String configPath, String header) {
        this.configPath = configPath;
        this.header = header;
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
                throw new RuntimeException("[EasyAuth] Config file " + configPath + " is corrupted. To regenerate it, delete it and the existing main.conf", e);
            }
        } else {
            return null;
        }
    }

    void backup(Path path) {
        try {
            if (Files.exists(path)) {
                Path backupFolder = gameDirectory.resolve("config/EasyAuth/backup");
                if (!Files.exists(backupFolder)) {
                    Files.createDirectories(backupFolder);
                }
                Files.move(path, path.resolveSibling("backup/" + configPath + "." + LocalDateTime.now().toString().replace(":", "-")), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LogError("Failed to save config file", e);
        }
    }

    <Config extends ConfigTemplate> void save(Class<Config> configClass, Config config) {
        Path path = gameDirectory.resolve("config/EasyAuth/" + configPath);
        backup(path);
        final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .defaultOptions(configurationOptions ->
                        configurationOptions
                                .serializers(builder ->
                                        builder.register(LangConfigV1.TranslatableText.class, TranslatableTextSerializer.INSTANCE))
                                .header(header))
                .path(path)
                .build();
        try {
            CommentedConfigurationNode rootNode = loader.load();
            rootNode.set(configClass, config);
            loader.save(rootNode);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    abstract public void save();

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
        public TranslatableText deserialize(@NotNull Type type, ConfigurationNode node) throws SerializationException {
            final String text = node.node(TEXT).getString("");
            final boolean enabled = node.node(ENABLED).getBoolean(true);
            final boolean serverSide = node.node(SERVER_SIDE).getBoolean(true);

            return new TranslatableText("text.easyauth." + camelCase(node.key()), text, enabled, serverSide);
        }

        @Override
        public void serialize(@NotNull Type type, @Nullable TranslatableText obj, @NotNull ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.node(TEXT).set("");
                node.node(ENABLED).set(true);
                node.node(SERVER_SIDE).set(true);
                return;
            }
            node.node(TEXT).set(obj.fallback);
            node.node(ENABLED).set(obj.enabled);
            node.node(SERVER_SIDE).set(obj.serverSide);
        }
    }

}
