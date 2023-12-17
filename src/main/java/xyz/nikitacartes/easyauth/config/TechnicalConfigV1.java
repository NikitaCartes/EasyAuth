package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class TechnicalConfigV1 extends ConfigTemplate {

    public String globalPassword = null;
    public ArrayList<String> forcedOfflinePlayers = new ArrayList<>();
    public ArrayList<String> confirmedOnlinePlayers = new ArrayList<>();
    public transient boolean floodgateLoaded = false;

    public TechnicalConfigV1() {
        super("technical.conf");
    }

    public static TechnicalConfigV1 load() {
        TechnicalConfigV1 config = loadConfig(TechnicalConfigV1.class, "technical.conf");
        if (config == null) {
            config = new TechnicalConfigV1();
            config.save();
        }
        if (FabricLoader.getInstance().isModLoaded("floodgate")) {
            config.floodgateLoaded = true;
        }
        return config;
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("globalPassword", wrapIfNecessary(globalPassword));
        configValues.put("forcedOfflinePlayers", wrapIfNecessary(forcedOfflinePlayers));
        configValues.put("confirmedOnlinePlayers", wrapIfNecessary(confirmedOnlinePlayers));
        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }
}
