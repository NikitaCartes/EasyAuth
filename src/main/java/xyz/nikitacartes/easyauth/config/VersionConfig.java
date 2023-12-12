package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class VersionConfig extends ConfigTemplate {
    public int configVersion = -1;

    public VersionConfig() {
        super("main.conf");
    }

    public static VersionConfig load() {
        VersionConfig config = loadConfig(VersionConfig.class, "main.conf");
        return config != null ? config : new VersionConfig();
    }

    protected String handleTemplate() {
        return null;
    }
}
