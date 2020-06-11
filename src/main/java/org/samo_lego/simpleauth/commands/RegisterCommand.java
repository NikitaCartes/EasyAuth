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
import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;


public class RegisterCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Registering the "/register" command
        dispatcher.register(literal("register")
            .then(argument("password", word())
                .then(argument("passwordAgain", word())
                    .executes( ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
            ))
        .executes(ctx -> {
            ctx.getSource().getPlayer().sendMessage(new LiteralText(config.lang.enterPassword), false);
            return 0;
        }));
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if(config.main.enableGlobalPassword) {
            player.sendMessage(new LiteralText(config.lang.loginRequired), false);
            return 0;
        }
        else if(SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(new LiteralText(config.lang.alreadyAuthenticated), false);
            return 0;
        }
        else if(!pass1.equals(pass2)) {
            player.sendMessage(new LiteralText(config.lang.matchPassword), false);
            return 0;
        }
        // New thread to avoid lag spikes
        new Thread(() -> {
            if(pass1.length() < config.main.minPasswordChars) {
                player.sendMessage(new LiteralText(
                        String.format(config.lang.minPasswordChars, config.main.minPasswordChars)
                ), false);
                return;
            }
            else if(pass1.length() > config.main.maxPasswordChars && config.main.maxPasswordChars != -1) {
                player.sendMessage(new LiteralText(
                        String.format(config.lang.maxPasswordChars, config.main.maxPasswordChars)
                ), false);
                return;
            }
            String hash = AuthHelper.hashPass(pass1.toCharArray());
            // JSON object holding password (may hold some other info in the future)
            JsonObject playerdata = new JsonObject();
            playerdata.addProperty("password", hash);

            if (SimpleAuth.db.registerUser(convertUuid(player), playerdata.toString())) {
                SimpleAuth.authenticatePlayer(player, new LiteralText(config.lang.registerSuccess));
                return;
            }
            player.sendMessage(new LiteralText(config.lang.alreadyRegistered), false);
        }).start();
        return 0;
    }
}
