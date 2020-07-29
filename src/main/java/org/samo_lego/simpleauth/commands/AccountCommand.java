package org.samo_lego.simpleauth.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.simpleauth.SimpleAuth.THREADPOOL;
import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/account" command
        dispatcher.register(literal("account")
                .then(literal("unregister")
                    .executes(ctx -> {
                        ctx.getSource().getPlayer().sendMessage(
                                new LiteralText(config.lang.enterPassword),
                                false
                        );
                        return 1;
                    })
                    .then(argument("password", word())
                            .executes( ctx -> unregister(
                                    ctx.getSource(),
                                    getString(ctx, "password")
                                    )
                            )
                    )
                )
                .then(literal("changePassword")
                    .then(argument("old password", word())
                        .executes(ctx -> {
                            ctx.getSource().getPlayer().sendMessage(
                                    new LiteralText(config.lang.enterNewPassword),
                                    false);
                            return 1;
                        })
                        .then(argument("new password", word())
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
                    new LiteralText(config.lang.cannotUnregister),
                    false
            );
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPass(convertUuid(player), pass.toCharArray()) == 1) {
                SimpleAuth.DB.deleteUserData(convertUuid(player));
                player.sendMessage(
                        new LiteralText(config.lang.accountDeleted),
                        false
                );
                SimpleAuth.deauthenticatePlayer(player);
                return;
            }
            player.sendMessage(
                    new LiteralText(config.lang.wrongPassword),
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
                    new LiteralText(config.lang.cannotChangePassword),
                    false
            );
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPass(convertUuid(player), oldPass.toCharArray()) == 1) {
                if (newPass.length() < config.main.minPasswordChars) {
                    player.sendMessage(new LiteralText(
                            String.format(config.lang.minPasswordChars, config.main.minPasswordChars)
                    ), false);
                    return;
                }
                else if (newPass.length() > config.main.maxPasswordChars && config.main.maxPasswordChars != -1) {
                    player.sendMessage(new LiteralText(
                            String.format(config.lang.maxPasswordChars, config.main.maxPasswordChars)
                    ), false);
                    return;
                }
                // JSON object holding password (may hold some other info in the future)
                JsonObject playerdata = new JsonObject();
                String hash = AuthHelper.hashPassword(newPass.toCharArray());
                playerdata.addProperty("password", hash);

                SimpleAuth.DB.updateUserData(convertUuid(player), playerdata.toString());
                player.sendMessage(
                        new LiteralText(config.lang.passwordUpdated),
                        false
                );
            }
            else
                player.sendMessage(
                    new LiteralText(config.lang.wrongPassword),
                    false
                );
        });
        return 0;
    }
}
