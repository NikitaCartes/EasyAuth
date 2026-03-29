package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.integrations.Permissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.io.IOException;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.integrations.MojangApi.isValidUsername;

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("account")
                .requires(Permissions.require("easyauth.commands.account.root", true))
                .then(literal("unregister")
                        .requires(Permissions.require("easyauth.commands.account.unregister", true))
                        .executes(ctx -> {
                            langConfig.enterPassword.send(ctx.getSource());
                            return 1;
                        })
                        .then(argument("password", string())
                                .executes(ctx -> unregister(
                                                ctx.getSource(),
                                                getString(ctx, "password")
                                        )
                                )
                        )
                )
                .then(literal("changePassword")
                        .requires(Permissions.require("easyauth.commands.account.changePassword", true))
                        .then(argument("old password", string())
                                .executes(ctx -> {
                                    langConfig.enterNewPassword.send(ctx.getSource());
                                    return 1;
                                })
                                .then(argument("new password", string())
                                        .executes(ctx -> changePassword(
                                                        ctx.getSource(),
                                                        getString(ctx, "old password"),
                                                        getString(ctx, "new password")
                                                )
                                        )
                                )
                        )
                )
                .then(literal("online")
                        .requires(Permissions.require("easyauth.commands.account.online", true))
                        .then(argument("password", string())
                                .executes(ctx -> markAsOnline(
                                        ctx.getSource(),
                                        getString(ctx, "password"),
                                        false
                                        )
                                )
                                .then(argument("confirm", bool())
                                        .executes(ctx -> markAsOnline(
                                                ctx.getSource(),
                                                getString(ctx, "password"),
                                                getBool(ctx, "confirm")
                                                )
                                        )
                                )
                        )
                )
        );
    }

    // Method called for checking the password and then removing user's account from db
    private static int unregister(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.cannotUnregister.send(source);
            return 0;
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.cannotUnregister.send(source);
            return 0;
        }

        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.loginRequired.send(source);
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            String username = StoneCutterUtils.getUsername(player);
            if (AuthHelper.checkPassword(playerAuth, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                PlayerEntryV1 playerEntry = DB.getUserData(username);
                if (playerEntry == null) {
                    langConfig.cannotUnregister.send(source);
                    return;
                }

                if (!DB.deleteUserData(username)) {
                    langConfig.unknownError.send(source);
                    return;
                }
                langConfig.accountDeleted.send(source);
                playerAuth.easyAuth$setAuthenticated(false);
                playerAuth.easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
                player.networkHandler.disconnect(langConfig.accountDeleted.get());
                return;
            }
            langConfig.wrongPassword.send(source);
        });
        return 0;
    }

    // Method called for checking the password and then changing it
    private static int changePassword(ServerCommandSource source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.cannotChangePassword.send(source);
            return 0;
        }
        if (newPass.length() < extendedConfig.minPasswordLength) {
            langConfig.minPasswordChars.send(source, extendedConfig.minPasswordLength);
            return 0;
        } else if (newPass.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.maxPasswordChars.send(source, extendedConfig.maxPasswordLength);
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                // Changing password

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.password = AuthHelper.hashPassword(newPass.toCharArray());
                playerEntry.update();

                langConfig.passwordUpdated.send(source);
            } else {
                langConfig.wrongPassword.send(source);
            }
        });
        return 0;
    }

    /**
     * Set player as player with online account
     *
     * @param source   executioner of the command
     * @param password password of the player
     * @return 0
     */
    private static int markAsOnline(ServerCommandSource source, String password) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, password.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                String username = StoneCutterUtils.getUsername(player);
                try {
                    if (!isValidUsername(username)) {
                        langConfig.accountNotFound.send(source);
                        return;
                    }
                } catch (IOException e) {
                    langConfig.accountCheckFailed.send(source);
                    return;
                }

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
                playerEntry.update();

                langConfig.selfMarkAsOnline.send(source);
            } else {
                langConfig.wrongPassword.send(source);
            }
        });

        return 1;
    }

    private static int markAsOnline(ServerCommandSource source, String password, boolean confirm) throws CommandSyntaxException {
        if (!confirm) {
            langConfig.selfMarkAsOnlineWarning.send(source);
            return 0;
        }
        return markAsOnline(source, password);
    }
}
