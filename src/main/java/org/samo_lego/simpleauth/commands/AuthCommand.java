package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.AuthConfig;
import org.samo_lego.simpleauth.utils.AuthHelper;

import java.io.File;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AuthCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    private static Text userdataDeleted = new LiteralText(SimpleAuth.config.lang.userdataDeleted);
    private static Text userdataUpdated = new LiteralText(SimpleAuth.config.lang.userdataUpdated);
    private static Text configurationReloaded = new LiteralText(SimpleAuth.config.lang.configurationReloaded);
    private static Text globalPasswordSet = new LiteralText(SimpleAuth.config.lang.globalPasswordSet);

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/auth" command
        dispatcher.register(literal("auth")
            .requires(source -> source.hasPermissionLevel(4))
            .then(literal("reload")
                .executes( ctx -> reloadConfig(ctx.getSource()))
            )
            .then(literal("setGlobalPassword")
                    .then(argument("password", word())
                            .executes( ctx -> setGlobalPassword(
                                    ctx.getSource(),
                                    getString(ctx, "password")
                            ))
                    )
            )
            .then(literal("update")
                .then(literal("byUuid")
                    .then(argument("uuid", word())
                        .then(argument("password", word())
                                .executes( ctx -> updatePass(
                                        ctx.getSource(),
                                        getString(ctx, "uuid"),
                                        null,
                                        getString(ctx, "password")
                                ))
                        )
                    )
                )
               .then(literal("byUsername")
                   .then(argument("username", word())
                        .then(argument("password", word())
                                .executes( ctx -> updatePass(
                                        ctx.getSource(),
                                        null,
                                        getString(ctx, "username"),
                                        getString(ctx, "password")
                                ))
                        )
                    )
                )
            )
            .then(literal("remove")
                .then(literal("byUuid")
                    .then(argument("uuid", word())
                        .executes( ctx -> removeAccount(
                                ctx.getSource(),
                                getString(ctx, "uuid"),
                                null
                        ))
                    )
                )
                .then(literal("byUsername")
                    .then(argument("username", word())
                        .executes( ctx -> removeAccount(
                                ctx.getSource(),
                                null,
                                getString(ctx, "username")
                        ))
                    )
                )
            )
        );
    }
    private static int setGlobalPassword(ServerCommandSource source, String pass) {
        // Getting the player who send the command
        Entity sender = source.getEntity();
        // Writing the global pass to config
        SimpleAuth.config.main.globalPassword = AuthHelper.hashPass(pass.toCharArray());
        SimpleAuth.config.main.enableGlobalPassword = true;
        SimpleAuth.config.save(new File("./mods/SimpleAuth/config.json"));

        if(sender != null)
            sender.sendMessage(globalPasswordSet);
        else
            LOGGER.info(globalPasswordSet);
        return 1;
    }


    // Method called for checking the password
    private static int updatePass(ServerCommandSource source, String uuid, String username, String pass) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        SimpleAuth.db.update(
                uuid,
                username,
                AuthHelper.hashPass(pass.toCharArray())
        );
        if(sender != null)
            sender.sendMessage(userdataUpdated);
        else
            LOGGER.info(SimpleAuth.config.lang.userdataUpdated);
        // TODO -> Kick player whose name was changed?
        return 1;
    }
    private static int removeAccount(ServerCommandSource source, String uuid, String username) {
        Entity sender = source.getEntity();
        SimpleAuth.db.delete(uuid, username);

        // TODO -> Kick player that was unregistered?

        if(sender != null)
            sender.sendMessage(userdataDeleted);
        else
            LOGGER.info(SimpleAuth.config.lang.userdataDeleted);
        return 1; // Success
    }
    private static int reloadConfig(ServerCommandSource source) {
        Entity sender = source.getEntity();
        SimpleAuth.config = AuthConfig.load(new File("./mods/SimpleAuth/config.json"));

        if(sender != null)
            sender.sendMessage(configurationReloaded);
        else
            LOGGER.info(SimpleAuth.config.lang.configurationReloaded);
        return 1;
    }
}
