package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.time.ZonedDateTime;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

public class LoginCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> node = registerLogin(dispatcher); // Registering the "/login" command
        if (extendedConfig.aliases.login) {
            dispatcher.register(literal("l")
                    .requires(Permissions.require("easyauth.commands.login", true))
                    .redirect(node));
        }
    }

    public static LiteralCommandNode<ServerCommandSource> registerLogin(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(literal("login")
                .requires(Permissions.require("easyauth.commands.login", true))
                .then(argument("password", string())
                        .executes(ctx -> login(ctx.getSource(), getString(ctx, "password")) // Tries to authenticate user
                        ))
                .executes(ctx -> {
                    langConfig.enterPassword.send(ctx.getSource());
                    return 0;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;
        
        if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        }
        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());

        if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
            LogDebug("Player " + player.getNameForScoreboard() + " provide correct password");
            if (playerData.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
                LogDebug("Player " + player.getNameForScoreboard() + " will be kicked due to kick timeout");
                player.networkHandler.disconnect(langConfig.loginTriesExceeded.get());
                return 0;
            }
            langConfig.successfullyAuthenticated.send(source);
            playerAuth.easyAuth$setAuthenticated(true);
            playerAuth.easyAuth$restoreTrueLocation();
            playerData.lastAuthenticatedDate = ZonedDateTime.now();
            playerData.loginTries = 0;
            playerData.lastIp = playerAuth.easyAuth$getIpAddress();
            playerData.update();
            
            // Отправляем уведомление в Telegram
            final String username = player.getNameForScoreboard();
            if (telegramManager != null && telegramManager.isEnabled() && telegramConfig.notifications.enableLoginNotifications) {
                String ip = playerAuth.easyAuth$getIpAddress();
                String message = String.format("🔐 Выполнен вход в аккаунт!\n\nИмя игрока: %s\nIP-адрес: %s\nВремя: %s", 
                        username, ip, ZonedDateTime.now().toString());
                
                // Отправляем асинхронно
                THREADPOOL.execute(() -> {
                    boolean sent = telegramManager.sendNotification(username, message);
                    if (sent) {
                        LogDebug("Sent Telegram login notification to user: " + username);
                    }
                });
            }
            
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
            return 0;
        } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
            LogDebug("Player " + player.getNameForScoreboard() + " is not registered");
            langConfig.registerRequired.send(source);
            return 0;
        } else {
            LogDebug("Player " + player.getNameForScoreboard() + " provided wrong password");

            playerData.loginTries++;
            if (playerData.loginTries > config.maxLoginTries) {
                LogDebug("Player " + player.getNameForScoreboard() + " failed during authentication too many times");
                langConfig.loginTriesExceeded.send(source);
                playerData.lastKickedDate = ZonedDateTime.now();
                playerData.update();
                
                // Отправляем уведомление в Telegram о превышении лимита попыток входа
                final String playerName = player.getNameForScoreboard();
                if (telegramManager != null && telegramManager.isEnabled() && telegramConfig.notifications.enableFailedLoginNotifications) {
                    String ip = playerAuth.easyAuth$getIpAddress();
                    String message = String.format("⚠️ Превышен лимит попыток входа!\n\nИмя игрока: %s\nIP-адрес: %s\nВремя: %s\nКоличество попыток: %d", 
                            playerName, ip, ZonedDateTime.now().toString(), playerData.loginTries);
                    
                    // Отправляем асинхронно
                    THREADPOOL.execute(() -> {
                        boolean sent = telegramManager.sendNotification(playerName, message);
                        if (sent) {
                            LogDebug("Sent Telegram login attempt limit notification to user: " + playerName);
                        }
                    });
                }
                
                player.networkHandler.disconnect(langConfig.loginTriesExceeded.get());
                return 0;
            }

            langConfig.wrongPassword.send(source);
            
            // Отправляем уведомление в Telegram о неудачной попытке входа
            boolean shouldNotify = telegramConfig.notifications.notifyOnlyOnSuspiciousAttempts
                ? playerData.loginTries >= config.maxLoginTries - 1
                : true;
                
            if (telegramManager != null && telegramManager.isEnabled() 
                    && telegramConfig.notifications.enableFailedLoginNotifications 
                    && shouldNotify) {
                // Отправляем уведомление в соответствии с настройками
                final String playerName = player.getNameForScoreboard();
                String ip = playerAuth.easyAuth$getIpAddress();
                String message = String.format("⚠️ Неудачная попытка входа!\n\nИмя игрока: %s\nIP-адрес: %s\nВремя: %s\nПопытка: %d из %d", 
                        playerName, ip, ZonedDateTime.now().toString(), playerData.loginTries, config.maxLoginTries);
                
                // Отправляем асинхронно
                THREADPOOL.execute(() -> {
                    boolean sent = telegramManager.sendNotification(playerName, message);
                    if (sent) {
                        LogDebug("Sent Telegram failed login notification to user: " + playerName);
                    }
                });
            }
            
            playerData.update();
            return 0;
        }
    }
}
