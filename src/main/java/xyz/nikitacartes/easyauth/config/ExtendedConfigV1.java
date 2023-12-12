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
public class ExtendedConfigV1 extends ConfigTemplate {
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

    public ExtendedConfigV1() {
        super("extended.conf");
    }

    public static ExtendedConfigV1 load() {
        ExtendedConfigV1 config = loadConfig(ExtendedConfigV1.class, "extended.conf");
        return config != null ? config : new ExtendedConfigV1();
    }

    protected String handleTemplate() throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("allowChat", wrapIfNecessary(allowChat));
        configValues.put("allowCommands", wrapIfNecessary(allowCommands));
        configValues.put("allowedCommands", wrapIfNecessary(allowedCommands));
        configValues.put("allowMovement", wrapIfNecessary(allowMovement));
        configValues.put("allowBlockInteraction", wrapIfNecessary(allowBlockInteraction));
        configValues.put("allowEntityInteraction", wrapIfNecessary(allowEntityInteraction));
        configValues.put("allowBlockBreaking", wrapIfNecessary(allowBlockBreaking));
        configValues.put("allowEntityAttacking", wrapIfNecessary(allowEntityAttacking));
        configValues.put("allowItemDropping", wrapIfNecessary(allowItemDropping));
        configValues.put("allowItemMoving", wrapIfNecessary(allowItemMoving));
        configValues.put("allowItemUsing", wrapIfNecessary(allowItemUsing));
        configValues.put("playerInvulnerable", wrapIfNecessary(playerInvulnerable));
        configValues.put("playerIgnored", wrapIfNecessary(playerIgnored));
        configValues.put("teleportationTimeoutMs", wrapIfNecessary(teleportationTimeoutMs));
        configValues.put("enableAliases", wrapIfNecessary(enableAliases));
        configValues.put("tryPortalRescue", wrapIfNecessary(tryPortalRescue));
        configValues.put("minPasswordLength", wrapIfNecessary(minPasswordLength));
        configValues.put("maxPasswordLength", wrapIfNecessary(maxPasswordLength));
        configValues.put("usernameRegexp", wrapIfNecessary(usernameRegexp));
        configValues.put("floodgateBypassRegex", wrapIfNecessary(floodgateBypassRegex));
        configValues.put("hidePlayersFromPlayerList", wrapIfNecessary(hidePlayersFromPlayerList));
        configValues.put("preventAnotherLocationKick", wrapIfNecessary(preventAnotherLocationKick));
        configValues.put("useBcrypt", wrapIfNecessary(useBcrypt));
        configValues.put("forcedOfflineUuid", wrapIfNecessary(forcedOfflineUuid));
        configValues.put("skipAllAuthChecks", wrapIfNecessary(skipAllAuthChecks));
        String configTemplate = Resources.toString(getResource("config/" + configPath), UTF_8);
        return new StringSubstitutor(configValues).replace(configTemplate);
    }

}
