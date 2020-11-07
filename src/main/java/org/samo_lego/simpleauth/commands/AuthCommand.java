package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.PlayerCache;
import org.samo_lego.simpleauth.utils.AuthHelper;

import java.io.File;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.simpleauth.SimpleAuth.*;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class AuthCommand {

    /**
     * Registers the "/auth" command
     * @param dispatcher
     */
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("auth")
            .requires(source -> source.hasPermissionLevel(4))
            .then(literal("reload")
                .executes( ctx -> reloadConfig(ctx.getSource().getEntity()))
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
                    .executes( ctx -> setSpawn(
                        ctx.getSource(),
                        ctx.getSource().getEntityOrThrow().getEntityWorld().getRegistryKey().getValue(),
                        ctx.getSource().getEntityOrThrow().getX(),
                        ctx.getSource().getEntityOrThrow().getY(),
                        ctx.getSource().getEntityOrThrow().getZ(),
                        ctx.getSource().getEntityOrThrow().yaw,
                        ctx.getSource().getEntityOrThrow().pitch
                    ))
                    .then(argument("dimension", DimensionArgumentType.dimension())
                            .then(argument("position", BlockPosArgumentType.blockPos())
                                .then(argument("angle", RotationArgumentType.rotation())
                                    .executes(ctx -> setSpawn(
                                            ctx.getSource(),
                                            DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue(),
                                            BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getX(),
                                            // +1 to not spawn player in ground
                                            BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getY() + 1,
                                            BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getZ(),
                                            RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).y,
                                            RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).x
                                    )
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
                        .executes( ctx -> updatePassword(
                                ctx.getSource(),
                                getString(ctx, "uuid"),
                                getString(ctx, "password")
                        ))
                    )
                )
            )
        );
    }

    /**
     * Reloads the config file.
     *
     * @param sender executioner of the command
     * @return 0
     */
    public static int reloadConfig(Entity sender) {
        config = AuthConfig.load(new File("./mods/SimpleAuth/config.json"));

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.configurationReloaded), false);
        else
            logInfo(config.lang.configurationReloaded);
        return 1;
    }

    /**
     * Sets global password.
     *
     * @param source executioner of the command
     * @param password password that will be set
     * @return 0
     */
    private static int setGlobalPassword(ServerCommandSource source, String password) {
        // Getting the player who send the command
        Entity sender = source.getEntity();
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            // Writing the global pass to config
            config.main.globalPassword = AuthHelper.hashPassword(password.toCharArray());
            config.main.enableGlobalPassword = true;
            config.save(new File("./mods/SimpleAuth/config.json"));
        });

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.globalPasswordSet), false);
        else
            logInfo(config.lang.globalPasswordSet);
        return 1;
    }

    /**
     * Sets {@link org.samo_lego.simpleauth.storage.AuthConfig.MainConfig.WorldSpawn global spawn}.
     *
     * @param source executioner of the command
     * @param world world id of global spawn
     * @param x x coordinate of the global spawn
     * @param y y coordinate of the global spawn
     * @param z z coordinate of the global spawn
     * @param yaw player yaw (y rotation)
     * @param pitch player pitch (x rotation)
     * @return 0
     */
    private static int setSpawn(ServerCommandSource source, Identifier world, double x, double y, double z, float yaw, float pitch) {
        // Setting config values and saving
        config.worldSpawn.dimension = String.valueOf(world);
        config.worldSpawn.x = x;
        config.worldSpawn.y = y;
        config.worldSpawn.z = z;
        config.worldSpawn.yaw = yaw;
        config.worldSpawn.pitch = pitch;
        config.main.spawnOnJoin = true;
        config.save(new File("./mods/SimpleAuth/config.json"));

        // Getting sender
        Entity sender = source.getEntity();
        if(sender != null)
            ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.worldSpawnSet), false);
        else
            logInfo(config.lang.worldSpawnSet);
        return 1;
    }

    /**
     * Deletes (unregisters) player's account.
     *
     * @param source executioner of the command
     * @param uuid uuid of the player to delete account for
     * @return 0
     */
    private static int removeAccount(ServerCommandSource source, String uuid) {
        Entity sender = source.getEntity();
        THREADPOOL.submit(() -> {
            DB.deleteUserData(uuid);
            playerCacheMap.put(uuid, null);
        });

        if(sender != null)
            ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.userdataDeleted), false);
        else
            logInfo(config.lang.userdataDeleted);
        return 1; // Success
    }

    /**
     * Creates account for player.
     *
     * @param source executioner of the command
     * @param uuid uuid of the player to create account for
     * @param password new password for the player account
     * @return 0
     */
    private static int registerUser(ServerCommandSource source, String uuid, String password) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        THREADPOOL.submit(() -> {
            PlayerCache playerCache;
            if(playerCacheMap.containsKey(uuid)) {
                playerCache = playerCacheMap.get(uuid);
            }
            else {
                playerCache = PlayerCache.fromJson(null, uuid);
            }

            playerCacheMap.put(uuid, playerCache);
            playerCacheMap.get(uuid).password = AuthHelper.hashPassword(password.toCharArray());

            if (sender != null)
                ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.userdataUpdated), false);
            else
                logInfo(config.lang.userdataUpdated);
        });
        return 0;
    }

    /**
     * Force-updates the player's password.
     *
     * @param source executioner of the command
     * @param uuid uuid of the player to update data for
     * @param password new password for the player
     * @return 0
     */
    private static int updatePassword(ServerCommandSource source, String uuid, String password) {
        // Getting the player who send the command
        Entity sender = source.getEntity();

        THREADPOOL.submit(() -> {
            PlayerCache playerCache;
            if(playerCacheMap.containsKey(uuid)) {
                playerCache = playerCacheMap.get(uuid);
            }
            else {
                playerCache = PlayerCache.fromJson(null, uuid);
            }

            playerCacheMap.put(uuid, playerCache);
            if(!playerCacheMap.get(uuid).password.isEmpty()) {
                if (sender != null)
                    ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.userNotRegistered), false);
                else
                    logInfo(config.lang.userNotRegistered);
                return;
            }
            playerCacheMap.get(uuid).password = AuthHelper.hashPassword(password.toCharArray());

            if (sender != null)
                ((PlayerEntity) sender).sendMessage(new LiteralText(config.lang.userdataUpdated), false);
            else
                logInfo(config.lang.userdataUpdated);
        });
        return 0;
    }
}
