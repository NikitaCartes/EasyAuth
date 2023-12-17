package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class MainConfigV1 extends ConfigTemplate {
    public boolean premiumAutoLogin = true;
    public boolean floodgateAutoLogin = true;
    public long maxLoginTries = 3;
    public long kickTimeout = 60;
    public long resetLoginAttemptsTimeout = 120;
    public long sessionTimeout = 86400;
    public boolean enableGlobalPassword = false;
    public boolean hidePlayerCoords = false;
    public boolean debug = false;
    public long configVersion = 1;
    public WorldSpawn worldSpawn = new WorldSpawn();


    public MainConfigV1() {
        super("main.conf");
    }

    public static MainConfigV1 load() {
        MainConfigV1 config = loadConfig(MainConfigV1.class, "main.conf");
        if (config == null) {
            config = new MainConfigV1();
            config.save();
        }
        return config;
    }

    protected String handleTemplate() throws IOException {
        Map<String, String> configValues = new HashMap<>();
        configValues.put("premiumAutologin", wrapIfNecessary(premiumAutoLogin));
        configValues.put("floodgateAutologin", wrapIfNecessary(floodgateAutoLogin));
        configValues.put("maxLoginTries", wrapIfNecessary(maxLoginTries));
        configValues.put("kickTimeout", wrapIfNecessary(kickTimeout));
        configValues.put("resetLoginAttemptsTimeout", wrapIfNecessary(resetLoginAttemptsTimeout));
        configValues.put("sessionTimeout", wrapIfNecessary(sessionTimeout));
        configValues.put("enableGlobalPassword", wrapIfNecessary(enableGlobalPassword));
        configValues.put("hidePlayerCoords", wrapIfNecessary(hidePlayerCoords));
        configValues.put("worldSpawn.dimension", wrapIfNecessary(worldSpawn.dimension));
        configValues.put("worldSpawn.x", wrapIfNecessary(worldSpawn.x));
        configValues.put("worldSpawn.y", wrapIfNecessary(worldSpawn.y));
        configValues.put("worldSpawn.z", wrapIfNecessary(worldSpawn.z));
        configValues.put("worldSpawn.yaw", wrapIfNecessary(worldSpawn.yaw));
        configValues.put("worldSpawn.pitch", wrapIfNecessary(worldSpawn.pitch));
        configValues.put("debug", wrapIfNecessary(debug));
        configValues.put("configVersion", wrapIfNecessary(configVersion));
        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
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
