package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class RegisterCommand {
    private static TranslatableText pleaseRegister = new TranslatableText("§4Type /register <password> <password> to login.");
    private static TranslatableText enterPassword = new TranslatableText("command.simpleauth.passwordTwice");
    private static TranslatableText alreadyAuthenticated = new TranslatableText("command.simpleauth.alreadyAuthenticated");
    private static TranslatableText alreadyRegistered = new TranslatableText("command.simpleauth.alreadyRegistered");
    private static TranslatableText registerSuccess = new TranslatableText("command.simpleauth.registerSuccess");

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
        }
        else if(pass1.equals(pass2)) {
            String hash = AuthHelper.hashPass(pass1.toCharArray());
            if (SimpleAuth.db.registerUser(player.getUuidAsString(), source.getName(), hash)) {
                SimpleAuth.authenticatedUsers.add(player);
                player.sendMessage(registerSuccess);
                return 1;
            }
            player.sendMessage(alreadyRegistered);
            return 0;
        }
        player.sendMessage(
            new LiteralText(source.getName() + ", passwords must match!")
        );
        return 0;
    }
}
