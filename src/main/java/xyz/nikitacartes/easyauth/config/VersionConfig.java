package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class VersionConfig extends ConfigTemplate<VersionConfig> {
    public int configVersion = -1;

    public VersionConfig() {
        super(VersionConfig.class, "main.conf");
        VersionConfig temp = loadConfig();
        if (temp != null) {
            this.configVersion = temp.configVersion;
        }
    }

    protected String handleTemplate() {
        return null;
    }
}
