package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.TranslationHelper;

import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

public class LoginCommand {

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
                    ctx.getSource().getPlayerOrThrow().sendMessage(TranslationHelper.getEnterPassword(), false);
                    return 0;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String uuid = ((PlayerAuth) player).getFakeUuid();
        LogDebug("Player " + player.getName().getString() + "(" + uuid + ") is trying to login");
        if (((PlayerAuth) player).isAuthenticated()) {
            LogDebug("Player " + player.getName().getString() + "(" + uuid + ") is already authenticated");
            player.sendMessage(TranslationHelper.getAlreadyAuthenticated(), false);
            return 0;
        }
        // Putting rest of the command in different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            PlayerCache playerCache = playerCacheMap.get(uuid);

            int maxLoginTries = config.main.maxLoginTries;
            AtomicInteger curLoginTries = playerCache.loginTries;
            AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(uuid, pass.toCharArray());

            if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
                LogDebug("Player " + player.getName().getString() + "(" + uuid + ") provide correct password");
                if (playerCache.lastKicked >= System.currentTimeMillis() - 1000 * config.experimental.resetLoginAttemptsTime) {
                    LogDebug("Player " + player.getName().getString() + "(" + uuid + ") will be kicked due to kick timeout");
                    player.networkHandler.disconnect(TranslationHelper.getLoginTriesExceeded());
                    return;
                }
                player.sendMessage(TranslationHelper.getSuccessfullyAuthenticated(), false);
                ((PlayerAuth) player).setAuthenticated(true);
                curLoginTries.set(0);
                // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                return;
            } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
                LogDebug("Player " + player.getName().getString() + "(" + uuid + ") is not registered");
                player.sendMessage(TranslationHelper.getRegisterRequired(), false);
                return;
            } else if (curLoginTries.incrementAndGet() == maxLoginTries && maxLoginTries != -1) { // Player exceeded maxLoginTries
                LogDebug("Player " + player.getName().getString() + "(" + uuid + ") exceeded max login tries");
                // Send the player a different error message if the max login tries is 1.
                if (maxLoginTries == 1) {
                    player.networkHandler.disconnect(TranslationHelper.getWrongPassword());
                } else {
                    player.networkHandler.disconnect(TranslationHelper.getLoginTriesExceeded());
                }
                playerCache.lastKicked = System.currentTimeMillis();
                curLoginTries.set(0);
                return;
            }
            LogDebug("Player " + player.getName().getString() + "(" + uuid + ") provided wrong password");
            // Sending wrong pass message
            player.sendMessage(TranslationHelper.getWrongPassword(), false);
        });
        return 0;
    }
}
