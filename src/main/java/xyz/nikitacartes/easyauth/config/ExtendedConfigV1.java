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
public class ExtendedConfigV1 extends Config {
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
    public long teleportationTimeoutMs = 5;
    public boolean enableAliases = true;
    public boolean tryPortalRescue = true;
    public long minPasswordLength = 4;
    public long maxPasswordLength = -1;
    public String usernameRegexp = "^[a-zA-Z0-9_]{3,16}$";
    public boolean floodgateBypassRegex = true;
    public boolean hidePlayersFromPlayerList = false;
    public boolean preventAnotherLocationKick = true;
    public boolean useBcrypt = false;
    public boolean forcedOfflineUuid = false;
    public boolean skipAllAuthChecks = false;

    public static ExtendedConfigV1 load() {
        return loadConfig(ExtendedConfigV1.class, "extended.conf");
    }

    public static ExtendedConfigV1 create() {
        return createConfig(ExtendedConfigV1.class);
    }

    protected String getConfigPath() {
        return "extended.conf";
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("allowChat", allowChat);
        configValues.put("allowCommands", allowCommands);
        configValues.put("allowedCommands", handleArray(allowedCommands));
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
        configValues.put("forcedOfflineUuid", forcedOfflineUuid);
        configValues.put("skipAllAuthChecks", skipAllAuthChecks);
        String configTemplate = Resources.toString(getResource("config/" + getConfigPath()), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }

}
