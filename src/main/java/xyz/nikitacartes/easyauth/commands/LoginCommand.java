package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.TranslationHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginCommand {
    
    // To reset the login attempts...
    public static final ScheduledExecutorService RESET_LOGIN_THREAD = Executors.newScheduledThreadPool(1);
    
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> node = registerLogin(dispatcher); // Registering the "/login" command
        if (config.experimental.enableAliases) {
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
                    ctx.getSource().getPlayer().sendMessage(TranslationHelper.getEnterPassword(), false);
                    return 0;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();
        String uuid = ((PlayerAuth) player).getFakeUuid();
        if (((PlayerAuth) player).isAuthenticated()) {
            player.sendMessage(TranslationHelper.getAlreadyAuthenticated(), false);
            return 0;
        }
        
        int maxLoginTries = config.main.maxLoginTries;
        // ++ the login tries. Maybe it's more threadsafe here than in the thread pool?
        if (playerCacheMap.get(uuid).loginTries <= maxLoginTries) {
            playerCacheMap.get(uuid).loginTries++;
        }
        
        // Putting rest of the command in different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(uuid, pass.toCharArray());

            if (playerCacheMap.get(uuid).loginTries > maxLoginTries && maxLoginTries != -1) {
                player.networkHandler.disconnect(TranslationHelper.getLoginTriesExceeded());
                return;
            } else if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
                player.sendMessage(TranslationHelper.getSuccessfullyAuthenticated(), false);
                ((PlayerAuth) player).setAuthenticated(true);
                
                // Reset their login tries
                playerCacheMap.get(uuid).loginTries = 0;
                
                // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                return;
            } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
                player.sendMessage(TranslationHelper.getRegisterRequired(), false);
                
            	// Reset their login tries
                playerCacheMap.get(uuid).loginTries = 0;
                
                return;
            }
            // Kicking the player out
            else if (maxLoginTries == 1) {
                // Reset their login tries
                playerCacheMap.get(uuid).loginTries = 0;
                
                player.networkHandler.disconnect(TranslationHelper.getWrongPassword());
                return;
            } else if (playerCacheMap.get(uuid).loginTries == maxLoginTries) {
            	// Reset their login try counter after the amount of seconds specified in the config.
            	RESET_LOGIN_THREAD.schedule(() -> {
            		playerCacheMap.get(uuid).loginTries = 0;
            	}, config.experimental.resetLoginAttemptsTime, TimeUnit.SECONDS);
            }
            // Sending wrong pass message
            player.sendMessage(TranslationHelper.getWrongPassword(), false);
        });
        return 0;
    }
}
