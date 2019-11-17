package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.samo_lego.simpleauth.SimpleAuth;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class LoginCommand {
    private static LiteralText pleaseLogin = new LiteralText("§4Type /login <password> to login.");
    private static TranslatableText enterPassword = new TranslatableText("command.simpleauth.password");
    private static TranslatableText wrongPassword = new TranslatableText("command.simpleauth.wrongPassword");
    private static TranslatableText alreadyAuthenticated = new TranslatableText("command.simpleauth.alreadyAuthenticated");
    private static Text text = new LiteralText("You have entered login command");

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/login" command
        dispatcher.register(literal("login")
                .then(argument("password", word())
                        .executes(ctx -> login(ctx.getSource(), getString(ctx, "password")) // Tries to authenticate user
                        ))
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterPassword);
                    return 1;
                }));
    }

    // Method called for checking the password
    private static int login(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();

        if(SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(alreadyAuthenticated);
        }
        else {
            // Create instance
            Argon2 argon2 = Argon2Factory.create();
            // Read password from user
            char[] password = pass.toCharArray();

            try {
                // Hashed password from DB
                String hashed = SimpleAuth.db.getPassword(player.getUuidAsString());

                // Verify password
                if (argon2.verify(hashed, password)) {
                    SimpleAuth.authenticatedUsers.add(player);
                    player.sendMessage(text);
                } else {
                    player.sendMessage(wrongPassword);
                }
            } finally {
                // Wipe confidential data
                argon2.wipeArray(password);
            }
        }
        return 1; // Success
    }
}
