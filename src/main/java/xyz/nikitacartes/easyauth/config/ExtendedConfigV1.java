package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;

@ConfigSerializable
public class ExtendedConfigV1 extends ConfigTemplate {

    @Comment("""
            Allow chat messages to be sent by players who are not logged in.""")
    public boolean allowChat = false;

    @Comment("""
            
            Allow players to use all commands while not logged in.""")
    public boolean allowCommands = false;

    @Comment("""
            
            List of allowed commands for players who are not logged in.""")
    public ArrayList<String> allowedCommands = new ArrayList<>();

    @Comment("""
            
            Allow players to move while not logged in.""")
    public boolean allowMovement = false;

    @Comment("""
            
            Allow players to interact with blocks while not logged in.""")
    public boolean allowBlockInteraction = false;

    @Comment("""
            
            Allow "right-clicking" on an entity (e.g. clicking on villagers) by players who are not logged in.""")
    public boolean allowEntityInteraction = false;

    @Comment("""
            
            Allow players to punch blocks while not logged in.""")
    public boolean allowBlockBreaking = false;

    @Comment("""
            
            Allow players to attack entities while not logged in.""")
    public boolean allowEntityAttacking = false;

    @Comment("""
            
            Allow players to drop items while not logged in.""")
    public boolean allowItemDropping = false;

    @Comment("""
            
            Allow players to move items in their inventory while not logged in.""")
    public boolean allowItemMoving = false;

    @Comment("""
            
            Allow players to use items while not logged in.""")
    public boolean allowItemUsing = false;

    @Comment("""
            
            Hide player's inventory from them while not logged in.""")
    public boolean hideInventory = true;

    @Comment("""
            
            If player should be invulnerable before authentication.""")
    public boolean playerInvulnerable = true;

    @Comment("""
            
            If player should be ignored by mobs before authentication.""")
    public boolean playerIgnored = true;

    @Comment("""
            
            Cancellation of packets with player's movement and teleportation back leads to an increase number of these packets.
            That setting limits players teleportation.
            This setting is server-wide so maximum rate would be (1000/teleportation-timeout-ms) per seconds for all unauthorised players.
            Value 0 would effectively disable this setting so players will be teleported after each packet.""")
    public long teleportationTimeoutMs = 20;

    @Comment("""
            
            List of aliases for commands.""")
    public Aliases aliases = new Aliases(true, true);

    @Comment("""
            
            Try to rescue player if they are stuck inside a portal on logging in.
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/Portal-Rescue""")
    public boolean tryPortalRescue = true;

    @Comment("""
            
            Minimum length of a password.""")
    public long minPasswordLength = 4;

    @Comment("""
            
            Maximum length of a password.
            -1 for no limit.""")
    public long maxPasswordLength = -1;

    @Comment("""
            
            Regex for validation of player names.
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/Username-Restriction""")
    public String usernameRegexp = "^[a-zA-Z0-9_]{3,16}$";

    @Comment("""
            
            Allow floodgate players to bypass regex check.""")
    public boolean floodgateBypassRegex = true;

    @Comment("""
            
            Prevents player being kicked because another player with the same name has joined the server.""")
    public boolean preventAnotherLocationKick = true;

    @Comment("""
            
            Whether to modify player uuids to offline style.
            Note: this should be used only if you had your server
            running in offline mode, and you made the switch to use
            AuthConfig#premiumAutoLogin AND your players already
            have e.g. villager discounts, which are based on uuid.
            Other things (advancements, playerdata) are migrated
            automatically, so think before enabling this. In case
            an online-mode player changes username, they'll lose all
            their stuff, unless you migrate it manually.""")
    public boolean forcedOfflineUuid = false;

    @Comment("""
            
            Skip all authentication checks for all players.
            Intended for use with proxies that handle authentication""")
    public boolean skipAllAuthChecks = false;

    @Comment("""
            
            Allow players to join the server with same username as previously registered player, but in different case.""")
    public boolean allowCaseInsensitiveUsername = false;

    @Comment("""
            
            Time in seconds before a player is prompted to authenticate again.""")
    public long authenticationPromptInterval = 10;

    @Comment("""
            
            Connection settings for the Mojang API.""")
    public MojangApiSettings mojangApiSettings = new MojangApiSettings();

    @Comment("""
            
            Log player registration as info level log.""")
    public boolean logPlayerRegistration = false;

    @Comment("""
            
            Log player login as info level log.""")
    public boolean logPlayerLogin = false;

    @Comment("""
            
            Prevent offline players from joining the server using online usernames.""")
    public boolean preventOfflinePlayersWithOnlineUsernames = false;

    @Comment("""
            
            Check offline players with online usernames every time they join the server for online account.""")
    public boolean checkOfflinePlayersWithOnlineUsernames = false;

    public ExtendedConfigV1() {
        super("extended.conf", """
                ##                          ##
                ##         EasyAuth         ##
                ##  Extended Configuration  ##
                ##                          ##""");
    }

    public static ExtendedConfigV1 create() {
        ExtendedConfigV1 config = loadConfig(ExtendedConfigV1.class, "extended.conf");
        if (config == null) {
            config = new ExtendedConfigV1();
            config.save();
        }
        return config;
    }

    public static ExtendedConfigV1 load() {
        ExtendedConfigV1 config = loadConfig(ExtendedConfigV1.class, "extended.conf");
        if (config == null) {
            throw new RuntimeException("Failed to load extended.conf");
        }
        return config;
    }

    @Override
    public void save() {
        save(ExtendedConfigV1.class, this);
    }

    @ConfigSerializable
    public static final class Aliases {
        @Comment("""
            
            `/l` for `/login`""")
        public boolean login = true;

        @Comment("""
            
            `/reg` for `/register`""")
        public boolean register = true;

        public Aliases() {
        }

        public Aliases(boolean login, boolean register) {
            this.login = login;
            this.register = register;
        }
    }

    @ConfigSerializable
    public static final class MojangApiSettings {
        @Comment("""
            
            URL of the Mojang API.""")
        public String url = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";

        @Comment("""
            
            Connection timeout in milliseconds.""")
        public int connectionTimeout = 5000;

        @Comment("""
            
            Read timeout in milliseconds.""")
        public int readTimeout = 5000;
    }
}
