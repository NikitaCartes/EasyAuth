package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.EasyAuthPermissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;

import java.time.ZonedDateTime;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.checkGlobalPassword;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.hashPassword;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogRegister;


public class RegisterCommand {

    // Registering the "/reg" alias
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            return;
        }
        LiteralCommandNode<CommandSourceStack> node = registerRegister(dispatcher);
        if (extendedConfig.aliases.register) {
            dispatcher.register(literal("reg")
                    .requires(EasyAuthPermissions.require("easyauth.commands.register", true))
                    .redirect(node));
        }
    }

    // Registering the "/register" command
    public static LiteralCommandNode<CommandSourceStack> registerRegister(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            return dispatcher.register(literal("register")
                    .requires(EasyAuthPermissions.require("easyauth.commands.register", true))
                    .then(argument("globalPassword", string())
                            .then(argument("password", string())
                                    .then(argument("passwordAgain", string())
                                            .executes(ctx -> register(ctx.getSource(),
                                                    getString(ctx, "globalPassword"),
                                                    getString(ctx, "password"),
                                                    getString(ctx, "passwordAgain")))
                                    )
                            )
                    )
                    .executes(ctx -> {
                        langConfig.password.enter.send(ctx.getSource());
                        return 0;
                    }));
        } else {
            return dispatcher.register(literal("register")
                    .requires(EasyAuthPermissions.require("easyauth.commands.register", true))
                    .then(argument("password", string())
                            .then(argument("passwordAgain", string())
                                    .executes(ctx -> register(ctx.getSource(),
                                            getString(ctx, "password"),
                                            getString(ctx, "passwordAgain")))
                            )
                    )
                    .executes(ctx -> {
                        langConfig.password.enter.send(ctx.getSource());
                        return 0;
                    }));
        }
    }

    public static int register(CommandSourceStack source, String globalPassword, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.alreadyAuthenticated.send(source);
            return 0;
        }

        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            if (checkGlobalPassword(globalPassword.toCharArray())) {
                return register(source, pass1, pass2);
            } else {
                PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

                playerData.loginTries++;
                if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
                    String username = StoneCutterUtils.getUsername(player);
                    LogRegister("Player " + username + " exceeded global password tries limit.");
                    playerData.lastKickedDate = ZonedDateTime.now();
                    playerData.loginTries = 0;
                    playerData.update();
                    player.connection.disconnect(langConfig.password.globalIncorrect.get());
                    return 0;
                }
                langConfig.password.globalIncorrect.send(source);
                return 0;
            }
        }
        return 0;
    }

    // Method called for hashing the password & writing to DB
    public static int register(CommandSourceStack source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.session.loginRequired.send(source);
            return 0;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.alreadyAuthenticated.send(source);
            return 0;
        } else if (!pass1.equals(pass2)) {
            langConfig.password.mismatch.send(source);
            return 0;
        }

        if (pass1.length() < extendedConfig.minPasswordLength) {
            langConfig.password.tooShort.send(source, extendedConfig.minPasswordLength);
            return 0;
        } else if (pass1.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.password.tooLong.send(source, extendedConfig.maxPasswordLength);
            return 0;
        }

        // Check IP limit before allowing registration
        String username = StoneCutterUtils.getUsername(player);
        String ipAddress = playerAuth.easyAuth$getIpAddress();
        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();
        if (IpLimitManager.isIpLimitExceeded(ipAddress, username, playerData)) {
            LogRegister("Player " + username + " exceeded IP limit from " + ipAddress);
            if (IpLimitManager.shouldBlockExcessRegistration()) {
                IpLimitManager.notifyAdmins(source.getServer(), ipAddress, username);
                langConfig.error.ipLimitExceeded.send(source);
                return 0;
            } else {
                // Just notify admins but allow registration
                IpLimitManager.notifyAdmins(source.getServer(), ipAddress, username);
            }
        }

        if (!playerData.password.isEmpty()) {
            langConfig.registration.alreadyRegistered.send(source);
            return 0;
        }
        playerAuth.easyAuth$setAuthenticated(true);
        playerAuth.easyAuth$restoreTrueLocation();
        langConfig.registration.success.send(source);
        // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));

        THREADPOOL.submit(() -> {
            String hash = hashPassword(pass1.toCharArray());
            if (hash == null) {
                revokeRegistration(player, playerAuth, source);
                return;
            }
            playerData.password = hash;
            playerData.registrationDate = ZonedDateTime.now();
            playerData.lastIp = playerAuth.easyAuth$getIpAddress();
            playerData.lastAuthenticatedDate = ZonedDateTime.now();
            playerAuth.easyAuth$setPlayerEntryV1(playerData);

            // Synchronous write with result (we're already in THREADPOOL): if it fails, revoke access.
            if (!DB.updateUserData(playerData)) {
                revokeRegistration(player, playerAuth, source);
                return;
            }

            // Invalidate IP cache after registration
            IpLimitManager.invalidateCache(playerData.lastIp);

            LogRegister("Player " + username + "{" + player.getStringUUID() + "} successfully registered");
        });
        return 0;
    }

    // Write failed -> strip the password and drop authentication on the server thread.
    private static void revokeRegistration(ServerPlayer player, PlayerAuth playerAuth, CommandSourceStack source) {
        playerAuth.easyAuth$getPlayerEntryV1().password = "";
        player.server.execute(() -> {
            playerAuth.easyAuth$setAuthenticated(false);
            playerAuth.easyAuth$setLoginDialogSuppressed(true);
            langConfig.error.database.send(source);
        });
    }
}
