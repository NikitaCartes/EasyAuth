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
public class TechnicalConfigV1 extends Config {

    public String globalPassword = null;
    public ArrayList<String> forcedOfflinePlayers = new ArrayList<>();
    public ArrayList<String> confirmedOnlinePlayers = new ArrayList<>();
    public boolean floodgateLoaded = false;

    public static TechnicalConfigV1 load() {
        TechnicalConfigV1 config = loadConfig(TechnicalConfigV1.class, "technical.conf");
        if (config != null) {
            config.floodgateLoaded = FabricLoader.getInstance().isModLoaded("floodgate");
        }
        return config;
    }

    public static TechnicalConfigV1 create() {
        return createConfig(TechnicalConfigV1.class);
    }

    protected String getConfigPath() {
        return "technical.conf";
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("globalPassword", globalPassword == null ? "null" : globalPassword);
        configValues.put("forcedOfflinePlayers", handleArray(forcedOfflinePlayers));
        configValues.put("confirmedOnlinePlayers", handleArray(confirmedOnlinePlayers));
        String configTemplate = Resources.toString(getResource("config/" + getConfigPath()), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }
}
