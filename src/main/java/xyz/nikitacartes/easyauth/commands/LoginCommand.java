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
        // Putting rest of the command in different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            PlayerCache playerCache = playerCacheMap.get(uuid);

            int maxLoginTries = config.main.maxLoginTries;
            AtomicInteger curLoginTries = playerCache.loginTries;
            AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(uuid, pass.toCharArray());

            if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
                // Check their kick timeout to avoid giving away information if they're already supposed to be kicked.
                if (playerCache.lastKicked >= System.currentTimeMillis() - 1000 * config.experimental.resetLoginAttemptsTime) {
                    player.networkHandler.disconnect(TranslationHelper.getWrongPassword());
                    return;
                }
                player.sendMessage(TranslationHelper.getSuccessfullyAuthenticated(), false);
                ((PlayerAuth) player).setAuthenticated(true);
                curLoginTries.set(0);
                // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                return;
            } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
                player.sendMessage(TranslationHelper.getRegisterRequired(), false);
                return;
            } else if (curLoginTries.incrementAndGet() == maxLoginTries && maxLoginTries != -1) { // Player exceeded maxLoginTries
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
            // Sending wrong pass message
            player.sendMessage(TranslationHelper.getWrongPassword(), false);
        });
        return 0;
    }
}
