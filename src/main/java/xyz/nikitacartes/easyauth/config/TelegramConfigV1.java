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
 * Telegram integration configuration class
 */
public class TelegramConfigV1 {
    private static final String CONFIG_NAME = "telegram.conf";
    private static HoconConfigurationLoader telegramLoader;
    private static CommentedConfigurationNode telegramRootNode;

    public boolean enabled = false;
    public String botToken = "";
    public Database database = new Database();
    public Code code = new Code();
    public Security security = new Security();
    public Notifications notifications = new Notifications();

    public static class Database {
        public String tableName = "easyauth_links";
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
     * Loads the Telegram configuration file
     * 
     * @return The loaded config
     */
    public static TelegramConfigV1 load() {
        Path configDir = gameDirectory.resolve("config/EasyAuth");
        Path telegramPath = configDir.resolve(CONFIG_NAME);
        telegramLoader = HoconConfigurationLoader.builder().path(telegramPath).build();
        TelegramConfigV1 config = new TelegramConfigV1();

        try {
            if (!Files.exists(telegramPath)) {
                LogWarn("Telegram config file not found, creating default at: " + telegramPath);
                Files.createDirectories(configDir);
                telegramRootNode = telegramLoader.load();
                
                // Create default config
                telegramRootNode.node("enabled").set(false);
                telegramRootNode.node("enabled").comment("Whether Telegram integration is enabled or not");
                
                telegramRootNode.node("bot-token").set("");
                telegramRootNode.node("bot-token").comment("Telegram bot token obtained from BotFather");

                telegramRootNode.node("database", "table-name").set("easyauth_links");
                telegramRootNode.node("database", "table-name").comment("Name of the table in the database");

                telegramRootNode.node("code", "length").set(6);
                telegramRootNode.node("code", "length").comment("Length of the verification code");
                
                telegramRootNode.node("code", "expiration-minutes").set(30);
                telegramRootNode.node("code", "expiration-minutes").comment("How long verification codes are valid for (in minutes)");
                
                telegramRootNode.node("code", "max-daily-attempts").set(5);
                telegramRootNode.node("code", "max-daily-attempts").comment("Maximum number of link attempts per day");

                telegramRootNode.node("security", "link-cooldown-hours").set(24);
                telegramRootNode.node("security", "link-cooldown-hours").comment("How long to wait before resetting link attempts (in hours)");

                telegramRootNode.node("notifications", "enable-login-notifications").set(true);
                telegramRootNode.node("notifications", "enable-login-notifications").comment("Whether to send notifications when a player logs in");
                
                telegramRootNode.node("notifications", "enable-failed-login-notifications").set(true);
                telegramRootNode.node("notifications", "enable-failed-login-notifications").comment("Whether to send notifications when a player fails to log in");
                
                telegramRootNode.node("notifications", "notify-only-on-suspicious-attempts").set(true);
                telegramRootNode.node("notifications", "notify-only-on-suspicious-attempts").comment("Whether to send failed login notifications only when approaching the max attempts limit");

                telegramLoader.save(telegramRootNode);
                LogWarn("Default Telegram config saved");
                telegramRootNode = telegramLoader.load();
            } else {
                LogWarn("Loading existing Telegram config from: " + telegramPath);
                telegramRootNode = telegramLoader.load();
            }

            // Load values from config
            config.enabled = telegramRootNode.node("enabled").getBoolean(false);
            config.botToken = telegramRootNode.node("bot-token").getString("");
            config.database.tableName = telegramRootNode.node("database", "table-name").getString("easyauth_links");
            config.code.length = telegramRootNode.node("code", "length").getInt(6);
            config.code.expirationMinutes = telegramRootNode.node("code", "expiration-minutes").getInt(30);
            config.code.maxDailyAttempts = telegramRootNode.node("code", "max-daily-attempts").getInt(5);
            config.security.cooldownHours = telegramRootNode.node("security", "link-cooldown-hours").getInt(24);
            config.notifications.enableLoginNotifications = telegramRootNode.node("notifications", "enable-login-notifications").getBoolean(true);
            config.notifications.enableFailedLoginNotifications = telegramRootNode.node("notifications", "enable-failed-login-notifications").getBoolean(true);
            config.notifications.notifyOnlyOnSuspiciousAttempts = telegramRootNode.node("notifications", "notify-only-on-suspicious-attempts").getBoolean(true);

            // Debug logging
            LogWarn("Loaded Telegram config with values:");
            LogWarn("- enabled: " + config.enabled);
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
                LogWarn("Telegram integration is enabled but the bot token is empty. Disabling Telegram integration.");
                config.enabled = false;
            }

            if (config.code.length < 4) {
                LogWarn("Telegram code length is too short. Using minimum length of 4.");
                config.code.length = 4;
            } else if (config.code.length > 10) {
                LogWarn("Telegram code length is too long. Using maximum length of 10.");
                config.code.length = 10;
            }

            if (config.code.expirationMinutes < 5) {
                LogWarn("Telegram code expiration is too short. Using minimum of 5 minutes.");
                config.code.expirationMinutes = 5;
            } else if (config.code.expirationMinutes > 1440) { // 24 hours
                LogWarn("Telegram code expiration is too long. Using maximum of 24 hours (1440 minutes).");
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
            EasyLogger.LogError("Error loading telegram config", e);
        }

        return config;
    }

    /**
     * Saves the config to disk
     */
    public void save() {
        try {
            if (telegramRootNode == null) {
                telegramRootNode = telegramLoader.load();
            }

            // Debug logging
            LogWarn("Saving Telegram config with values:");
            LogWarn("- enabled: " + enabled);
            LogWarn("- database.tableName: " + database.tableName);
            LogWarn("- code.length: " + code.length);
            LogWarn("- code.expirationMinutes: " + code.expirationMinutes);
            LogWarn("- code.maxDailyAttempts: " + code.maxDailyAttempts);
            LogWarn("- security.cooldownHours: " + security.cooldownHours);
            LogWarn("- notifications.enableLoginNotifications: " + notifications.enableLoginNotifications);
            LogWarn("- notifications.enableFailedLoginNotifications: " + notifications.enableFailedLoginNotifications);
            LogWarn("- notifications.notifyOnlyOnSuspiciousAttempts: " + notifications.notifyOnlyOnSuspiciousAttempts);

            telegramRootNode.node("enabled").set(enabled);
            telegramRootNode.node("bot-token").set(botToken);
            telegramRootNode.node("database", "table-name").set(database.tableName);
            telegramRootNode.node("code", "length").set(code.length);
            telegramRootNode.node("code", "expiration-minutes").set(code.expirationMinutes);
            telegramRootNode.node("code", "max-daily-attempts").set(code.maxDailyAttempts);
            telegramRootNode.node("security", "link-cooldown-hours").set(security.cooldownHours);
            telegramRootNode.node("notifications", "enable-login-notifications").set(notifications.enableLoginNotifications);
            telegramRootNode.node("notifications", "enable-failed-login-notifications").set(notifications.enableFailedLoginNotifications);
            telegramRootNode.node("notifications", "notify-only-on-suspicious-attempts").set(notifications.notifyOnlyOnSuspiciousAttempts);

            telegramLoader.save(telegramRootNode);
            
            LogWarn("Telegram config saved successfully to " + CONFIG_NAME);
        } catch (IOException e) {
            EasyLogger.LogError("Error saving telegram config", e);
        }
    }
} 