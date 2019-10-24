package org.samo_lego.simpleauth;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.literal; // literal("foo")
import static net.minecraft.server.command.CommandManager.argument; // argument("bar", word())
import static net.minecraft.server.command.CommandManager.*;


class AuthCommands {

    static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("register")
            .then(argument("password", greedyString())
                /*.executes(ctx -> {
                    System.out.println(getString(ctx, "password"));
                    return 1;
                })*/
                .then(argument("passwordAgain", string())
                    .executes( ctx -> {
                        System.out.println(getString(ctx, "passwordAgain"));
                        return 1;}/*register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "password"))*/)
            ))
        .executes(ctx -> {
            System.out.println("You need to enter your password twice!");
            return 1;
        }));
        // You can deal with the arguments out here and pipe them into the command.
        dispatcher.register(literal("login")
            .then(argument("password", greedyString())
                .executes(ctx -> login(ctx.getSource(), getString(ctx, "password"))
            ))
            .executes(ctx -> {
                System.out.println("You need to enter your password!");
                return 1;
            }));
    }

    // Registering our "register" command
    private static int register(ServerCommandSource source, String pass1, String pass2) {
        System.out.println(pass1);
        if(pass1.equals(pass1)){
            Text text = new LiteralText(source.getName() + ", you have entered register command");
            source.getMinecraftServer().getPlayerManager().broadcastChatMessage(text, false);
        }
        return 1;
    }

    // Registering our "login" command
    private static int login(ServerCommandSource source, String pass) {
        if(pass.equals(pass)){ //From database
            Text text = new LiteralText(source.getName() + ", you have entered login command");
            source.getMinecraftServer().getPlayerManager().broadcastChatMessage(text, false);
        }
        return 1; // Success
    }
}
