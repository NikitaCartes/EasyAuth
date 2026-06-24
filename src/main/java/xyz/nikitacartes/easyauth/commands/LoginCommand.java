package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.EasyAuthPermissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;

import java.time.ZonedDateTime;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogLogin;

public class LoginCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = registerLogin(dispatcher); // Registering the "/login" command
        if (extendedConfig.aliases.login) {
            dispatcher.register(literal("l")
                    .requires(EasyAuthPermissions.require("easyauth.commands.login", true))
                    .redirect(node));
        }
    }

    public static LiteralCommandNode<CommandSourceStack> registerLogin(CommandDispatcher<CommandSourceStack> dispatcher) {
        return dispatcher.register(literal("login")
                .requires(EasyAuthPermissions.require("easyauth.commands.login", true))
                .then(argument("password", string())
                        .executes(ctx -> login(ctx.getSource(), getString(ctx, "password")) // Tries to authenticate user
                        ))
                .executes(ctx -> {
                    langConfig.password.enter.send(ctx.getSource());
                    return 0;
                }));
    }

    // Method called for checking the password
    public static int login(CommandSourceStack source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = StoneCutterUtils.getUsername(player);
        LogLogin("Player " + username + " is trying to login");
        if (playerAuth.easyAuth$isAuthenticated()) {
            LogLogin("Player " + username + " is already authenticated");
            langConfig.session.alreadyAuthenticated.send(source);
            return 0;
        }

        String ip = playerAuth.easyAuth$getIpAddress();
        if (IpLimitManager.isLoginRateLimitExceeded(ip)) {
            LogLogin("Player " + username + " blocked by login rate limit");
            langConfig.session.tooManyAttempts.send(source);
            return 0;
        }

        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());

        if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
            LogLogin("Player " + username + " provide correct password");
            if (playerData.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
                LogLogin("Player " + username + " will be kicked due to kick timeout");
                player.connection.disconnect(langConfig.session.tooManyAttempts.get());
                return 0;
            }
            langConfig.session.loginSuccess.send(source);
            playerAuth.easyAuth$restoreTrueLocation();
            playerAuth.easyAuth$setAuthenticated(true);
            playerData.lastAuthenticatedDate = ZonedDateTime.now();
            playerData.loginTries = 0;
            String oldIp = playerData.lastIp;
            playerData.lastIp = playerAuth.easyAuth$getIpAddress();
            playerData.update();
            
            // Invalidate IP cache if IP changed
            IpLimitManager.clearLoginAttempts(ip);
            if (!oldIp.equals(playerData.lastIp)) {
                IpLimitManager.invalidateCache(oldIp);
                IpLimitManager.invalidateCache(playerData.lastIp);
            }
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
            return 0;
        } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
            LogLogin("Player " + username + " is not registered");
            if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
                langConfig.registration.requiredWithGlobalPassword.send(source);
                return 0;
            }
            langConfig.registration.required.send(source);
            return 0;
        }
        playerData.loginTries++;
        if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
            LogLogin("Player " + username + " exceeded max login tries");
            // Send the player a different error message if the max login tries is 1.
            playerData.lastKickedDate = ZonedDateTime.now();
            playerData.loginTries = 0;
            playerData.update();
            if (config.maxLoginTries == 1) {
                player.connection.disconnect(langConfig.password.incorrect.get());
            } else {
                player.connection.disconnect(langConfig.session.tooManyAttempts.get());
            }
            return 0;
        }
        LogLogin("Player " + username + " provided wrong password");
        // Sending wrong pass message
        langConfig.password.incorrect.send(source);
        return 0;
    }
}
