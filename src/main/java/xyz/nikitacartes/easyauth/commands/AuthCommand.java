package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.deprecated.AuthConfig;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.storage.database.DBApiException;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

public class AuthCommand {
    /**
     * Registers the "/auth" command
     *
     * @param dispatcher
     */
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("auth")
                .requires(Permissions.require("easyauth.commands.auth.root", 3))
                .then(literal("reload")
                        .requires(Permissions.require("easyauth.commands.auth.reload", 3))
                        .executes(ctx -> reloadConfig(ctx.getSource()))
                )
                .then(literal("setGlobalPassword")
                        .requires(Permissions.require("easyauth.commands.auth.setGlobalPassword", 4))
                        .then(argument("password", string())
                                .executes(ctx -> setGlobalPassword(
                                        ctx.getSource(),
                                        getString(ctx, "password"),
                                        false
                                ))
                                .then(argument("singleUse", bool())
                                        .executes(ctx -> setGlobalPassword(
                                                ctx.getSource(),
                                                getString(ctx, "password"),
                                                getBool(ctx, "singleUse")
                                        ))
                                )
                        )
                )
                .then(literal("setSpawn")
                        .requires(Permissions.require("easyauth.commands.auth.setSpawn", 3))
                        .executes(ctx -> setSpawn(
                                ctx.getSource(),
                                ctx.getSource().getEntityOrThrow().getEntityWorld().getRegistryKey().getValue(),
                                ctx.getSource().getEntityOrThrow().getX(),
                                ctx.getSource().getEntityOrThrow().getY(),
                                ctx.getSource().getEntityOrThrow().getZ(),
                                ctx.getSource().getEntityOrThrow().getYaw(),
                                ctx.getSource().getEntityOrThrow().getPitch()
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
                                                                RotationArgumentType.getRotation(ctx, "angle").getRotation(ctx.getSource()).y,
                                                                RotationArgumentType.getRotation(ctx, "angle").getRotation(ctx.getSource()).x
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(literal("remove")
                        .requires(Permissions.require("easyauth.commands.auth.remove", 3))
                        .then(argument("username", word())
                                .executes(ctx -> removeAccount(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("register")
                        .requires(Permissions.require("easyauth.commands.auth.register", 3))
                        .then(argument("username", word())
                                .then(argument("password", string())
                                        .executes(ctx -> registerUser(
                                                ctx.getSource(),
                                                getString(ctx, "username"),
                                                getString(ctx, "password")
                                        ))
                                )
                        )
                )
                .then(literal("update")
                        .requires(Permissions.require("easyauth.commands.auth.update", 3))
                        .then(argument("username", word())
                                .then(argument("password", string())
                                        .executes(ctx -> updatePassword(
                                                ctx.getSource(),
                                                getString(ctx, "username"),
                                                getString(ctx, "password")
                                        ))
                                )
                        )
                )
                .then(literal("list")
                        .requires(Permissions.require("easyauth.commands.auth.list", 3))
                        .executes(ctx -> getRegisteredPlayers(ctx.getSource()))
                )
                .then(literal("markAsOffline")
                        .requires(Permissions.require("easyauth.commands.auth.markAsOffline", 3))
                        .then(argument("username", word())
                                .executes(ctx -> markAsOffline(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("markAsOnline")
                        .requires(Permissions.require("easyauth.commands.auth.markAsOnline", 3))
                        .then(argument("username", word())
                                .executes(ctx -> markAsOnline(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("getPlayerInfo")
                        .requires(Permissions.require("easyauth.commands.auth.getPlayerInfo", 3))
                        .then(argument("username", word())
                                .executes(ctx -> getPlayerInfo(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
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
    public static int reloadConfig(ServerCommandSource sender) {
        DB.close();
        EasyAuth.loadConfigs();

        try {
            DB.connect();
        } catch (DBApiException e) {
            LogError("onInitialize error: ", e);
        }

        langConfig.configurationReloaded.send(sender);

        return Command.SINGLE_SUCCESS;
    }

    public static void reloadConfig(MinecraftServer sender) {
        DB.close();
        EasyAuth.loadConfigs();

        try {
            DB.connect();
        } catch (DBApiException e) {
            LogError("onInitialize error: ", e);
        }

        langConfig.configurationReloaded.send(sender);
    }

    /**
     * Sets global password.
     *
     * @param source   executioner of the command
     * @param password password that will be set
     * @param singleUse whether the global password is single-use
     * @return 0
     */
    private static int setGlobalPassword(ServerCommandSource source, String password, boolean singleUse) {
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            // Writing the global pass to config
            technicalConfig.globalPassword = AuthHelper.hashPassword(password.toCharArray());
            config.enableGlobalPassword = true;
            config.singleUseGlobalPassword = singleUse;
            technicalConfig.save();
            config.save();
        });

        langConfig.globalPasswordSet.send(source);
        return 1;
    }

    /**
     * Sets {@link AuthConfig.MainConfig.WorldSpawn global spawn}.
     *
     * @param source executioner of the command
     * @param world  world id of global spawn
     * @param x      x coordinate of the global spawn
     * @param y      y coordinate of the global spawn
     * @param z      z coordinate of the global spawn
     * @param yaw    player yaw (y rotation)
     * @param pitch  player pitch (x rotation)
     * @return 0
     */
    private static int setSpawn(ServerCommandSource source, Identifier world, double x, double y, double z, float yaw, float pitch) {
        // Setting config values and saving
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            config.worldSpawn.dimension = String.valueOf(world);
            config.worldSpawn.x = x;
            config.worldSpawn.y = y;
            config.worldSpawn.z = z;
            config.worldSpawn.yaw = yaw;
            config.worldSpawn.pitch = pitch;
            config.hidePlayerCoords = true;
            config.save();
        });

        langConfig.worldSpawnSet.send(source);
        return 1;
    }

    /**
     * Deletes (unregisters) player's account.
     *
     * @param source   executioner of the command
     * @param username username of the player to delete account for
     * @return 0
     */
    private static int removeAccount(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            DB.deleteUserData(username);
        });

        ServerPlayerEntity playerEntity = source.getServer().getPlayerManager().getPlayer(username);
        if (playerEntity != null) {
            ((PlayerAuth) playerEntity).easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
            playerEntity.networkHandler.disconnect(langConfig.userdataDeleted.get());
        }

        langConfig.userdataDeleted.send(source);
        return 1; // Success
    }

    /**
     * Creates account for player.
     *
     * @param source   executioner of the command
     * @param username username of the player to create account for
     * @param password new password for the player account
     * @return 0
     */
    private static int registerUser(ServerCommandSource source, String username, String password) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserDataOrCreate(username);
            playerData.password = AuthHelper.hashPassword(password.toCharArray());
            playerData.registrationDate = ZonedDateTime.now();
            playerData.update();

            langConfig.userdataUpdated.send(source);
        });
        return 0;
    }

    /**
     * Force-updates the player's password.
     *
     * @param source   executioner of the command
     * @param username username of the player to update data for
     * @param password new password for the player
     * @return 0
     */
    private static int updatePassword(ServerCommandSource source, String username, String password) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserData(username);
            if (playerData == null || playerData.password.isEmpty()) {
                langConfig.userNotRegistered.send(source);
                return;
            }
            playerData.password = AuthHelper.hashPassword(password.toCharArray());
            playerData.update();
            langConfig.userdataUpdated.send(source);
        });
        return 0;
    }

    /**
     * List of registered username
     *
     * @param source executioner of the command
     * @return 0
     */
    public static int getRegisteredPlayers(ServerCommandSource source) {
        THREADPOOL.submit(() -> {
            if (langConfig.registeredPlayers.enabled) {
                AtomicInteger i = new AtomicInteger();
                MutableText message = langConfig.registeredPlayers.get();
                DB.getAllData().forEach((username, playerData) -> {
                    if (playerData == null || playerData.password == null) {
                        return;
                    }
                    i.getAndIncrement();
                    message.append(Text.translatable(username)
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, username)))
                            .formatted(Formatting.YELLOW))
                            .append(", ");
                });
                source.sendMessage(message);
            }
        });
        return 1;
    }

    /**
     * Set player as player with offline account
     *
     * @param source   executioner of the command
     * @param username player to add in list
     * @return 0
     */
    private static int markAsOffline(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserDataOrCreate(username);
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
            entry.update();
        });

        langConfig.markAsOffline.send(source, username);
        return 1;
    }

    /**
     * Set player as player with online account
     *
     * @param source   executioner of the command
     * @param username player to add in list
     * @return 0
     */
    private static int markAsOnline(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserDataOrCreate(username);
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
            entry.update();
        });

        langConfig.markAsOnline.send(source, username);
        return 1;
    }

    /**
     * Retrieves information about a player from the database.
     *
     * @param source   executioner of the command
     * @param username username of the player to get information for
     * @return 0
     */
    private static int getPlayerInfo(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserData(username);
            if (playerData == null) {
                langConfig.userNotRegistered.send(source);
                return;
            }
            // Send player information to the source
            source.sendMessage(Text.literal("Player Info: " + playerData.toJson()));
        });
        return 1;
    }

}
