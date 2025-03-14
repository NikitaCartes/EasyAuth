package xyz.nikitacartes.easyauth.discord;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.config.DiscordConfigV1;
import xyz.nikitacartes.easyauth.storage.DiscordLinkV1;
import xyz.nikitacartes.easyauth.storage.database.DbApi;
import xyz.nikitacartes.easyauth.storage.database.DBApiException;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static xyz.nikitacartes.easyauth.EasyAuth.getUnixZero;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Manages Discord bot integration
 */
public class DiscordManager {
    private final DiscordConfigV1 config;
    private final DbApi dbApi;
    private Connection connection;
    private final ScheduledExecutorService cleanupExecutor;
    private final SecureRandom random = new SecureRandom();
    private static final String ALLOWED_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    // JDA бот
    private DiscordJDABot jdaBot;

    public DiscordManager(DiscordConfigV1 config, DbApi dbApi) {
        this.config = config;
        this.dbApi = dbApi;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

        // Initialize Discord integration
        if (config.enabled) {
            try {
                initializeDatabase();
                initializeJDABot();
                startCleanupTask();
                LogInfo("Discord integration enabled");
            } catch (Exception e) {
                LogError("Failed to initialize Discord integration", e);
                this.config.enabled = false;
            }
        } else {
            LogInfo("Discord integration disabled");
        }
    }

    /**
     * Initialize the database for Discord links
     */
    private void initializeDatabase() throws Exception {
        try {
            // Используем существующее подключение из DbApi вместо создания нового
            LogInfo("Initializing Discord database using the main EasyAuth database connection");
            
            // Проверяем, что DbApi не закрыто
            if (dbApi.isClosed()) {
                throw new Exception("DbApi connection is closed");
            }
            
            try {
                // Получаем соединение через метод getConnection()
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
                            "`discord_id` BIGINT," +
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
                            "discord_id INTEGER," +
                            "linked_at TEXT," +
                            "link_attempts_today INTEGER," +
                            "attempts_reset_at TEXT," +
                            "json_data TEXT" +
                            ")";
                }
                
                // Используем метод executeRawUpdate для выполнения запроса
                LogInfo("Creating Discord table if not exists: " + config.database.tableName);
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
                    LogError("Failed to create Discord table: " + config.database.tableName);
                    throw new Exception("Table creation failed");
                } else {
                    LogInfo("Discord database table initialized successfully");
                }
            } catch (SQLException | DBApiException e) {
                LogError("Error creating Discord table", e);
                throw new Exception("Failed to create Discord table", e);
            }
        } catch (Exception e) {
            LogError("General error during Discord database initialization", e);
            throw new Exception("Database initialization failed", e);
        }
    }

    /**
     * Initialize the JDA bot
     */
    private void initializeJDABot() {
        if (config.botToken == null || config.botToken.isEmpty()) {
            LogError("Discord bot token is empty");
            return;
        }

        try {
            jdaBot = new DiscordJDABot(this, config);
            LogInfo("JDA Discord bot initialized");
        } catch (Exception e) {
            LogError("Error initializing JDA Discord bot", e);
        }
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            // Эта задача будет удалять истекшие коды привязки из базы данных
            try {
                cleanExpiredCodes();
            } catch (Exception e) {
                LogError("Error cleaning expired codes", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void cleanExpiredCodes() {
        if (!config.enabled || connection == null) {
            return;
        }

        try {
            // Получаем тип базы данных
            String dbType = dbApi.getDatabaseType();
            
            // Формируем запрос в зависимости от типа БД
            String query;
            if (dbType.equals("mysql")) {
                query = "UPDATE `" + config.database.tableName + "` SET code = '', code_expires_at = ? " +
                        "WHERE code_expires_at < ?";
            } else {
                query = "UPDATE " + config.database.tableName + " SET code = '', code_expires_at = ? " +
                        "WHERE code_expires_at < ?";
            }
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                String now = ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME);
                statement.setString(1, getUnixZero().format(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME));
                statement.setString(2, now);
                
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    LogDebug("Cleaned " + updated + " expired Discord verification codes");
                }
            }
        } catch (Exception e) {
            LogError("Error cleaning expired codes", e);
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Остановка JDA бота
        if (jdaBot != null) {
            jdaBot.shutdown();
        }
        
        // Не закрываем connection, так как оно принадлежит DbApi
        // DbApi закроет его самостоятельно при остановке сервера
        LogInfo("Discord manager closed");
    }

    public String generateLinkCode(ServerPlayerEntity player) {
        String username = player.getName().getString();
        
        if (!config.enabled || connection == null) {
            return null;
        }
        
        try {
            // Получаем текущую информацию о пользователе
            DiscordLinkV1 link = getDiscordLink(username);
            
            // Проверяем, не превышено ли число попыток привязки за сегодня
            ZonedDateTime now = ZonedDateTime.now();
            if (link.attemptsResetAt.isBefore(now)) {
                // Если время сброса уже прошло, сбрасываем счетчик попыток
                link.linkAttemptsToday = 0;
                link.attemptsResetAt = now.plusHours(24).truncatedTo(ChronoUnit.DAYS);
            }
            
            if (link.linkAttemptsToday >= config.code.maxDailyAttempts) {
                // Превышено количество попыток за день
                LogInfo("Max daily link attempts exceeded for " + username);
                return null;
            }
            
            // Генерируем новый код
            String code = generateRandomCode();
            link.code = code;
            link.codeExpiresAt = now.plusMinutes(config.code.expirationMinutes);
            link.linkAttemptsToday++;
            
            // Сохраняем изменения в базе
            saveDiscordLink(link);
            
            return code;
        } catch (Exception e) {
            LogError("Error generating link code for " + username, e);
            return null;
        }
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < config.code.length; i++) {
            code.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return code.toString();
    }

    public String verifyAndLink(String code, long discordId) {
        LogInfo("Attempting to verify and link code: " + code + " for Discord ID: " + discordId);
        
        if (!config.enabled) {
            LogInfo("Discord integration is disabled");
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
            String dbType = dbApi.getDatabaseType();
            
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
                        // Найден код в базе
                        String username = resultSet.getString("username");
                        String json = resultSet.getString("json_data");
                        
                        // Загружаем полную информацию о привязке
                        DiscordLinkV1 link = DiscordLinkV1.fromJson(username, json);
                        
                        // Проверяем, не истек ли код
                        if (link.isCodeExpired()) {
                            LogInfo("Code is expired for " + username);
                            return null;
                        }
                        
                        // Обновляем информацию о привязке
                        link.discordId = discordId;
                        link.linkedAt = ZonedDateTime.now();
                        link.code = ""; // Очищаем код, так как он уже использован
                        
                        // Сохраняем обновленную информацию
                        saveDiscordLink(link);
                        
                        LogInfo("Successfully linked " + username + " to Discord ID: " + discordId);
                        return username;
                    } else {
                        LogInfo("Code not found in database: " + code);
                    }
                }
            }
        } catch (SQLException e) {
            LogError("Failed to verify and link Discord account", e);
        }
        
        return null;
    }

    public DiscordLinkV1 getDiscordLink(String username) {
        if (username == null || username.isEmpty() || !config.enabled || connection == null) {
            return new DiscordLinkV1(username);
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
                        return DiscordLinkV1.fromJson(username, json);
                    }
                }
            }
        } catch (SQLException e) {
            LogError("Failed to get Discord link data for " + username, e);
        }
        
        // No record found, create new
        return new DiscordLinkV1(username);
    }
    
    /**
     * Save Discord link data to the database
     * 
     * @param link The DiscordLinkV1 object to save
     * @return true if successful, false otherwise
     */
    public boolean saveDiscordLink(DiscordLinkV1 link) {
        LogInfo("Attempting to save Discord link data for username: " + (link != null ? link.username : "null"));
        
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
                        "` (username, code, code_expires_at, discord_id, linked_at, link_attempts_today, attempts_reset_at, json_data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                // Для SQLite и других БД
                query = "INSERT OR REPLACE INTO " + config.database.tableName + 
                        " (username, code, code_expires_at, discord_id, linked_at, link_attempts_today, attempts_reset_at, json_data) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            }
            
            LogInfo("Executing query: " + query);
            
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, link.username);
                statement.setString(2, link.code);
                statement.setString(3, link.codeExpiresAt.format(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME));
                
                if (link.discordId != null) {
                    statement.setLong(4, link.discordId);
                } else {
                    statement.setNull(4, java.sql.Types.BIGINT);
                }
                
                if (link.linkedAt != null) {
                    statement.setString(5, link.linkedAt.format(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME));
                } else {
                    statement.setNull(5, java.sql.Types.VARCHAR);
                }
                
                statement.setInt(6, link.linkAttemptsToday);
                statement.setString(7, link.attemptsResetAt.format(java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME));
                statement.setString(8, json);
                
                int updated = statement.executeUpdate();
                LogInfo("Updated " + updated + " rows for Discord link: " + link.username);
                
                return updated > 0;
            }
        } catch (SQLException e) {
            LogError("Failed to save Discord link data for " + link.username, e);
            return false;
        }
    }

    public void unlinkAccount(String username) {
        if (username == null || username.isEmpty() || !config.enabled || connection == null) {
            return;
        }
        
        try {
            DiscordLinkV1 link = getDiscordLink(username);
            if (link.isLinked()) {
                link.discordId = null;
                link.linkedAt = null;
                saveDiscordLink(link);
                LogInfo("Unlinked Discord account for " + username);
            }
        } catch (Exception e) {
            LogError("Error unlinking Discord account for " + username, e);
        }
    }

    public boolean isAccountLinked(String username) {
        if (username == null || username.isEmpty() || !config.enabled || connection == null) {
            return false;
        }
        
        DiscordLinkV1 link = getDiscordLink(username);
        return link.isLinked();
    }

    public Long getDiscordId(String username) {
        if (username == null || username.isEmpty() || !config.enabled || connection == null) {
            return null;
        }
        
        DiscordLinkV1 link = getDiscordLink(username);
        return link.discordId;
    }

    /**
     * Sends a notification to a Discord user
     * 
     * @param discordId The Discord user ID
     * @param content The message content
     * @return true if successful, false otherwise
     */
    public boolean sendDirectMessage(long discordId, String content) {
        if (!config.enabled || jdaBot == null) {
            return false;
        }
        
        return jdaBot.sendDirectMessage(discordId, content);
    }
    
    /**
     * Sends a message to a Discord channel
     * 
     * @param channelId The channel ID
     * @param content The message content
     * @return true if successful, false otherwise
     */
    public boolean sendChannelMessage(long channelId, String content) {
        if (!config.enabled || jdaBot == null) {
            return false;
        }
        
        return jdaBot.sendChannelMessage(channelId, content);
    }

    /**
     * Check if Discord integration is enabled
     * 
     * @return true if Discord integration is enabled, false otherwise
     */
    public boolean isEnabled() {
        return config.enabled;
    }

    public String getBotUsername() {
        if (jdaBot != null) {
            return jdaBot.getBotUsername();
        }
        return "EasyAuthBot";
    }
} 