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
        if (!config.otpEnabled) {
            return dispatcher.register(literal("login")
                    .requires(Permissions.require("easyauth.commands.login", true))
                    .then(argument("password", string())
                            .executes(ctx -> login(ctx.getSource(),
                                    getString(ctx, "password"))
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPassword.send(ctx.getSource());
                        return 0;
                    }));
        } else {
            return dispatcher.register(literal("login")
                    .requires(Permissions.require("easyauth.commands.login", true))
                    .then(argument("password/otp", string())
                            .executes(ctx -> login(ctx.getSource(),
                                    getString(ctx, "password/otp"))
                            )
                            .then(argument("otp", string())
                                    .executes(ctx -> login(ctx.getSource(),
                                            getString(ctx, "password/otp"),
                                            getString(ctx, "otp")))
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPasswordWithOtp.send(ctx.getSource());
                        return 0;
                    }));
        }
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getNameForScoreboard();
        LogDebug("Player " + player.getNameForScoreboard() + "{" + username + "} is trying to login");
        if (playerAuth.easyAuth$isAuthenticated()) {
            LogDebug("Player " + player.getNameForScoreboard() + "{" + username + "} is already authenticated");
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        }
        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        boolean otpEnabled = config.otpEnabled && playerData.otpEnabled;

        if (otpEnabled && playerData.twoFactorAuthRequired) {
            langConfig.otpRequired.send(source);
            return 0;
        }

        AuthHelper.PasswordOptions passwordResult;
        if (otpEnabled) {
            if (playerData.verifyOtp(pass)) {
                passwordResult = AuthHelper.PasswordOptions.CORRECT;
            } else {
                passwordResult = AuthHelper.PasswordOptions.WRONG;
            }
        } else {
            passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());
        }

        if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
            handleSuccessfulLogin(playerData, playerAuth, source, player);
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
            return 0;
        } else if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
            LogDebug("Player " + player.getNameForScoreboard() + " is not registered");
            if (config.singleUseGlobalPassword) {
                langConfig.registerRequiredWithGlobalPassword.send(source);
                return 0;
            }
            langConfig.registerRequired.send(source);
            return 0;
        }
        handleFailedLogin(playerData, source, player, otpEnabled);
        return 0;
    }

    private static int login(ServerCommandSource source, String pass, String otp) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getNameForScoreboard();
        LogDebug("Player " + player.getNameForScoreboard() + "{" + username + "} is trying to login");
        if (playerAuth.easyAuth$isAuthenticated()) {
            LogDebug("Player " + player.getNameForScoreboard() + "{" + username + "} is already authenticated");
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        }
        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        boolean otpEnabled = config.otpEnabled && playerData.otpEnabled;

        if (!otpEnabled) {
            langConfig.otpNotEnabled.send(source);
            return 0;
        }
        if (!playerData.twoFactorAuthRequired) {
            langConfig.passwordIsNotRequired.send(source);
            return 0;
        }

        if (playerData.verifyOtp(otp)) {
            AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());
            if (passwordResult == AuthHelper.PasswordOptions.CORRECT) {
                handleSuccessfulLogin(playerData, playerAuth, source, player);
                return 0;
            }
            handleFailedLogin(playerData, source, player, false);
        } else {
            handleFailedLogin(playerData, source, player, true);
        }
        return 0;
    }

    private static void handleSuccessfulLogin(PlayerEntryV1 playerData, PlayerAuth playerAuth, ServerCommandSource source, ServerPlayerEntity player) {
        LogDebug("Player " + player.getNameForScoreboard() + " logged in successfully");
        if (playerData.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
            LogDebug("Player " + player.getNameForScoreboard() + " will be kicked due to kick timeout");
            player.networkHandler.disconnect(langConfig.loginTriesExceeded.get());
            return;
        }
        langConfig.successfullyAuthenticated.send(source);
        playerAuth.easyAuth$setAuthenticated(true);
        playerAuth.easyAuth$restoreTrueLocation();
        playerData.lastAuthenticatedDate = ZonedDateTime.now();
        playerData.loginTries = 0;
        playerData.lastIp = playerAuth.easyAuth$getIpAddress();
        playerData.update();
    }

    private static void handleFailedLogin(PlayerEntryV1 playerData, ServerCommandSource source, ServerPlayerEntity player, boolean otp) {
        playerData.loginTries++;
        if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) {
            LogDebug("Player " + player.getNameForScoreboard() + " exceeded max login tries");
            playerData.lastKickedDate = ZonedDateTime.now();
            playerData.loginTries = 0;
            playerData.update();
            if (config.maxLoginTries == 1) {
                if (otp) {
                    player.networkHandler.disconnect(langConfig.wrongOtp.get());
                } else {
                    player.networkHandler.disconnect(langConfig.wrongPassword.get());
                }
            } else {
                player.networkHandler.disconnect(langConfig.loginTriesExceeded.get());
            }
            return;
        }
        LogDebug("Player " + player.getNameForScoreboard() + " provided wrong " + (otp ? "OTP" : "password"));
        if (otp) {
            langConfig.wrongOtp.send(source);
        } else {
            langConfig.wrongPassword.send(source);
        }
    }
}