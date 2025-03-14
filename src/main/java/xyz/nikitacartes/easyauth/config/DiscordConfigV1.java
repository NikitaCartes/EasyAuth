package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import xyz.nikitacartes.easyauth.utils.EasyLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

/**
 * Discord integration configuration class
 */
public class DiscordConfigV1 {
    private static final String CONFIG_NAME = "discord.conf";
    private static HoconConfigurationLoader discordLoader;
    private static CommentedConfigurationNode discordRootNode;

    public boolean enabled = false;
    public String botToken = "";
    public long channelId = 0;
    public String inviteLink = "";
    public Database database = new Database();
    public Code code = new Code();
    public Security security = new Security();
    public Notifications notifications = new Notifications();

    public static class Database {
        public String tableName = "easyauth_discord";
    }

    public static class Code {
        public int length = 6;
        public int expirationMinutes = 30;
        public int maxDailyAttempts = 5;
    }

    public static class Security {
        public int cooldownHours = 24;
    }
    
    public static class Notifications {
        public boolean enableLoginNotifications = true;
        public boolean enableFailedLoginNotifications = true;
        public boolean notifyOnlyOnSuspiciousAttempts = true;
    }

    /**
     * Loads the Discord configuration file
     * 
     * @return The loaded config
     */
    public static DiscordConfigV1 load() {
        Path configDir = gameDirectory.resolve("config/EasyAuth");
        Path discordPath = configDir.resolve(CONFIG_NAME);
        discordLoader = HoconConfigurationLoader.builder().path(discordPath).build();
        DiscordConfigV1 config = new DiscordConfigV1();

        try {
            if (!Files.exists(discordPath)) {
                LogWarn("Discord config file not found, creating default at: " + discordPath);
                Files.createDirectories(configDir);
                discordRootNode = discordLoader.load();
                
                // Create default config
                discordRootNode.node("enabled").set(false);
                discordRootNode.node("enabled").comment("Whether Discord integration is enabled or not");
                
                discordRootNode.node("bot-token").set("");
                discordRootNode.node("bot-token").comment("Discord bot token obtained from Discord Developer Portal");

                discordRootNode.node("channel-id").set(0);
                discordRootNode.node("channel-id").comment("Discord channel ID where the bot will listen for messages");

                discordRootNode.node("invite-link").set("");
                discordRootNode.node("invite-link").comment("Discord server invite link to show to players");

                discordRootNode.node("database", "table-name").set("easyauth_discord");
                discordRootNode.node("database", "table-name").comment("Name of the table in the database");

                discordRootNode.node("code", "length").set(6);
                discordRootNode.node("code", "length").comment("Length of the verification code");
                
                discordRootNode.node("code", "expiration-minutes").set(30);
                discordRootNode.node("code", "expiration-minutes").comment("How long verification codes are valid for (in minutes)");
                
                discordRootNode.node("code", "max-daily-attempts").set(5);
                discordRootNode.node("code", "max-daily-attempts").comment("Maximum number of link attempts per day");

                discordRootNode.node("security", "link-cooldown-hours").set(24);
                discordRootNode.node("security", "link-cooldown-hours").comment("How long to wait before resetting link attempts (in hours)");

                discordRootNode.node("notifications", "enable-login-notifications").set(true);
                discordRootNode.node("notifications", "enable-login-notifications").comment("Whether to send notifications when a player logs in");
                
                discordRootNode.node("notifications", "enable-failed-login-notifications").set(true);
                discordRootNode.node("notifications", "enable-failed-login-notifications").comment("Whether to send notifications when a player fails to log in");
                
                discordRootNode.node("notifications", "notify-only-on-suspicious-attempts").set(true);
                discordRootNode.node("notifications", "notify-only-on-suspicious-attempts").comment("Whether to send failed login notifications only when approaching the max attempts limit");

                discordLoader.save(discordRootNode);
                LogWarn("Default Discord config saved");
                discordRootNode = discordLoader.load();
            } else {
                LogWarn("Loading existing Discord config from: " + discordPath);
                discordRootNode = discordLoader.load();
            }

            // Load values from config
            config.enabled = discordRootNode.node("enabled").getBoolean(false);
            config.botToken = discordRootNode.node("bot-token").getString("");
            config.channelId = discordRootNode.node("channel-id").getLong(0);
            config.inviteLink = discordRootNode.node("invite-link").getString("");
            config.database.tableName = discordRootNode.node("database", "table-name").getString("easyauth_discord");
            config.code.length = discordRootNode.node("code", "length").getInt(6);
            config.code.expirationMinutes = discordRootNode.node("code", "expiration-minutes").getInt(30);
            config.code.maxDailyAttempts = discordRootNode.node("code", "max-daily-attempts").getInt(5);
            config.security.cooldownHours = discordRootNode.node("security", "link-cooldown-hours").getInt(24);
            config.notifications.enableLoginNotifications = discordRootNode.node("notifications", "enable-login-notifications").getBoolean(true);
            config.notifications.enableFailedLoginNotifications = discordRootNode.node("notifications", "enable-failed-login-notifications").getBoolean(true);
            config.notifications.notifyOnlyOnSuspiciousAttempts = discordRootNode.node("notifications", "notify-only-on-suspicious-attempts").getBoolean(true);

            // Debug logging
            LogWarn("Loaded Discord config with values:");
            LogWarn("- enabled: " + config.enabled);
            LogWarn("- channel-id: " + config.channelId);
            LogWarn("- invite-link: " + (!config.inviteLink.isEmpty() ? "set" : "empty"));
            LogWarn("- database.tableName: " + config.database.tableName);
            LogWarn("- code.length: " + config.code.length);
            LogWarn("- code.expirationMinutes: " + config.code.expirationMinutes);
            LogWarn("- code.maxDailyAttempts: " + config.code.maxDailyAttempts);
            LogWarn("- security.cooldownHours: " + config.security.cooldownHours);
            LogWarn("- notifications.enableLoginNotifications: " + config.notifications.enableLoginNotifications);
            LogWarn("- notifications.enableFailedLoginNotifications: " + config.notifications.enableFailedLoginNotifications);
            LogWarn("- notifications.notifyOnlyOnSuspiciousAttempts: " + config.notifications.notifyOnlyOnSuspiciousAttempts);

            // Validate settings
            if (config.enabled && config.botToken.isEmpty()) {
                LogWarn("Discord integration is enabled but the bot token is empty. Disabling Discord integration.");
                config.enabled = false;
            }

            if (config.enabled && config.channelId == 0) {
                LogWarn("Discord integration is enabled but no channel ID is set. Disabling Discord integration.");
                config.enabled = false;
            }

            if (config.code.length < 4) {
                LogWarn("Discord code length is too short. Using minimum length of 4.");
                config.code.length = 4;
            } else if (config.code.length > 10) {
                LogWarn("Discord code length is too long. Using maximum length of 10.");
                config.code.length = 10;
            }

            if (config.code.expirationMinutes < 5) {
                LogWarn("Discord code expiration is too short. Using minimum of 5 minutes.");
                config.code.expirationMinutes = 5;
            } else if (config.code.expirationMinutes > 1440) { // 24 hours
                LogWarn("Discord code expiration is too long. Using maximum of 24 hours (1440 minutes).");
                config.code.expirationMinutes = 1440;
            }

            if (config.code.maxDailyAttempts < 1) {
                LogWarn("Max link attempts is too low. Using minimum of 1.");
                config.code.maxDailyAttempts = 1;
            } else if (config.code.maxDailyAttempts > 20) {
                LogWarn("Max link attempts is too high. Using maximum of 20.");
                config.code.maxDailyAttempts = 20;
            }

            if (config.security.cooldownHours < 1) {
                LogWarn("Link cooldown is too short. Using minimum of 1 hour.");
                config.security.cooldownHours = 1;
            } else if (config.security.cooldownHours > 168) { // 7 days
                LogWarn("Link cooldown is too long. Using maximum of 7 days (168 hours).");
                config.security.cooldownHours = 168;
            }
        } catch (IOException e) {
            EasyLogger.LogError("Error loading discord config", e);
        }

        return config;
    }

    /**
     * Saves the config to disk
     */
    public void save() {
        try {
            if (discordRootNode == null) {
                discordRootNode = discordLoader.load();
            }

            // Debug logging
            LogWarn("Saving Discord config with values:");
            LogWarn("- enabled: " + enabled);
            LogWarn("- channel-id: " + channelId);
            LogWarn("- invite-link: " + (!inviteLink.isEmpty() ? "set" : "empty"));
            LogWarn("- database.tableName: " + database.tableName);
            LogWarn("- code.length: " + code.length);
            LogWarn("- code.expirationMinutes: " + code.expirationMinutes);
            LogWarn("- code.maxDailyAttempts: " + code.maxDailyAttempts);
            LogWarn("- security.cooldownHours: " + security.cooldownHours);
            LogWarn("- notifications.enableLoginNotifications: " + notifications.enableLoginNotifications);
            LogWarn("- notifications.enableFailedLoginNotifications: " + notifications.enableFailedLoginNotifications);
            LogWarn("- notifications.notifyOnlyOnSuspiciousAttempts: " + notifications.notifyOnlyOnSuspiciousAttempts);

            discordRootNode.node("enabled").set(enabled);
            discordRootNode.node("bot-token").set(botToken);
            discordRootNode.node("channel-id").set(channelId);
            discordRootNode.node("invite-link").set(inviteLink);
            discordRootNode.node("database", "table-name").set(database.tableName);
            discordRootNode.node("code", "length").set(code.length);
            discordRootNode.node("code", "expiration-minutes").set(code.expirationMinutes);
            discordRootNode.node("code", "max-daily-attempts").set(code.maxDailyAttempts);
            discordRootNode.node("security", "link-cooldown-hours").set(security.cooldownHours);
            discordRootNode.node("notifications", "enable-login-notifications").set(notifications.enableLoginNotifications);
            discordRootNode.node("notifications", "enable-failed-login-notifications").set(notifications.enableFailedLoginNotifications);
            discordRootNode.node("notifications", "notify-only-on-suspicious-attempts").set(notifications.notifyOnlyOnSuspiciousAttempts);

            discordLoader.save(discordRootNode);
            
            LogWarn("Discord config saved successfully to " + CONFIG_NAME);
        } catch (IOException e) {
            EasyLogger.LogError("Error saving discord config", e);
        }
    }
} 