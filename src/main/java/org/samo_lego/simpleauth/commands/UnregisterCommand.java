package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class UnregisterCommand {
    private static TranslatableText enterPassword = new TranslatableText("§6You need to enter your password!");
    private static TranslatableText wrongPassword = new TranslatableText("§4Wrong password!");
    private static TranslatableText accountDeleted = new TranslatableText("§4Your account was successfully deleted!");

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/unregister" command
        dispatcher.register(literal("unregister")
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterPassword);
                    return 1;
                })
                .then(argument("password", word())
                        .executes( ctx -> unregister(
                                ctx.getSource(),
                                getString(ctx, "password")
                                )
                        )
                )
        );
    }

    // Method called for checking the password and then removing user's account from db
    private static int unregister(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();
        if (AuthHelper.checkPass(player.getUuidAsString(), pass.toCharArray())) {
            SimpleAuth.db.delete(player.getUuidAsString(), null);
            player.sendMessage(accountDeleted);
            return 1;
        }
        player.sendMessage(wrongPassword);
        return 0;
    }
}
