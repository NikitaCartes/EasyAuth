package org.samo_lego.simpleauth.commands;

import com.google.common.io.Files;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.mindrot.jbcrypt.BCrypt;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class RegisterCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Registering the "/register" command
        dispatcher.register(literal("register")
            .then(argument("password", word())
                .then(argument("passwordAgain", word())
                    .executes( ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
            ))
        .executes(ctx -> {
            System.out.println("You need to enter your password twice!");
            return 1;
        }));
    }

    // Registering our "register" command
    private static int register(ServerCommandSource source, String pass1, String pass2) {
        if(pass1.equals(pass2)){
            // Hashing the password with help of jBCrypt library
            String hashed = BCrypt.hashpw(pass1, BCrypt.gensalt());

            source.getMinecraftServer().getPlayerManager().broadcastChatMessage(
                    new LiteralText(source.getName() + ", you have registered successfully!"),
                    false
            );
        }
        else
            source.getMinecraftServer().getPlayerManager().broadcastChatMessage(
                new LiteralText(source.getName() + ", passwords must match!"),
                false
            );
        return 1;
    }
}
