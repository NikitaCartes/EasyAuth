package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@ConfigSerializable
public class MainConfig {
    private static final String configPath = "main.conf";
    public boolean premiumAutologin = true;
    public boolean floodgateAutoLogin = true;
    public int maxLoginTries = 3;
    public int kickTimeout = 60;
    public int resetLoginAttemptsTimeout = 120;
    public int sessionTimeout = 86400;
    public boolean enableGlobalPassword = false;
    public boolean hidePlayerCoords = false;
    public boolean debug = false;
    public int configVersion = 1;
    public WorldSpawn worldSpawn = new WorldSpawn();


    public static MainConfig load() {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(configPath);
        if (Files.exists(path)) {
            final HoconConfigurationLoader loader = HoconConfigurationLoader
                    .builder()
                    .path(path)
                    .build();
            try {
                return loader.load().get(MainConfig.class);
            } catch (ConfigurateException e) {
                throw new RuntimeException("[EasyAuth] Failed to load config file", e);
            }
        } else {
            MainConfig config = new MainConfig();
            config.save();
            return config;
        }
    }

    private String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("premiumAutologin", premiumAutologin);
        configValues.put("floodgateAutologin", floodgateAutoLogin);
        configValues.put("maxLoginTries", maxLoginTries);
        configValues.put("kickTimeout", kickTimeout);
        configValues.put("resetLoginAttemptsTimeout", resetLoginAttemptsTimeout);
        configValues.put("sessionTimeout", sessionTimeout);
        configValues.put("enableGlobalPassword", enableGlobalPassword);
        configValues.put("hidePlayerCoords", hidePlayerCoords);
        configValues.put("worldSpawn.dimension", worldSpawn.dimension);
        configValues.put("worldSpawn.x", worldSpawn.x);
        configValues.put("worldSpawn.y", worldSpawn.y);
        configValues.put("worldSpawn.z", worldSpawn.z);
        configValues.put("worldSpawn.yaw", worldSpawn.yaw);
        configValues.put("worldSpawn.pitch", worldSpawn.pitch);
        configValues.put("debug", debug);
        configValues.put("configVersion", configVersion);
        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }

    public void save() {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(configPath);
        try {
            Files.writeString(path, handleTemplate());
        } catch (IOException e) {
            LogError("Failed to save config file", e);
        }
    }

    @ConfigSerializable
    public static class WorldSpawn {
        public String dimension = "minecraft:overworld";
        public double x = 0;
        public double y = 64;
        public double z = 0;
        public float yaw;
        public float pitch;
    }
}
