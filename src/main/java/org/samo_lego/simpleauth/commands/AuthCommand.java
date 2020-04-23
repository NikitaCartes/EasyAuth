package org.samo_lego.simpleauth.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.PlayerCache;
import org.samo_lego.simpleauth.utils.AuthHelper;

import java.io.File;
import java.util.Objects;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.SimpleAuth.db;

public class AuthCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    private static Text userdataDeleted = new LiteralText(config.lang.userdataDeleted);
    private static Text userdataUpdated = new LiteralText(config.lang.userdataUpdated);
    private static Text configurationReloaded = new LiteralText(config.lang.configurationReloaded);
    private static Text globalPasswordSet = new LiteralText(config.lang.globalPasswordSet);

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
            .then(literal("setSpawn")
                    .then(argument("here", word())
                            .executes( ctx -> setSpawn(
                                    ctx.getSource(),
                                    Objects.requireNonNull(ctx.getSource().getEntity()).dimension.getRawId(),
                                    ctx.getSource().getEntity().getX(),
                                    ctx.getSource().getEntity().getY(),
                                    ctx.getSource().getEntity().getZ()
                            ))
                    )
                    .then(argument("dimensionId", integer())
                            .then(argument("x", integer())
                                    .then(argument("y", integer())
                                            .then(argument("z", integer())
                                                .executes( ctx -> setSpawn(
                                                        ctx.getSource(),
                                                        getInteger(ctx, "dimensionId"),
                                                        getInteger(ctx, "x"),
                                                        getInteger(ctx, "y"),
                                                        getInteger(ctx, "z")
                                                ))
                                            )
                                    )
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
            .then(literal("register")
                .then(argument("uuid", word())
                    .then(argument("password", word())
                        .executes( ctx -> registerUser(
                                ctx.getSource(),
                                getString(ctx, "uuid"),
                                getString(ctx, "password")
                        ))
                    )
                )
            )
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
        );
    }

    // Reloading the config
    private static int reloadConfig(ServerCommandSource source) {
        Entity sender = source.getEntity();
        config = AuthConfig.load(new File("./mods/SimpleAuth/config.json"));

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(configurationReloaded, false);
        else
            LOGGER.info(config.lang.configurationReloaded);
        return 1;
    }

    // Setting global password
    private static int setGlobalPassword(ServerCommandSource source, String pass) {
        // Getting the player who send the command
        Entity sender = source.getEntity();
        // Writing the global pass to config
        config.main.globalPassword = AuthHelper.hashPass(pass.toCharArray());
        config.main.enableGlobalPassword = true;
        config.save(new File("./mods/SimpleAuth/config.json"));

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(globalPasswordSet, false);
        else
            LOGGER.info(config.lang.globalPasswordSet);
        return 1;
    }

    //
    private static int setSpawn(ServerCommandSource source, int dimensionId, double x, double y, double z) {
        config.worldSpawn.dimensionId = dimensionId;
        config.worldSpawn.x = x;
        config.worldSpawn.y = y;
        config.worldSpawn.z = z;
        Entity sender = source.getEntity();
        if(sender != null)
            sender.sendSystemMessage(new LiteralText(config.lang.worldSpawnSet));
        else
            LOGGER.info(config.lang.worldSpawnSet);
        return 1;
    }

    // Deleting (unregistering) user's account
    private static int removeAccount(ServerCommandSource source, String uuid) {
        Entity sender = source.getEntity();
        db.deleteUserData(uuid);
        SimpleAuth.deauthenticatedUsers.put(uuid, new PlayerCache(uuid, null));

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(userdataDeleted, false);
        else
            LOGGER.info(config.lang.userdataDeleted);
        return 1; // Success
    }

    // Creating account for user
    private static int registerUser(ServerCommandSource source, String uuid, String password) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        // JSON object holding password (may hold some other info in the future)
        JsonObject playerdata = new JsonObject();
        String hash = AuthHelper.hashPass(password.toCharArray());
        playerdata.addProperty("password", hash);

        if(db.registerUser(uuid, playerdata.toString())) {
            if(sender != null)
                ((PlayerEntity) sender).sendMessage(userdataUpdated, false);
            else
                LOGGER.info(config.lang.userdataUpdated);
            return 1;
        }
        return 0;
    }

    // Force-updating the user's password
    private static int updatePass(ServerCommandSource source, String uuid, String password) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        // JSON object holding password (may hold some other info in the future)
        JsonObject playerdata = new JsonObject();
        String hash = AuthHelper.hashPass(password.toCharArray());
        playerdata.addProperty("password", hash);

        db.updateUserData(uuid, playerdata.toString());
        if(sender != null)
            ((PlayerEntity) sender).sendMessage(userdataUpdated, false);
        else
            LOGGER.info(config.lang.userdataUpdated);
        return 1;
    }
    // todo PlayerEntity.getOfflinePlayerUuid("")
}
