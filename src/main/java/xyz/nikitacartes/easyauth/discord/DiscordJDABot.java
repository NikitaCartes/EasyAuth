package xyz.nikitacartes.easyauth.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import xyz.nikitacartes.easyauth.config.DiscordConfigV1;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Discord bot implementation using JDA
 */
public class DiscordJDABot extends ListenerAdapter {
    private final DiscordManager discordManager;
    private final DiscordConfigV1 config;
    private JDA jda;
    private String botUsername;

    /**
     * Constructor
     * 
     * @param discordManager The Discord manager
     * @param config The Discord configuration
     */
    public DiscordJDABot(DiscordManager discordManager, DiscordConfigV1 config) {
        this.discordManager = discordManager;
        this.config = config;
        initialize();
    }
    
    /**
     * Initialize the JDA bot
     */
    private void initialize() {
        if (config.botToken == null || config.botToken.isEmpty()) {
            LogError("Discord bot token is empty");
            return;
        }
        
        try {
            JDABuilder builder = JDABuilder.createDefault(config.botToken)
                .addEventListeners(this)
                .setActivity(Activity.playing("EasyAuth"))
                .enableIntents(
                    GatewayIntent.GUILD_MESSAGES, 
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                );
                
            jda = builder.build();
            jda.awaitReady(); // Ждем, пока бот подключится
            botUsername = jda.getSelfUser().getName();
            LogInfo("Initialized Discord bot with username: " + botUsername);
        } catch (Exception e) {
            LogError("Error initializing Discord JDA bot", e);
        }
    }
    
    /**
     * Handles incoming messages
     * 
     * @param event The message event
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Игнорируем сообщения от ботов, включая самого себя
        if (event.getAuthor().isBot()) {
            return;
        }
        
        String content = event.getMessage().getContentRaw().trim();
        long authorId = event.getAuthor().getIdLong();
        String authorUsername = event.getAuthor().getName();
        
        LogDebug("Received Discord message from " + authorUsername + " (" + authorId + "): " + content);
        
        // Проверяем, является ли сообщение кодом подтверждения
        if (content.matches("^[A-Z0-9]{" + config.code.length + "}$")) {
            String username = discordManager.verifyAndLink(content, authorId);
            
            if (username != null) {
                // Успешная привязка
                if (event.isFromGuild()) {
                    // Если сообщение из канала на сервере
                    event.getMessage().reply("Successfully linked account **" + username + "** to Discord user **" + authorUsername + "**!").queue();
                } else {
                    // Если личное сообщение
                    event.getMessage().reply("Successfully linked your Minecraft account **" + username + "** to your Discord account!").queue();
                    
                    // Отправляем уведомление в канал сервера, если он настроен
                    if (config.channelId > 0) {
                        TextChannel channel = jda.getTextChannelById(config.channelId);
                        if (channel != null) {
                            channel.sendMessage("User **" + authorUsername + "** successfully linked their account to Minecraft username **" + username + "**!").queue();
                        }
                    }
                }
            } else {
                // Неверный или истекший код
                event.getMessage().reply("Invalid or expired link code. Please generate a new code in the game using `/discord link` command.").queue();
            }
        } else if (content.equalsIgnoreCase("!help") || content.equalsIgnoreCase("/help")) {
            // Отправляем справку
            event.getMessage().reply("To link your Minecraft account with Discord:\n" +
                    "1. Execute the `/discord link` command in the Minecraft game\n" +
                    "2. Send the verification code you received here or in the designated Discord channel\n\n" +
                    "If you have any issues, contact a server administrator.").queue();
        }
    }
    
    /**
     * Sends a message to a user via DM
     * 
     * @param userId The Discord user ID
     * @param content The message content
     * @return true if successful, false otherwise
     */
    public boolean sendDirectMessage(long userId, String content) {
        try {
            jda.retrieveUserById(userId).queue(user -> {
                if (user != null) {
                    user.openPrivateChannel().queue(channel -> {
                        channel.sendMessage(content).queue();
                    });
                }
            });
            return true;
        } catch (Exception e) {
            LogError("Error sending direct message to " + userId, e);
            return false;
        }
    }
    
    /**
     * Sends a message to a channel
     * 
     * @param channelId The channel ID
     * @param content The message content
     * @return true if successful, false otherwise
     */
    public boolean sendChannelMessage(long channelId, String content) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(content).queue();
                return true;
            }
            return false;
        } catch (Exception e) {
            LogError("Error sending channel message to " + channelId, e);
            return false;
        }
    }
    
    /**
     * Shutdown the bot
     */
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            LogInfo("Discord JDA bot shutdown");
        }
    }
    
    /**
     * Get the bot's username
     * 
     * @return The bot's username
     */
    public String getBotUsername() {
        return botUsername != null ? botUsername : "EasyAuthBot";
    }
} 