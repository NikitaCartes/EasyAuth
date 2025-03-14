package xyz.nikitacartes.easyauth.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.nikitacartes.easyauth.config.TelegramConfigV1;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Простой клиент для работы с Telegram Bot API без внешних зависимостей
 */
public class SimpleTelegramClient {
    private final String apiUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TelegramConfigV1 config;
    private String botUsername = null;
    private final TelegramManager telegramManager;
    private final AtomicLong lastUpdateId = new AtomicLong(0);
    private ScheduledExecutorService pollingExecutor;
    
    /**
     * Создает новый клиент для работы с Telegram API
     * @param config Конфигурация Telegram
     * @param telegramManager Менеджер Telegram для обработки кодов
     */
    public SimpleTelegramClient(TelegramConfigV1 config, TelegramManager telegramManager) {
        this.config = config;
        this.telegramManager = telegramManager;
        this.apiUrl = "https://api.telegram.org/bot" + config.botToken;
        initialize();
        startPolling();
    }
    
    /**
     * Инициализирует клиент, получая информацию о боте
     */
    private void initialize() {
        if (config.botToken == null || config.botToken.isEmpty()) {
            LogError("Bot token is empty");
            return;
        }
        
        try {
            JsonNode response = makeRequest("GET", "/getMe", null);
            if (response.has("ok") && response.get("ok").asBoolean()) {
                JsonNode result = response.get("result");
                botUsername = result.get("username").asText();
                LogInfo("Initialized Telegram bot with username: " + botUsername);
            } else {
                LogError("Failed to get bot info: " + response);
            }
        } catch (Exception e) {
            LogError("Error initializing Telegram bot", e);
        }
    }
    
    /**
     * Запускает периодический опрос обновлений Telegram API
     */
    private void startPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor();
        pollingExecutor.scheduleAtFixedRate(this::pollUpdates, 0, 3, TimeUnit.SECONDS);
        LogInfo("Started polling for Telegram updates");
    }
    
    /**
     * Останавливает опрос обновлений
     */
    public void stopPolling() {
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LogInfo("Stopped polling for Telegram updates");
        }
    }
    
    /**
     * Получает обновления от Telegram API
     */
    private void pollUpdates() {
        try {
            String endpoint = "/getUpdates";
            long offset = lastUpdateId.get();
            if (offset > 0) {
                endpoint += "?offset=" + (offset + 1);
            }
            
            // Добавляем параметры для избежания конфликтов
            if (!endpoint.contains("?")) {
                endpoint += "?";
            } else {
                endpoint += "&";
            }
            endpoint += "timeout=60&limit=100";
            
            JsonNode response = makeRequest("GET", endpoint, null);
            LogDebug("Polling response: " + response);
            
            if (response.has("ok") && response.get("ok").asBoolean() && response.has("result")) {
                JsonNode updates = response.get("result");
                
                if (updates.isArray() && updates.size() > 0) {
                    LogInfo("Received " + updates.size() + " updates from Telegram");
                    
                    for (JsonNode update : updates) {
                        long updateId = update.get("update_id").asLong();
                        lastUpdateId.set(Math.max(lastUpdateId.get(), updateId));
                        
                        if (update.has("message")) {
                            JsonNode message = update.get("message");
                            processMessage(message);
                        }
                    }
                }
            } else if (response.has("ok") && !response.get("ok").asBoolean()) {
                LogError("Error polling updates: " + response);
            }
        } catch (Exception e) {
            LogError("Error polling Telegram updates", e);
        }
    }
    
    /**
     * Обрабатывает сообщение от пользователя
     */
    private void processMessage(JsonNode message) {
        try {
            if (!message.has("text") || !message.has("from") || !message.has("chat")) {
                return;
            }
            
            String text = message.get("text").asText();
            JsonNode from = message.get("from");
            long userId = from.get("id").asLong();
            String username = from.has("username") ? from.get("username").asText() : "unknown";
            long chatId = message.get("chat").get("id").asLong();
            
            LogInfo("Received message from " + username + " (ID: " + userId + "): " + text);
            
            if (text.startsWith("/start")) {
                handleStartCommand(chatId, userId, username);
            } else if (text.startsWith("/help")) {
                handleHelpCommand(chatId);
            } else if (text.matches("^[A-Z0-9]{" + config.code.length + "}$")) {
                // Предполагаем, что это код подтверждения, если он соответствует формату
                handleVerificationCode(chatId, userId, username, text);
            } else {
                // Неизвестная команда или текст
                sendMessage(chatId, "Пожалуйста, используйте команду /help для получения списка доступных команд" +
                           " или введите код подтверждения для привязки аккаунта.");
            }
        } catch (Exception e) {
            LogError("Error processing message", e);
        }
    }
    
    /**
     * Обрабатывает команду /start
     */
    private void handleStartCommand(long chatId, long userId, String username) {
        StringBuilder message = new StringBuilder();
        message.append("👋 Добро пожаловать в EasyAuth Telegram Integration!\n\n");
        message.append("Этот бот позволяет привязать ваш аккаунт Minecraft к Telegram.\n");
        message.append("Для привязки используйте команду /telegram link в игре, чтобы получить код подтверждения, ");
        message.append("а затем отправьте этот код мне.\n\n");
        message.append("Используйте /help для получения дополнительной информации.");
        
        sendMessage(chatId, message.toString());
    }
    
    /**
     * Обрабатывает команду /help
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
     * Обрабатывает код подтверждения
     */
    private void handleVerificationCode(long chatId, long userId, String username, String code) {
        LogInfo("Received verification code from user: " + username + " (ID: " + userId + ")");
        LogInfo("Code: " + code);
        
        // Проверяем код и привязываем аккаунт
        String linkedUsername = telegramManager.verifyAndLink(code, userId);
        
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
     * Отправляет сообщение через Telegram API
     * 
     * @param chatId ID чата для отправки
     * @param text Текст сообщения
     * @return true если сообщение отправлено успешно
     */
    public boolean sendMessage(long chatId, String text) {
        if (config.botToken == null || config.botToken.isEmpty()) {
            return false;
        }
        
        try {
            String json = String.format("{\"chat_id\":%d,\"text\":\"%s\"}", 
                    chatId, escapeJson(text));
            
            JsonNode response = makeRequest("POST", "/sendMessage", json);
            return response.has("ok") && response.get("ok").asBoolean();
        } catch (Exception e) {
            LogError("Error sending message to " + chatId, e);
            return false;
        }
    }
    
    /**
     * Выполняет HTTP-запрос к Telegram API
     * 
     * @param method HTTP-метод (GET или POST)
     * @param endpoint Конечная точка API
     * @param jsonBody Тело запроса в формате JSON (для POST)
     * @return JsonNode с ответом от API
     * @throws IOException если произошла ошибка при выполнении запроса
     */
    private JsonNode makeRequest(String method, String endpoint, String jsonBody) throws IOException {
        try {
            URI uri = new URI(apiUrl + endpoint);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            
            if ("POST".equals(method) && jsonBody != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            try {
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return mapper.readTree(connection.getInputStream());
                } else if (responseCode == 409) { // Conflict - другой клиент уже получает обновления
                    LogError("HTTP error 409: Conflict - another instance might be running. Waiting before next poll.");
                    // При ошибке 409 делаем паузу чтобы дать другому клиенту завершить работу
                    try {
                        Thread.sleep(5000); // Пауза 5 секунд
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return mapper.createObjectNode().put("ok", false).put("error_code", 409);
                } else {
                    LogError("HTTP error: " + responseCode);
                    return mapper.createObjectNode();
                }
            } finally {
                connection.disconnect();
            }
        } catch (URISyntaxException e) {
            LogError("Invalid URI: " + apiUrl + endpoint, e);
            return mapper.createObjectNode();
        }
    }
    
    /**
     * Получает имя пользователя бота
     * 
     * @return Имя пользователя бота или "EasyAuthBot" если не удалось получить
     */
    public String getBotUsername() {
        return botUsername != null ? botUsername : "EasyAuthBot";
    }
    
    /**
     * Экранирует специальные символы в JSON
     * 
     * @param text Текст для экранирования
     * @return Экранированный текст
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
} 