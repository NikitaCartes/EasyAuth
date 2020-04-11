package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class LoginCommand {
    private static Text enterPassword = new LiteralText(SimpleAuth.config.lang.enterPassword);
    private static Text wrongPassword = new LiteralText(SimpleAuth.config.lang.wrongPassword);
    private static Text alreadyAuthenticated = new LiteralText(SimpleAuth.config.lang.alreadyAuthenticated);
    private static Text notRegistered = new LiteralText(SimpleAuth.config.lang.notRegistered);
    private static Text loginTriesExceeded = new LiteralText(SimpleAuth.config.lang.loginTriesExceeded);
    private static Text successfullyAuthenticated = new LiteralText(SimpleAuth.config.lang.successfullyAuthenticated);
    private static int maxLoginTries = SimpleAuth.config.main.maxLoginTries;

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/login" command
        dispatcher.register(literal("login")
                .then(argument("password", word())
                        .executes(ctx -> login(ctx.getSource(), getString(ctx, "password")) // Tries to authenticate user
                        ))
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterPassword);
                    return 0;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();

        if(SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(alreadyAuthenticated);
            return 0;
        }
        else if(SimpleAuth.deauthenticatedUsers.get(player) >= maxLoginTries && maxLoginTries != -1) {
            SimpleAuth.deauthenticatePlayer(player);
            player.networkHandler.disconnect(loginTriesExceeded);
            return 0;
        }
        else if (AuthHelper.checkPass(player.getUuidAsString(), pass.toCharArray()) == 1) {
            SimpleAuth.authenticatePlayer(player, successfullyAuthenticated);
            return 1;
        }
        else if(AuthHelper.checkPass(player.getUuidAsString(), pass.toCharArray()) == -1) {
            player.sendMessage(notRegistered);
            return 0;
        }
        // Kicking the player out
        else if(maxLoginTries == 1) {
            SimpleAuth.deauthenticatePlayer(player);
            player.networkHandler.disconnect(wrongPassword);
            return 0;
        }
        // Sending wrong pass message
        player.sendMessage(wrongPassword);
        // ++ the login tries
        SimpleAuth.deauthenticatedUsers.replace(
                player,
                SimpleAuth.deauthenticatedUsers.getOrDefault(player, 0) + 1
        );
        return 0;
    }
}
