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


public class RegisterCommand {
    private static Text enterPassword = new LiteralText(SimpleAuth.config.lang.enterPassword);
    private static Text alreadyAuthenticated = new LiteralText(SimpleAuth.config.lang.alreadyAuthenticated);
    private static Text alreadyRegistered = new LiteralText(SimpleAuth.config.lang.alreadyRegistered);
    private static Text registerSuccess = new LiteralText(SimpleAuth.config.lang.registerSuccess);
    private static Text matchPass = new LiteralText( SimpleAuth.config.lang.matchPassword);

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Registering the "/register" command
        dispatcher.register(literal("register")
            .then(argument("password", word())
                .then(argument("passwordAgain", word())
                    .executes( ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
            ))
        .executes(ctx -> {
            ctx.getSource().getPlayer().sendMessage(enterPassword);
            return 0;
        }));
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if(SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(alreadyAuthenticated);
            return 0;
        }
        else if(pass1.equals(pass2)) {
            String hash = AuthHelper.hashPass(pass1.toCharArray());
            if (SimpleAuth.db.registerUser(player.getUuidAsString(), source.getName(), hash)) {
                SimpleAuth.deauthenticatedUsers.remove(player);
                // Player no longer needs to be invisible and invulnerable
                player.setInvulnerable(false);
                player.setInvisible(false);
                player.sendMessage(registerSuccess);
                return 1;
            }
            player.sendMessage(alreadyRegistered);
            return 0;
        }
        player.sendMessage(matchPass);
        return 0;
    }
}
