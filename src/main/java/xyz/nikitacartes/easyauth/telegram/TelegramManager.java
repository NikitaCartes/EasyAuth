package xyz.nikitacartes.easyauth.telegram;

import xyz.nikitacartes.easyauth.config.TelegramConfigV1;
import xyz.nikitacartes.easyauth.storage.TelegramLinkV1;
import xyz.nikitacartes.easyauth.storage.database.DbApi;
import xyz.nikitacartes.easyauth.storage.database.DBApiException;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static xyz.nikitacartes.easyauth.EasyAuth.getUnixZero;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

/**
 * Manages Telegram integration
 */
public class TelegramManager {
    private final TelegramConfigV1 config;
    private final DbApi dbApi;
    private Connection connection;
    private final SecureRandom random = new SecureRandom();
    private static final String ALLOWED_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    private TelegramBotManager botManager;

    public TelegramManager(TelegramConfigV1 config, DbApi dbApi) {
        this.config = config;
        this.dbApi = dbApi;

        // Initialize Telegram integration
        if (config.enabled) {
            try {
                initializeDatabase();
                initializeBot();
                LogInfo("Telegram integration enabled");
            } catch (Exception e) {
                LogError("Failed to initialize Telegram integration", e);
                this.config.enabled = false;
            }
        } else {
            LogInfo("Telegram integration disabled");
        }
    }

    /**
     * Initialize the database for Telegram links
     */
    private void initializeDatabase() throws Exception {
        try {
            // Используем существующее подключение из DbApi вместо создания нового
            LogInfo("Initializing Telegram database using the main EasyAuth database connection");
            
            // Проверяем, что DbApi не закрыто
            if (dbApi.isClosed()) {
                throw new Exception("DbApi connection is closed");
            }
            
            try {
                // Получаем соединение через новый метод getConnection()
                connection = dbApi.getConnection();
                
                if (connection == null || connection.isClosed()) {
                    throw new Exception("Failed to get connection from DbApi");
                }
                
                // Определяем тип базы данных из DbApi
                String dbType = dbApi.getDatabaseType();
                LogDebug("Detected database type from DbApi: " + dbType);
                
                // Создаем таблицу, если она не существует
                String createTableQuery;
                
                if (dbType.equals("mysql")) {
                    // MySQL требует другой синтаксис для PRIMARY KEY и поддержки TEXT/VARCHAR
                    createTableQuery = "CREATE TABLE IF NOT EXISTS `" + config.database.tableName + "` (" +
                            "`username` VARCHAR(255) PRIMARY KEY," +
                            "`code` VARCHAR(255)," +
                            "`code_expires_at` VARCHAR(255)," +
                            "`telegram_id` BIGINT," +
                            "`linked_at` VARCHAR(255)," +
                            "`link_attempts_today` INT," +
                            "`attempts_reset_at` VARCHAR(255)," +
                            "`json_data` TEXT" +
                            ")";
                } else {
                    // SQLite и другие базы данных
                    createTableQuery = "CREATE TABLE IF NOT EXISTS " + config.database.tableName + " (" +
                            "username TEXT PRIMARY KEY," +
                            "code TEXT," +
                            "code_expires_at TEXT," +
                            "telegram_id INTEGER," +
                            "linked_at TEXT," +
                            "link_attempts_today INTEGER," +
                            "attempts_reset_at TEXT," +
                            "json_data TEXT" +
                            ")";
                }
                
                // Используем новый метод executeRawUpdate для выполнения запроса
                LogInfo("Creating Telegram table if not exists: " + config.database.tableName);
                int result = dbApi.executeRawUpdate(createTableQuery);
                LogDebug("Create table result: " + result);
                
                // Проверяем существование таблицы
                boolean tableExists = false;
                
                // Проверка существования таблицы
                if (dbType.equals("mysql")) {
                    try (ResultSet rs = connection.getMetaData().getTables(null, null, config.database.tableName, new String[]{"TABLE"})) {
                        tableExists = rs.next();
                    }
                } else {
                    try (ResultSet rs = connection.getMetaData().getTables(null, null, config.database.tableName, null)) {
                        tableExists = rs.next();
                    }
                    
                    if (!tableExists) {
                        // Для некоторых БД имя таблицы может быть регистрозависимым
                        try (ResultSet rs = connection.getMetaData().getTables(null, null, config.database.tableName.toLowerCase(), null)) {
                            tableExists = rs.next();
                        }
                    }
                }
                
                if (!tableExists) {
                    LogError("Failed to create Telegram table: " + config.database.tableName);
                    throw new Exception("Table creation failed");
                } else {
                    LogInfo("Telegram database table initialized successfully");
                }
            } catch (SQLException | DBApiException e) {
                LogError("Error creating Telegram table", e);
                throw new Exception("Failed to create Telegram table", e);
            }
        } catch (Exception e) {
            LogError("General error during Telegram database initialization", e);
            throw new Exception("Database initialization failed", e);
        }
    }
    
    /**
     * Initialize the Telegram bot
     */
    private void initializeBot() {
        if (!config.enabled || config.botToken.isEmpty()) {
            LogInfo("Telegram bot not initialized - integration is disabled or token is empty");
            return;
        }
        
        try {
            botManager = new TelegramBotManager(this, config);
            boolean started = botManager.startBot();
            
            if (started) {
                LogInfo("Telegram bot initialized and started successfully");
            } else {
                LogError("Failed to start Telegram bot");
                this.config.enabled = false;
            }
        } catch (Exception e) {
            LogError("Error initializing Telegram bot", e);
            this.config.enabled = false;
        }
    }
    
    /**
     * Send a notification to a user via Telegram
     * 
     * @param username The username to send the notification to
     * @param text The text to send
     * @return true if the notification was sent successfully, false otherwise
     */
    public boolean sendNotification(String username, String text) {
        if (!config.enabled || botManager == null) {
            return false;
        }
        
        TelegramLinkV1 link = getTelegramLink(username);
        
        if (!link.isLinked()) {
            return false;
        }
        
        return botManager.sendNotification(link.telegramId, text);
    }
    
    /**
     * Generate a verification code for linking with Telegram
     * 
     * @param username The username to generate a code for
     * @return The generated code or null if too many attempts
     */
    public String generateLinkCode(String username) {
        LogInfo("Generating link code for username: " + username);
        
        if (!config.enabled) {
            LogInfo("Telegram integration is disabled");
            return null;
        }

        TelegramLinkV1 link = getTelegramLink(username);
        
        // Check if already linked
        if (link.isLinked()) {
            LogInfo("Account is already linked to Telegram");
            return null;
        }
        
        // Check if too many attempts
        ZonedDateTime now = ZonedDateTime.now();
        if (link.attemptsResetAt.isAfter(now)) {
            if (link.linkAttemptsToday >= config.code.maxDailyAttempts) {
                LogInfo("Too many link attempts today for username: " + username);
                return null;
            }
        } else {
            // Reset attempts if past reset time
            LogInfo("Resetting link attempts for username: " + username);
            link.linkAttemptsToday = 0;
            link.attemptsResetAt = now.plus(config.security.cooldownHours, ChronoUnit.HOURS);
        }
        
        // Generate code
        StringBuilder code = new StringBuilder(config.code.length);
        for (int i = 0; i < config.code.length; i++) {
            int index = random.nextInt(ALLOWED_CHARACTERS.length());
            code.append(ALLOWED_CHARACTERS.charAt(index));
        }
        
        // Update link data
        link.code = code.toString();
        link.codeExpiresAt = now.plus(config.code.expirationMinutes, ChronoUnit.MINUTES);
        link.linkAttemptsToday++;
        
        LogInfo("Generated code: " + link.code + " for username: " + username);
        LogInfo("Code expires at: " + link.codeExpiresAt);
        
        // Save to database
        if (saveTelegramLink(link)) {
            LogInfo("Successfully saved link code to database");
            return link.code;
        } else {
            LogInfo("Failed to save link code to database");
            return null;
        }
    }
    
    /**
     * Unlink a Minecraft account from Telegram
     * 
     * @param username The username to unlink
     * @return true if successful, false otherwise
     */
    public boolean unlinkAccount(String username) {
        if (!config.enabled) {
            return false;
        }
        
        TelegramLinkV1 link = getTelegramLink(username);
        
        // Check if not linked
        if (!link.isLinked()) {
            return false;
        }
        
        // Reset link data
        link.telegramId = null;
        link.linkedAt = null;
        
        // Save to database
        return saveTelegramLink(link);
    }
    
    /**
     * Check if a player is linked to Telegram
     * 
     * @param username The username to check
     * @return true if linked, false otherwise
     */
    public boolean isLinked(String username) {
        if (!config.enabled) {
            return false;
        }
        
        return getTelegramLink(username).isLinked();
    }
    
    /**
     * Get Telegram link data for a player
     * 
     * @param username The username to get data for
     * @return The TelegramLinkV1 object
     */
    public TelegramLinkV1 getTelegramLink(String username) {
        if (username == null || username.isEmpty() || !config.enabled || connection == null) {
            return new TelegramLinkV1(username);
        }
        
        try {
            // Получаем тип базы данных
            String dbType;
            try {
                dbType = dbApi.getDatabaseType();
            } catch (Exception e) {
                // В случае ошибки предполагаем SQLite
                LogDebug("Error getting database type, using SQLite as fallback: " + e.getMessage());
                dbType = "sqlite";
            }
            
            // Формируем запрос в зависимости от типа БД
            String query;
            if (dbType.equals("mysql")) {
                query = "SELECT * FROM `" + config.database.tableName + "` WHERE username = ?";
            } else {
                query = "SELECT * FROM " + config.database.tableName + " WHERE username = ?";
            }
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // Found existing record
                        String json = resultSet.getString("json_data");
                        return TelegramLinkV1.fromJson(username, json);
                    }
                }
            }
        } catch (SQLException e) {
            LogError("Failed to get Telegram link data for " + username, e);
        }
        
        // No record found, create new
        return new TelegramLinkV1(username);
    }
    
    /**
     * Save Telegram link data to the database
     * 
     * @param link The TelegramLinkV1 object to save
     * @return true if successful, false otherwise
     */
    public boolean saveTelegramLink(TelegramLinkV1 link) {
        LogInfo("Attempting to save Telegram link data for username: " + (link != null ? link.username : "null"));
        
        if (link == null || link.username == null || link.username.isEmpty() || !config.enabled || connection == null) {
            LogInfo("Invalid link data or disabled integration");
            return false;
        }
        
        try {
            String json = link.toJson();
            LogInfo("Generated JSON data: " + json);
            
            // Получаем тип базы данных
            String dbType;
            try {
                dbType = dbApi.getDatabaseType();
                LogInfo("Database type: " + dbType);
            } catch (Exception e) {
                LogDebug("Error getting database type, using SQLite as fallback: " + e.getMessage());
                dbType = "sqlite";
            }
            
            // Формируем запрос в зависимости от типа БД
            String query;
            if (dbType.equals("mysql")) {
                // Для MySQL используем REPLACE вместо INSERT OR REPLACE
                query = "REPLACE INTO `" + config.database.tableName + 
                        "` (username, code, code_expires_at, telegram_id, linked_at, link_attempts_today, attempts_reset_at, json_data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                // Для SQLite и других БД
                query = "INSERT OR REPLACE INTO " + config.database.tableName + 
                        " (username, code, code_expires_at, telegram_id, linked_at, link_attempts_today, attempts_reset_at, json_data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            }
            
            LogInfo("Executing query: " + query);
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, link.username);
                statement.setString(2, link.code);
                statement.setString(3, link.codeExpiresAt.toString());
                statement.setObject(4, link.telegramId);
                statement.setObject(5, link.linkedAt != null ? link.linkedAt.toString() : null);
                statement.setInt(6, link.linkAttemptsToday);
                statement.setString(7, link.attemptsResetAt.toString());
                statement.setString(8, json);
                
                int result = statement.executeUpdate();
                LogInfo("Query result: " + result);
                return result > 0;
            }
        } catch (SQLException e) {
            LogError("Failed to save Telegram link data for " + link.username, e);
            return false;
        }
    }
    
    /**
     * Verify and link a Telegram account
     * Meant to be called by the Telegram bot
     * 
     * @param code The verification code
     * @param telegramId The Telegram user ID
     * @return The username if successful, null otherwise
     */
    public String verifyAndLink(String code, long telegramId) {
        LogInfo("Attempting to verify and link code: " + code + " for Telegram ID: " + telegramId);
        
        if (!config.enabled) {
            LogInfo("Telegram integration is disabled");
            return null;
        }
        
        if (code == null || code.isEmpty()) {
            LogInfo("Code is null or empty");
            return null;
        }
        
        if (connection == null) {
            LogInfo("Database connection is null");
            return null;
        }
        
        try {
            // Получаем тип базы данных
            String dbType;
            try {
                dbType = dbApi.getDatabaseType();
                LogInfo("Database type: " + dbType);
            } catch (Exception e) {
                LogDebug("Error getting database type, using SQLite as fallback: " + e.getMessage());
                dbType = "sqlite";
            }
            
            // Формируем запрос в зависимости от типа БД
            String query;
            if (dbType.equals("mysql")) {
                query = "SELECT * FROM `" + config.database.tableName + "` WHERE code = ?";
            } else {
                query = "SELECT * FROM " + config.database.tableName + " WHERE code = ?";
            }
            
            LogInfo("Executing query: " + query + " with code: " + code);
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, code);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        LogInfo("Found matching code in database");
                        String username = resultSet.getString("username");
                        String json = resultSet.getString("json_data");
                        LogInfo("Username from database: " + username);
                        
                        TelegramLinkV1 link = TelegramLinkV1.fromJson(username, json);
                        
                        // Check if code is expired
                        if (link.isCodeExpired()) {
                            LogInfo("Code is expired");
                            return null;
                        }
                        
                        LogInfo("Code is valid, linking account");
                        
                        // Link account
                        link.telegramId = telegramId;
                        link.linkedAt = ZonedDateTime.now();
                        link.code = "";
                        link.codeExpiresAt = getUnixZero();
                        
                        // Save changes
                        if (saveTelegramLink(link)) {
                            LogInfo("Successfully saved link for username: " + username);
                            return username;
                        } else {
                            LogInfo("Failed to save link for username: " + username);
                        }
                    } else {
                        LogInfo("No matching code found in database");
                    }
                }
            }
        } catch (SQLException e) {
            LogError("Failed to verify and link Telegram account", e);
        }
        
        return null;
    }
    
    /**
     * Close the database connection and stop the bot
     */
    public void close() {
        // Останавливаем бота
        if (botManager != null) {
            botManager.stopBot();
            botManager = null;
            LogInfo("Telegram bot manager closed");
        }
        
        // Не закрываем connection, так как оно принадлежит DbApi
        // DbApi закроет его самостоятельно при остановке сервера
        LogInfo("Telegram manager closed");
    }

    /**
     * Check if Telegram integration is enabled
     * 
     * @return true if Telegram integration is enabled, false otherwise
     */
    public boolean isEnabled() {
        return config.enabled;
    }
    
    /**
     * Get the Telegram bot's username
     * 
     * @return The bot's username or "EasyAuthBot" if not available
     */
    public String getBotUsername() {
        if (!config.enabled || botManager == null) {
            return "EasyAuthBot";
        }
        
        return botManager.getBotUsername();
    }
} 