package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
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

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/account" command
        dispatcher.register(literal("account")
                .requires(Permissions.require("easyauth.commands.account.root", true))
                .then(literal("unregister")
                        .requires(Permissions.require("easyauth.commands.account.unregister", true))
                        .executes(ctx -> {
                            ctx.getSource().getPlayerOrThrow().sendMessage(
                                    TranslationHelper.getEnterPassword(),
                                    false
                            );
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
                .then(literal("changePassword") //todo mongodb update
                        .requires(Permissions.require("easyauth.commands.account.changePassword", true))
                        .then(argument("old password", string())
                                .executes(ctx -> {
                                    ctx.getSource().getPlayerOrThrow().sendMessage(
                                            TranslationHelper.getEnterNewPassword(),
                                            false);
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
        );
    }

    // Method called for checking the password and then removing user's account from db
    private static int unregister(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (config.main.enableGlobalPassword) {
            player.sendMessage(
                    TranslationHelper.getCannotUnregister(),
                    false
            );
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            String uuid = ((PlayerAuth) player).easyAuth$getFakeUuid();
            if (AuthHelper.checkPassword(uuid, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                DB.deleteUserData(uuid);
                player.sendMessage(TranslationHelper.getAccountDeleted(), false);
                ((PlayerAuth) player).easyAuth$setAuthenticated(false);
                player.networkHandler.disconnect(TranslationHelper.getAccountDeleted());
                playerCacheMap.remove(uuid);
                return;
            }
            player.sendMessage(
                    TranslationHelper.getWrongPassword(),
                    false
            );
        });
        return 0;
    }

    // Method called for checking the password and then changing it
    private static int changePassword(ServerCommandSource source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (config.main.enableGlobalPassword) {
            player.sendMessage(
                    TranslationHelper.getCannotChangePassword(),
                    false
            );
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(((PlayerAuth) player).easyAuth$getFakeUuid(), oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                if (newPass.length() < config.main.minPasswordChars) {
                    player.sendMessage(TranslationHelper.getMinPasswordChars(), false);
                    return;
                } else if (newPass.length() > config.main.maxPasswordChars && config.main.maxPasswordChars != -1) {
                    player.sendMessage(TranslationHelper.getMaxPasswordChars(), false);
                    return;
                }
                // Changing password in playercache
                playerCacheMap.get(((PlayerAuth) player).easyAuth$getFakeUuid()).password = AuthHelper.hashPassword(newPass.toCharArray());
                player.sendMessage(
                        TranslationHelper.getPasswordUpdated(),
                        false
                );
            } else
                player.sendMessage(
                        TranslationHelper.getWrongPassword(),
                        false
                );
        });
        return 0;
    }
}
