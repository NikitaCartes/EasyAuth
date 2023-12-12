package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigSerializable
public class TechnicalConfigV1 extends ConfigTemplate<TechnicalConfigV1> {

    public String globalPassword = null;
    public ArrayList<String> forcedOfflinePlayers = new ArrayList<>();
    public ArrayList<String> confirmedOnlinePlayers = new ArrayList<>();
    public boolean floodgateLoaded = false;

    public TechnicalConfigV1() {
        super(TechnicalConfigV1.class, "technical.conf");
        TechnicalConfigV1 temp = loadConfig();
        if (temp != null) {
            this.globalPassword = temp.globalPassword;
            this.forcedOfflinePlayers = temp.forcedOfflinePlayers;
            this.confirmedOnlinePlayers = temp.confirmedOnlinePlayers;
            this.floodgateLoaded = temp.floodgateLoaded;
        }
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
