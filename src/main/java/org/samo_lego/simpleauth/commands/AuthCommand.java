package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.SimpleAuth;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AuthCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    private static TranslatableText userdataDeleted = new TranslatableText("command.simpleauth.userdataDeleted");
    private static TranslatableText userdataUpdated = new TranslatableText("command.simpleauth.userdataUpdated");

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/auth" command
        dispatcher.register(literal("auth")
            .requires(source -> source.hasPermissionLevel(4))
            .then(literal("update")
                .then(argument("uuid", word())
                    .then(argument("password", word())
                            .executes( ctx -> updatePass(
                                    ctx.getSource(),
                                    getString(ctx, "uuid"),
                                    getString(ctx, "password")
                            ))
                    )
                )
            )
            .then(literal("remove")
                .then(argument("uuid", word())
                    .executes( ctx -> removeAccount(
                            ctx.getSource(),
                            getString(ctx, "uuid")
                    ))
                )
            )
        );
    }

    // Method called for checking the password
    private static int updatePass(ServerCommandSource source, String uuid, String pass) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        if(uuid == null)
            return -1;
        // Create instance
        Argon2 argon2 = Argon2Factory.create();
        char[] password = pass.toCharArray();
        try {
            // Hashed password from DB
            String hashed = SimpleAuth.db.getPassword(uuid);

            // Writing into DB
            SimpleAuth.db.update(uuid, hashed);
            if(sender != null)
                sender.sendMessage(userdataUpdated);
            else
                LOGGER.info(userdataUpdated);
        } finally {
            // Wipe confidential data
            argon2.wipeArray(password);
        }

        return 1; // Success
    }
    private static int removeAccount(ServerCommandSource source, String uuid) {
        // Getting the player who send the command
        Entity sender = source.getEntity();
        SimpleAuth.db.delete(uuid);
        if(sender != null)
            sender.sendMessage(userdataDeleted);
        else
            LOGGER.info(userdataDeleted);
        return 1; // Success
    }
}
