package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.ClientModBridge;
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
                        )
                        .then(argument("otp", string())
                                .executes(ctx -> login(ctx.getSource(), getString(ctx, "password"), getString(ctx, "otp")))
                        ))
                .executes(ctx -> {
                    langConfig.password.enter.send(ctx.getSource());
                    return 0;
                }));
    }

    // Method called for checking the password
    public static int login(CommandSourceStack source, String pass) throws CommandSyntaxException {
        return login(source, pass, null, null);
    }

    public static int login(CommandSourceStack source, String pass, String otp) throws CommandSyntaxException {
        return login(source, pass, otp, null);
    }

    public static int login(CommandSourceStack source, String pass, String otp, Runnable onComplete) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = StoneCutterUtils.getUsername(player);
        LogLogin("Player " + username + " is trying to login");
        if (playerAuth.easyAuth$isAuthenticated()) {
            LogLogin("Player " + username + " is already authenticated");
            langConfig.session.alreadyAuthenticated.send(source);
            runComplete(onComplete);
            return 0;
        }

        String ip = playerAuth.easyAuth$getIpAddress();
        if (IpLimitManager.isLoginRateLimitExceeded(ip)) {
            LogLogin("Player " + username + " blocked by login rate limit");
            langConfig.session.tooManyAttempts.send(source);
            runComplete(onComplete);
            return 0;
        }

        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

        THREADPOOL.submit(() -> {
            AuthHelper.PasswordOptions passwordResult = AuthHelper.checkPassword(playerData, pass.toCharArray());
            player.server.execute(() -> {
                applyLoginResult(source, player, playerAuth, playerData, username, ip, passwordResult, otp);
                runComplete(onComplete);
            });
        });
        return 0;
    }

    private static void runComplete(Runnable onComplete) {
        if (onComplete != null) {
            onComplete.run();
        }
    }

    // Applies the verify result on the server thread (state mutations, packets, feedback).
    private static void applyLoginResult(CommandSourceStack source, ServerPlayer player, PlayerAuth playerAuth,
                                         PlayerEntryV1 playerData, String username, String ip,
                                         AuthHelper.PasswordOptions passwordResult, String otp) {
        // Player may have left or authenticated another way while we were hashing.
        if (playerAuth.easyAuth$isAuthenticated()) {
            return;
        }

        if (passwordResult == AuthHelper.PasswordOptions.NOT_REGISTERED) {
            LogLogin("Player " + username + " is not registered");
            if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
                langConfig.registration.requiredWithGlobalPassword.send(source);
                return;
            }
            langConfig.registration.required.send(source);
            return;
        }

        boolean otpFailed = false;
        if (passwordResult == AuthHelper.PasswordOptions.CORRECT && playerData.hasOtp()) {
            if (otp == null || otp.isEmpty()) {
                // Password was right; just ask for the second factor (no failed-attempt penalty).
                LogLogin("Player " + username + " must provide a 2FA code");
                langConfig.session.otpRequired.send(source);
                return;
            }
            if (!playerData.verifyOtp(otp)) {
                LogLogin("Player " + username + " provided wrong 2FA code");
                otpFailed = true;
            }
        }

        if (passwordResult == AuthHelper.PasswordOptions.CORRECT && !otpFailed) {
            LogLogin("Player " + username + " provide correct password");
            finishLogin(player, playerAuth, playerData, ip);
            return;
        }
        playerData.loginTries++;
        if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
            handleMaxTries(player, playerData, username, otpFailed);
            return;
        }
        LogLogin("Player " + username + " provided wrong " + (otpFailed ? "2FA code" : "password"));
        // Sending wrong pass / wrong code message
        (otpFailed ? langConfig.session.otpIncorrect : langConfig.password.incorrect).send(source);
    }

    /**
     * Success tail shared by every explicit login path: the password command/dialog above and the
     * packet token/passkey modes in {@link ClientModBridge}. Re-checks the post-kick window, flips
     * auth state, does the IP bookkeeping, lets the companion issue/rotate its session token, and
     * persists the entry once (the token mutation rides the same DB write). Server thread only.
     */
    public static void finishLogin(ServerPlayer player, PlayerAuth playerAuth, PlayerEntryV1 playerData, String ip) {
        if (playerData.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
            LogLogin("Player " + StoneCutterUtils.getUsername(player) + " will be kicked due to kick timeout");
            player.connection.disconnect(langConfig.session.tooManyAttempts.get());
            return;
        }
        langConfig.session.loginSuccess.send(player);
        playerAuth.easyAuth$restoreTrueLocation();
        playerAuth.easyAuth$setAuthenticated(true);
        playerData.lastAuthenticatedDate = ZonedDateTime.now();
        playerData.loginTries = 0;
        String oldIp = playerData.lastIp;
        playerData.lastIp = ip;

        // Invalidate IP cache if IP changed
        IpLimitManager.clearLoginAttempts(ip);
        if (!oldIp.equals(playerData.lastIp)) {
            IpLimitManager.invalidateCache(oldIp);
            IpLimitManager.invalidateCache(playerData.lastIp);
        }
        // Companion clients get a session token ("remember me") and a login confirmation.
        // onAuthSuccess only mutates the entry, so the update() below is the single DB write.
        ClientModBridge.onAuthSuccess(player);
        playerData.update();
    }

    /**
     * Failure tail shared with the packet token/passkey modes in {@link ClientModBridge}: a
     * rejected credential counts like a wrong password (same maxLoginTries kick), so packet
     * modes keep the failed-attempt penalty any future weaker mode would otherwise silently lack.
     */
    public static void recordFailedAttempt(ServerPlayer player, PlayerEntryV1 playerData) {
        playerData.loginTries++;
        if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) {
            handleMaxTries(player, playerData, StoneCutterUtils.getUsername(player), false);
        }
    }

    private static void handleMaxTries(ServerPlayer player, PlayerEntryV1 playerData, String username, boolean otpFailed) {
        LogLogin("Player " + username + " exceeded max login tries");
        // Send the player a different error message if the max login tries is 1.
        playerData.lastKickedDate = ZonedDateTime.now();
        playerData.loginTries = 0;
        playerData.update();
        if (config.maxLoginTries == 1) {
            player.connection.disconnect((otpFailed ? langConfig.session.otpIncorrect : langConfig.password.incorrect).get());
        } else {
            player.connection.disconnect(langConfig.session.tooManyAttempts.get());
        }
    }
}
