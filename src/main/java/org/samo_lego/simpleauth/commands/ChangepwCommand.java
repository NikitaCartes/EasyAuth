package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import org.samo_lego.simpleauth.SimpleAuth;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ChangepwCommand {
    private static TranslatableText enterNewPassword = new TranslatableText("command.simpleauth.passwordNew");
    private static TranslatableText enterPassword = new TranslatableText("command.simpleauth.password");
    private static TranslatableText wrongPassword = new TranslatableText("command.simpleauth.wrongPassword");
    private static TranslatableText passwordUpdated = new TranslatableText("command.simpleauth.passwordUpdated");

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/changepw" command
        dispatcher.register(literal("changepw")
            .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterPassword);
                    return 1;
            })
            .then(argument("oldPassword", word())
                .executes(ctx -> {
                    ctx.getSource().getPlayer().sendMessage(enterNewPassword);
                    return 1;
                })
                .then(argument("newPassword", word())
                    .executes( ctx -> changepw(
                            ctx.getSource(),
                            getString(ctx, "oldPassword"),
                            getString(ctx, "newPassword")
                        )
                    )
                )
            )
        );
    }

    // Method called for checking the password and then changing it
    private static int changepw(ServerCommandSource source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayer();

        // Create instance
        Argon2 argon2 = Argon2Factory.create();
        // Read password from user
        char[] password = oldPass.toCharArray();

        try {
            // Hashed password from DB
            String hashedOld = SimpleAuth.db.getPassword(player.getUuidAsString());

            // Verify password
            if (argon2.verify(hashedOld, password)) {
                String hash = argon2.hash(10, 65536, 1, newPass.toCharArray());
                // Writing into DB
                SimpleAuth.db.update(player.getUuidAsString(), hash);
                player.sendMessage(passwordUpdated);
            }
            else
                player.sendMessage(wrongPassword);
        } finally {
            // Wipe confidential data
            argon2.wipeArray(password);
        }
        return 1;
    }
}
