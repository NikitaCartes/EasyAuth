package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MainConfigV1 extends ConfigTemplate {

    public static final int CURRENT_CONFIG_VERSION = 8;

    @Comment("""
            Whether online players should skip the authentication process.
            You have to set online-mode to true in server.properties!
            (cracked players will still be able to enter, but they'll need to log in)""")
    public boolean premiumAutoLogin = true;

    @Comment("""
            
            Consider all players as offline players until they set online status by themselves with /account online command.
            Also this status can be set with /auth markAsOnline/markAsOffline <player> command by admin.
            Enabling this option will prevent "invalid session" errors
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/AutoLogin-Mojang-accounts""")
    public boolean offlineByDefault = false;

    @Comment("""
            
            Whether bedrock players should skip the authentication process.
            You have to set online-mode to true in server.properties!""")
    public boolean floodgateAutoLogin = true;

    @Comment("""
            
            How long to keep session (auto-logging in the player), in seconds.
            Set to -1 to disable.
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/Sessions""")
    public long sessionTimeout = 86400; // 24 hours

    @Comment("""
            
            Maximum login tries before kicking the player from server.
            Set to -1 to allow unlimited, not recommended, however.""")
    public long maxLoginTries = 3;

    @Comment("""
            
            Time in seconds before a player is kicked for not logging in.""")
    public long kickTimeout = 60;

    @Comment("""
            
            Time in seconds player to be allowed back in after kicked for too many login attempts.""")
    public long resetLoginAttemptsTimeout = 120;

    @Comment("""
            
            To login or register, player must use global password or password set by admin.
            Global password should be set with next command: /auth setGlobalPassword <password>
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/Global-password""")
    public boolean enableGlobalPassword = false;

    @Comment("""
            
            Global password can be used only for registration, after that, the player should log in with their own password.
            You need to restart the server to apply changes.
            | enable-global-password | single-use-global-password | Description |
            | true                   | true                       | Global password can be used only for registration, after that, the player should log in with their own password. |
            | true                   | false                      | Registration is disabled, players should log in with global password (or password set by admin).                 |
            | false                  | true / false               | Normal registration/login process, global password is not used.                                                  |""")
    public boolean singleUseGlobalPassword = false;

    @Comment("""
            
            Whether to teleport player to chosen location when joining (to hide original player coordinates).""")
    public boolean hidePlayerCoords = false;

    @Comment("""
            
            Whether to hide player from other players until they are authenticated
            This option requires the Vanish mod to be installed: https://github.com/DrexHD/Vanish""")
    public boolean vanishUntilAuth = true;

    @Comment("""
            
            Location where player will be teleported when joining.
            For more information, see https://github.com/NikitaCartes/EasyAuth/wiki/Coordinate-Hiding""")
    public WorldSpawn worldSpawn = new WorldSpawn();

    @Comment("""
            
            Debug mode. Prints more information to debug.log.""")
    public boolean debug = false;

    @Comment("""
            
            Config Version. Used for automatic migration of config files.
            Do not change this value manually.""")
    public long configVersion = CURRENT_CONFIG_VERSION;

    public MainConfigV1() {
        super("main.conf", """
                ##                          ##
                ##         EasyAuth         ##
                ##    Main Configuration    ##
                ##                          ##""");
    }

    public static MainConfigV1 create() {
        MainConfigV1 config = loadConfig(MainConfigV1.class, "main.conf");
        if (config == null) {
            config = new MainConfigV1();
            config.save();
        }
        return config;
    }

    public static MainConfigV1 load() {
        MainConfigV1 config = loadConfig(MainConfigV1.class, "main.conf");
        if (config == null) {
            throw new RuntimeException("main.conf was not found. How? To regenerate the config files, delete the existing main.conf");
        }
        return config;
    }

    @Override
    public void save() {
        save(MainConfigV1.class, this);
    }

    @ConfigSerializable
    public static class WorldSpawn {
        public String dimension = "minecraft:overworld";
        public double x = 0;
        public double y = 64;
        public double z = 0;
        public float yaw = 0;
        public float pitch = 0;

        public String toString() {
            return String.format("WorldSpawn{dimension=%s, x=%s, y=%s, z=%s, yaw=%s, pitch=%s}", dimension, x, y, z, yaw, pitch);
        }
    }
}
