package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class LoginCommand {
    private static TranslatableText enterPassword = new TranslatableText("§6You need to enter your password!");
    private static TranslatableText wrongPassword = new TranslatableText("§4Wrong password!");
    private static TranslatableText alreadyAuthenticated = new TranslatableText("§4You are already authenticated.");
    private static Text text = new LiteralText("§aYou are now authenticated.");

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
        else if (AuthHelper.checkPass(player.getUuidAsString(), pass.toCharArray())) {
            SimpleAuth.authenticatedUsers.add(player);
            // Player no longer needs to be invisible and invulnerable
            player.setInvulnerable(false);
            player.setInvisible(false);
            player.sendMessage(text);
            return 1;
        }
        player.sendMessage(wrongPassword);
        return 0;
    }
}
