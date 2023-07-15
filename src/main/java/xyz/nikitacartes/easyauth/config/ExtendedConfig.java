package xyz.nikitacartes.easyauth.config;

import com.google.common.io.Resources;
import org.apache.commons.text.StringSubstitutor;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@ConfigSerializable
public class ExtendedConfig {

    private static final String configPath = "extended.conf";

    public boolean allowChat = false;
    public boolean allowCommands = false;
    public ArrayList<String> allowedCommands = new ArrayList<>();
    public boolean allowMovement = false;
    public boolean allowBlockInteraction = false;
    public boolean allowEntityInteraction = false;
    public boolean allowBlockBreaking = false;
    public boolean allowEntityAttacking = false;
    public boolean allowItemDropping = false;
    public boolean allowItemMoving = false;
    public boolean allowItemUsing = false;
    public boolean playerInvulnerable = true;
    public boolean playerIgnored = true;
    public int teleportationTimeoutMs = 5;
    public boolean enableAliases = true;
    public boolean tryPortalRescue = true;
    public int minPasswordLength = 4;
    public int maxPasswordLength = -1;
    public String usernameRegexp = "^[a-zA-Z0-9_]{3,16}$";
    public boolean floodgateBypassRegex = true;
    public boolean hidePlayersFromPlayerList = false;
    public boolean preventAnotherLocationKick = true;
    public boolean useBcrypt = false;
    public boolean useSimpleAuthDb = false;
    public boolean forcedOfflineUuid = false;

    public static ExtendedConfig load() {
        Path path = gameDirectory.resolve("config/EasyAuth").resolve(configPath);
        if (Files.exists(path)) {
            final HoconConfigurationLoader loader = HoconConfigurationLoader
                    .builder()
                    .path(path)
                    .build();
            try {
                return loader.load().get(ExtendedConfig.class);
            } catch (ConfigurateException e) {
                throw new RuntimeException("[EasyAuth] Failed to load config file", e);
            }
        } else {
            ExtendedConfig config = new ExtendedConfig();
            config.save();
            return config;
        }
    }

    private String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("allowChat", allowChat);
        configValues.put("allowCommands", allowCommands);
        configValues.put("allowedCommands", allowedCommands.stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(", ")));
        configValues.put("allowMovement", allowMovement);
        configValues.put("allowBlockInteraction", allowBlockInteraction);
        configValues.put("allowEntityInteraction", allowEntityInteraction);
        configValues.put("allowBlockBreaking", allowBlockBreaking);
        configValues.put("allowEntityAttacking", allowEntityAttacking);
        configValues.put("allowItemDropping", allowItemDropping);
        configValues.put("allowItemMoving", allowItemMoving);
        configValues.put("allowItemUsing", allowItemUsing);
        configValues.put("playerInvulnerable", playerInvulnerable);
        configValues.put("playerIgnored", playerIgnored);
        configValues.put("teleportationTimeoutMs", teleportationTimeoutMs);
        configValues.put("enableAliases", enableAliases);
        configValues.put("tryPortalRescue", tryPortalRescue);
        configValues.put("minPasswordLength", minPasswordLength);
        configValues.put("maxPasswordLength", maxPasswordLength);
        configValues.put("usernameRegexp", usernameRegexp);
        configValues.put("floodgateBypassRegex", floodgateBypassRegex);
        configValues.put("hidePlayersFromPlayerList", hidePlayersFromPlayerList);
        configValues.put("preventAnotherLocationKick", preventAnotherLocationKick);
        configValues.put("useBcrypt", useBcrypt);
        configValues.put("useSimpleAuthDb", useSimpleAuthDb);
        configValues.put("forcedOfflineUuid", forcedOfflineUuid);
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


}
