package xyz.nikitacartes.easyauth.config;

import java.util.List;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;

public class MainConfig {
    public boolean premiumAutologin = true;
    public boolean floodgateAutoLogin = true;
    public int maxLoginTries = 3;
    public int kickTimeout = 60;
    public int resetLoginAttemptsTimeout = 120;
    public int sessionTimeout = 86400;
    public boolean enableGlobalPassword = false;
    public boolean hidePlayerCoords = true;
    public boolean debug = false;
    public int configVersion = 1;

    public static class WorldSpawn {
        public String dimension = "minecraft:overworld";
        public double x = 0;
        public double y = 64;
        public double z = 0;
        public float yaw;
        public float pitch;
    }

    public void saveConfig() {
        ConfigFile mainConfig = new ConfigFile();
        mainConfig.setConfigPath(gameDirectory + "/config/EasyAuth/main.conf");
        mainConfig.setHeader("""
                ##                          ##
                ##         EasyAuth         ##
                ##    Main Configuration    ##
                ##                          ##""");
        mainConfig.addEntry(new ConfigEntry<>("premium-auto-login",
                premiumAutologin,
                List.of("Whether players who have a valid session should skip the authentication process.",
                        "You have to set online-mode to true in server.properties!",
                        "(cracked players will still be able to enter, but they'll need to log in)")));
        mainConfig.addEntry(new ConfigEntry<>("floodgate-auto-login",
                floodgateAutoLogin,
                List.of("Whether bedrock players should skip the authentication process.",
                        "You have to set online-mode to true in server.properties!")));
        mainConfig.addEntry(new ConfigEntry<>("max-login-tries",
                maxLoginTries,
                List.of("Maximum login tries before kicking the player from server.",
                        "Set to -1 to allow unlimited, not recommended, however.")));
        mainConfig.addEntry(new ConfigEntry<>("kick-timeout",
                kickTimeout,
                "Time in seconds before a player is kicked for not logging in."));
        mainConfig.addEntry(new ConfigEntry<>("reset-login-attempts-timeout",
                resetLoginAttemptsTimeout,
                "Time in seconds player to be allowed back in after kicked for too many login attempts."));
        mainConfig.addEntry(new ConfigEntry<>("session-timeout",
                sessionTimeout,
                List.of("How long to keep session (auto-logging in the player), in seconds.",
                        "Set to -1 to disable."),
                "https://github.com/NikitaCartes/EasyAuth/wiki/Sessions"));
        mainConfig.addEntry(new ConfigEntry<>("enable-global-password",
                enableGlobalPassword,
                "Disable registering and force logging in with global password or passwored setted by admin.",
                "https://github.com/NikitaCartes/EasyAuth/wiki/Global-password"));
        mainConfig.addEntry(new ConfigEntry<>("hide-player-coords",
                hidePlayerCoords,
                "Whether to teleport player to choosen location when joining (to hide original player coordinates)."));

        ConfigFile worldSpawn = new ConfigFile();
        worldSpawn.addEntry(new ConfigEntry<>("dimension", "\"minecraft:overworld\""));
        worldSpawn.addEntry(new ConfigEntry<>("x", 0));
        worldSpawn.addEntry(new ConfigEntry<>("y", 64));
        worldSpawn.addEntry(new ConfigEntry<>("z", 0));
        worldSpawn.addEntry(new ConfigEntry<>("yaw", 0));
        worldSpawn.addEntry(new ConfigEntry<>("pitch", 0));

        mainConfig.addEntry(new ConfigEntry<>("world-spawn",
                worldSpawn,
                "Location where player will be teleported when joining.",
                "https://github.com/NikitaCartes/EasyAuth/wiki/Coordinate-Hiding"));
        mainConfig.addEntry(new ConfigEntry<>("debug",
                debug,
                "Debug mode. Prints more information to debug.log."));
        mainConfig.addEntry(new ConfigEntry<>("config-version",
                configVersion,
                List.of("Config Version. Used for automatic migration of config files.",
                        "Do not change this value manually.")));
        mainConfig.writeToFile();
    }
}
