package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        LogDebug("Player " + player.getName().getString() + " is trying to login");
        if (playerAuth.easyAuth$isAuthenticated()) {
            LogDebug("Player " + player.getName().getString() + " is already authenticated");
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        }
        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());

        if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
            LogDebug("Player " + player.getName().getString() + " provide correct password");
            if (playerData.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
                LogDebug("Player " + player.getName().getString() + " will be kicked due to kick timeout");
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
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
            return 0;
        } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
            LogDebug("Player " + player.getName().getString() + " is not registered");
            if (config.singleUseGlobalPassword) {
                langConfig.registerRequiredWithGlobalPassword.send(source);
                return 0;
            }
            langConfig.registerRequired.send(source);
            return 0;
        }
        playerData.loginTries++;
        if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
            LogDebug("Player " + player.getName().getString() + " exceeded max login tries");
            // Send the player a different error message if the max login tries is 1.
            playerData.lastKickedDate = ZonedDateTime.now();
            playerData.loginTries = 0;
            playerData.update();
            if (config.maxLoginTries == 1) {
                player.networkHandler.disconnect(langConfig.wrongPassword.get());
            } else {
                player.networkHandler.disconnect(langConfig.loginTriesExceeded.get());
            }
            return 0;
        }
        LogDebug("Player " + player.getName().getString() + " provided wrong password");
        // Sending wrong pass message
        langConfig.wrongPassword.send(source);
        return 0;
    }
}
