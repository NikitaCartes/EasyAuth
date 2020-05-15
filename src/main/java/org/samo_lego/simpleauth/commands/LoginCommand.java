package org.samo_lego.simpleauth.commands;

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
import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

public class LoginCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/login" command
        dispatcher.register(literal("login")
                .then(argument("password", word())
                        .executes(ctx -> login(ctx.getSource(), getString(ctx, "password")) // Tries to authenticate user
                        ))
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(new LiteralText(config.lang.enterPassword), false);
                    return 0;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();

        if(SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(new LiteralText(config.lang.alreadyAuthenticated), false);
            return 0;
        }

        String uuid = convertUuid(player);
        int passwordResult = AuthHelper.checkPass(uuid, pass.toCharArray());
        int maxLoginTries = config.main.maxLoginTries;
        
        if(SimpleAuth.deauthenticatedUsers.get(uuid).loginTries >= maxLoginTries && maxLoginTries != -1) {
            player.networkHandler.disconnect(new LiteralText(config.lang.loginTriesExceeded));
            return 0;
        }
        else if(passwordResult == 1) {
            SimpleAuth.authenticatePlayer(player, new LiteralText(config.lang.successfullyAuthenticated));
            return 1;
        }
        else if(passwordResult == -1) {
            player.sendMessage(new LiteralText(config.lang.notRegistered), false);
            return 0;
        }
        // Kicking the player out
        else if(maxLoginTries == 1) {
            player.networkHandler.disconnect(new LiteralText(config.lang.wrongPassword));
            return 0;
        }
        // Sending wrong pass message
        player.sendMessage(new LiteralText(config.lang.wrongPassword), false);
        // ++ the login tries
        SimpleAuth.deauthenticatedUsers.get(uuid).loginTries += 1;
        return 0;
    }
}
