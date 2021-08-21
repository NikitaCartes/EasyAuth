package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/account" command
        dispatcher.register(literal("account")
            .then(literal("unregister")
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(
                            new TranslatableText("text.easyauth.enterPassword"),
                            false
                    );
                    return 1;
                })
                .then(argument("password", string())
                        .executes( ctx -> unregister(
                                ctx.getSource(),
                                getString(ctx, "password")
                                )
                        )
                )
            )
            .then(literal("changePassword") //todo mongodb update
                .then(argument("old password", string())
                    .executes(ctx -> {
                        ctx.getSource().getPlayer().sendMessage(
                                new TranslatableText("text.easyauth.enterNewPassword"),
                                false);
                        return 1;
                    })
                    .then(argument("new password", string())
                            .executes( ctx -> changePassword(
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
        ServerPlayerEntity player = source.getPlayer();

        if (config.main.enableGlobalPassword) {
            player.sendMessage(
                    new TranslatableText("text.easyauth.cannotUnregister"),
                    false
            );
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            String uuid = ((PlayerAuth) player).getFakeUuid();
            if (AuthHelper.checkPassword(uuid, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                DB.deleteUserData(uuid);
                player.sendMessage(new TranslatableText("text.easyauth.accountDeleted"), false);
                ((PlayerAuth) player).setAuthenticated(false);
                player.networkHandler.disconnect(new TranslatableText("text.easyauth.accountDeleted"));
                playerCacheMap.remove(uuid);
                return;
            }
            player.sendMessage(
                    new TranslatableText("text.easyauth.wrongPassword"),
                    false
            );
        });
        return 0;
    }

    // Method called for checking the password and then changing it
    private static int changePassword(ServerCommandSource source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();

        if (config.main.enableGlobalPassword) {
            player.sendMessage(
                    new TranslatableText("text.easyauth.cannotChangePassword"),
                    false
            );
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(((PlayerAuth) player).getFakeUuid(), oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                if (newPass.length() < config.main.minPasswordChars) {
                    player.sendMessage(new TranslatableText("text.easyauth.minPasswordChars", config.main.minPasswordChars), false);
                    return;
                }
                else if (newPass.length() > config.main.maxPasswordChars && config.main.maxPasswordChars != -1) {
                    player.sendMessage(new TranslatableText("text.easyauth.maxPasswordChars", config.main.maxPasswordChars), false);
                    return;
                }
                // Changing password in playercache
                playerCacheMap.get(((PlayerAuth) player).getFakeUuid()).password = AuthHelper.hashPassword(newPass.toCharArray());
                player.sendMessage(
                        new TranslatableText("text.easyauth.passwordUpdated"),
                        false
                );
            }
            else
                player.sendMessage(
                    new TranslatableText("text.easyauth.wrongPassword"),
                    false
                );
        });
        return 0;
    }
}
