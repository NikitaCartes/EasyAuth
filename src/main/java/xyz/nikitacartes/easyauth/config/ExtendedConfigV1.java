package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

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
            
            Allow custom payload and custom click action packets to be processed by non-op players (op level 0 or 1) while not logged in.""")
    public boolean allowCustomPacketsForNonOp = false;

    @Comment("""
            
            Allow custom payload and custom click action packets to be processed by all players while not logged in.
            Note: this setting overrides allowCustomPacketsForNonOp.""")
    public boolean allowCustomPackets = false;

    @Comment("""
            
            List of allowed custom packet identifiers for players who are not logged in.
            Works similarly to allowedCommands by checking packet identifier prefix.
            For custom payload packets, use channel identifiers (e.g. voicechat:request_secret).
            With debug logging enabled, you can see declined custom packets in the console with their identifiers.""")
    public List<String> allowedCustomPackets = new ArrayList<>();

    @Comment("""
            
            Allow all packets to be processed while not logged in.
            Note: this setting overrides allowCustomPackets.""")
    public boolean allowAllPackets = false;

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
            This setting is per-player so maximum rate would be (1000/teleportation-timeout-ms) per seconds for each unauthorised player.
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
            
            If true, 'skipAllAuthChecks' does not apply to registered players.""")
    public boolean skipAllAuthChecksNotForRegisteredPlayers = true;

    @Comment("""
            
            If true, 'skipAllAuthChecks' does not apply to operator (op level >=2) players.""")
    public boolean skipAllAuthChecksNotForOperators = true;

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

    @Comment("""
            
            IP Limit Settings - Restrict the number of accounts that can be registered/logged in from the same IP address.""")
    public IpLimitSettings ipLimit = new IpLimitSettings();

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
        AuthEventHandler.usernamePattern = Pattern.compile(config.usernameRegexp);
        return config;
    }

    public static ExtendedConfigV1 load() {
        ExtendedConfigV1 config = loadConfig(ExtendedConfigV1.class, "extended.conf");
        if (config == null) {
            throw new RuntimeException("extended.conf was not found. To regenerate the config files, delete the existing main.conf");
        }
        AuthEventHandler.usernamePattern = Pattern.compile(config.usernameRegexp);
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

    @ConfigSerializable
    public static final class IpLimitSettings {
        @Comment("""
            
            Enable IP-based account limit.
            When enabled, limits the number of accounts that can be registered/logged in from the same IP address.""")
        public boolean enabled = false;

        @Comment("""
            
            Maximum number of accounts allowed per IP address.
            Set to -1 to disable the limit.""")
        public int maxAccountsPerIp = 2;

        @Comment("""
            
            Block registration attempts when the IP limit is exceeded.
            If false, players can still register but admins will be notified.""")
        public boolean blockExcessRegistration = true;

        @Comment("""
            
            Notify admins (players with op level >=3) when a new IP address attempts to exceed the account limit.""")
        public boolean notifyAdmins = true;

        @Comment("""
            
            List of IP addresses that are exempt from the limit (e.g., localhost, trusted IPs).""")
        public ArrayList<String> exemptIps = new ArrayList<>(asList("127.0.0.1", "localhost"));

        @Comment("""
            
            Cache expiry time in seconds for IP account count cache.
            Lower values mean more frequent database queries but more accurate counts.""")
        public int cacheExpirySeconds = 300;

        @Comment("""
            
            Maximum number of concurrent online sessions allowed from the same IP address.
            Set to -1 to disable the limit.
            This check is performed at player join time.""")
        public int maxConcurrentSessionsPerIp = -1;

        @Comment("""
            
            Whether online (premium) players are exempt from the concurrent session limit.
            If true, premium players that auto-login will not be blocked by the session limit.""")
        public boolean exemptOnlinePlayers = false;
    }
}
