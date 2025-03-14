package xyz.nikitacartes.easyauth.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import xyz.nikitacartes.easyauth.config.TelegramConfigV1;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Telegram bot for EasyAuth integration
 */
public class EasyAuthBot extends TelegramLongPollingBot {
    
    private final TelegramManager telegramManager;
    private final TelegramConfigV1 config;
    private final String botUsername;
    
    /**
     * Creates a new EasyAuth Telegram bot
     * 
     * @param telegramManager The Telegram manager to use
     * @param config The Telegram configuration
     */
    public EasyAuthBot(TelegramManager telegramManager, TelegramConfigV1 config) {
        super(config.botToken);
        this.telegramManager = telegramManager;
        this.config = config;
        this.botUsername = getBotUsername();
        LogInfo("Initialized EasyAuth Telegram bot with username: " + botUsername);
    }
    
    @Override
    public String getBotUsername() {
        try {
            if (botUsername != null) {
                return botUsername;
            }
            
            // Если имя бота не было получено ранее, пытаемся получить его через getMe()
            return getMe().getUserName();
        } catch (TelegramApiException e) {
            LogError("Failed to get bot username", e);
            return "EasyAuthBot"; // Возвращаем дефолтное имя в случае ошибки
        }
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        LogInfo("Received update from Telegram: " + update);
        try {
            if (update.hasMessage()) {
                LogInfo("Update contains message: " + update.getMessage().getText());
                handleIncomingMessage(update.getMessage());
            } else {
                LogInfo("Update does not contain a message");
            }
        } catch (Exception e) {
            LogError("Error processing update", e);
        }
    }
    
    /**
     * Handles an incoming message
     * 
     * @param message The message to handle
     */
    private void handleIncomingMessage(Message message) {
        String text = message.getText();
        User sender = message.getFrom();
        long chatId = message.getChatId();
        
        if (text == null) {
            return;
        }
        
        LogDebug("Received message from " + sender.getUserName() + " (" + sender.getId() + "): " + text);
        
        // Обрабатываем команды
        if (text.startsWith("/start")) {
            handleStartCommand(chatId, sender);
        } else if (text.startsWith("/help")) {
            handleHelpCommand(chatId);
        } else if (text.matches("^[A-Z0-9]{" + config.code.length + "}$")) {
            // Предполагаем, что это код подтверждения, если он соответствует формату
            handleVerificationCode(chatId, sender, text);
        } else {
            // Неизвестная команда или текст
            sendMessage(chatId, "Пожалуйста, используйте команду /help для получения списка доступных команд" +
                       " или введите код подтверждения для привязки аккаунта.");
        }
    }
    
    /**
     * Handles the /start command
     * 
     * @param chatId The chat ID to reply to
     * @param sender The user who sent the command
     */
    private void handleStartCommand(long chatId, User sender) {
        StringBuilder message = new StringBuilder();
        message.append("👋 Добро пожаловать в EasyAuth Telegram Integration!\n\n");
        message.append("Этот бот позволяет привязать ваш аккаунт Minecraft к Telegram.\n");
        message.append("Для привязки используйте команду /telegram link в игре, чтобы получить код подтверждения, ");
        message.append("а затем отправьте этот код мне.\n\n");
        message.append("Используйте /help для получения дополнительной информации.");
        
        sendMessage(chatId, message.toString());
    }
    
    /**
     * Handles the /help command
     * 
     * @param chatId The chat ID to reply to
     */
    private void handleHelpCommand(long chatId) {
        StringBuilder message = new StringBuilder();
        message.append("📋 Доступные команды:\n\n");
        message.append("• Используйте команду /telegram link в игре, чтобы получить код привязки\n");
        message.append("• Отправьте этот код мне для привязки вашего аккаунта\n");
        message.append("• Используйте команду /telegram status в игре, чтобы проверить статус привязки\n");
        message.append("• Используйте команду /telegram unlink в игре, чтобы отвязать аккаунт\n\n");
        message.append("После привязки вы сможете получать уведомления о входе в ваш аккаунт.");
        
        sendMessage(chatId, message.toString());
    }
    
    /**
     * Handles a verification code
     * 
     * @param chatId The chat ID to reply to
     * @param sender The user who sent the code
     * @param code The verification code
     */
    private void handleVerificationCode(long chatId, User sender, String code) {
        long telegramId = sender.getId();
        String username = sender.getUserName();
        
        LogInfo("Received verification code from user: " + username + " (ID: " + telegramId + ")");
        LogInfo("Code: " + code);
        
        // Проверяем код и привязываем аккаунт
        String linkedUsername = telegramManager.verifyAndLink(code, telegramId);
        
        if (linkedUsername != null) {
            // Код действителен, аккаунт привязан
            LogInfo("Successfully linked account: " + linkedUsername + " to Telegram user: " + username);
            sendMessage(chatId, "✅ Ваш аккаунт Minecraft (" + linkedUsername + ") успешно привязан к Telegram!\n\n" +
                       "Теперь вы будете получать уведомления о попытках входа в ваш аккаунт.");
        } else {
            // Код недействителен или истек
            LogInfo("Failed to verify code: " + code + " for Telegram user: " + username);
            sendMessage(chatId, "❌ Неверный или просроченный код подтверждения.\n\n" +
                       "Пожалуйста, используйте команду /telegram link в игре, чтобы получить новый код, " +
                       "и убедитесь, что вводите его без ошибок.");
        }
    }
    
    /**
     * Sends a message to a chat
     * 
     * @param chatId The chat ID to send the message to
     * @param text The text to send
     */
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LogError("Error sending message to " + chatId, e);
        }
    }
    
    /**
     * Sends a notification to a user
     * 
     * @param telegramId The Telegram user ID to send the notification to
     * @param text The text to send
     * @return true if the notification was sent successfully, false otherwise
     */
    public boolean sendNotification(Long telegramId, String text) {
        if (telegramId == null) {
            return false;
        }
        
        try {
            SendMessage message = new SendMessage();
            message.setChatId(telegramId);
            message.setText(text);
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            LogError("Error sending notification to " + telegramId, e);
            return false;
        }
    }
} 