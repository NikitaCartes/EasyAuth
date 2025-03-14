package xyz.nikitacartes.easyauth.telegram;

import xyz.nikitacartes.easyauth.config.TelegramConfigV1;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Manager for the Telegram bot
 */
public class TelegramBotManager {
    private SimpleTelegramClient client;
    private final TelegramManager telegramManager;
    private final TelegramConfigV1 config;
    
    /**
     * Creates a new TelegramBotManager
     * 
     * @param telegramManager The Telegram manager to use
     * @param config The Telegram configuration
     */
    public TelegramBotManager(TelegramManager telegramManager, TelegramConfigV1 config) {
        this.telegramManager = telegramManager;
        this.config = config;
    }
    
    /**
     * Starts the Telegram bot
     * 
     * @return true if the bot was started successfully, false otherwise
     */
    public boolean startBot() {
        if (!config.enabled || config.botToken.isEmpty()) {
            LogInfo("Telegram bot not started - integration is disabled or token is empty");
            return false;
        }
        
        try {
            LogInfo("Starting Telegram bot...");
            client = new SimpleTelegramClient(config, telegramManager);
            LogInfo("Telegram bot started successfully with username: " + client.getBotUsername());
            return true;
        } catch (Exception e) {
            LogError("Failed to start Telegram bot", e);
            return false;
        }
    }
    
    /**
     * Stops the Telegram bot
     */
    public void stopBot() {
        if (client != null) {
            client.stopPolling();
            client = null;
            LogInfo("Telegram bot stopped");
        }
    }
    
    /**
     * Sends a notification to a user via Telegram
     * 
     * @param telegramId The Telegram user ID to send the notification to
     * @param text The text to send
     * @return true if the notification was sent successfully, false otherwise
     */
    public boolean sendNotification(Long telegramId, String text) {
        if (client == null || telegramId == null) {
            return false;
        }
        
        return client.sendMessage(telegramId, text);
    }
    
    /**
     * Gets the username of the Telegram bot
     * 
     * @return The username of the bot or "EasyAuthBot" if the bot is not started
     */
    public String getBotUsername() {
        if (client == null) {
            return "EasyAuthBot";
        }
        
        return client.getBotUsername();
    }
} 