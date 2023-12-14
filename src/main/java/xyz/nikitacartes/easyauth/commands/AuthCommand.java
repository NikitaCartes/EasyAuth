package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.deprecated.AuthConfig;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.storage.database.DBApiException;
import xyz.nikitacartes.easyauth.utils.AuthHelper;

import java.util.Locale;
import java.util.UUID;

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
                                        getString(ctx, "password")
                                ))
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
                                                                RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).y,
                                                                RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).x
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(literal("remove")
                        .requires(Permissions.require("easyauth.commands.auth.remove", 3))
                        .then(argument("uuid", word())
                                .executes(ctx -> removeAccount(
                                        ctx.getSource(),
                                        getString(ctx, "uuid")
                                ))
                        )
                )
                .then(literal("register")
                        .requires(Permissions.require("easyauth.commands.auth.register", 3))
                        .then(argument("uuid", word())
                                .then(argument("password", string())
                                        .executes(ctx -> registerUser(
                                                ctx.getSource(),
                                                getString(ctx, "uuid"),
                                                getString(ctx, "password")
                                        ))
                                )
                        )
                )
                .then(literal("update")
                        .requires(Permissions.require("easyauth.commands.auth.update", 3))
                        .then(argument("uuid", word())
                                .then(argument("password", string())
                                        .executes(ctx -> updatePassword(
                                                ctx.getSource(),
                                                getString(ctx, "uuid"),
                                                getString(ctx, "password")
                                        ))
                                )
                        )
                )
                .then(literal("uuid")
                        .requires(Permissions.require("easyauth.commands.auth.uuid", 3))
                        .then(argument("player", word())
                                .executes(ctx -> getOfflineUuid(
                                        ctx.getSource(),
                                        getString(ctx, "player")
                                ))
                        )
                )
                .then(literal("list")
                        .requires(Permissions.require("easyauth.commands.auth.list", 3))
                        .executes(ctx -> getRegisteredPlayers(ctx.getSource()))
                )
                .then(literal("addToForcedOffline")
                        .requires(Permissions.require("easyauth.commands.auth.addToForcedOffline", 3))
                        .then(argument("player", word())
                                .executes(ctx -> addPlayerToForcedOffline(
                                        ctx.getSource(),
                                        getString(ctx, "player")
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
     * @return 0
     */
    private static int setGlobalPassword(ServerCommandSource source, String password) {
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            // Writing the global pass to config
            technicalConfig.globalPassword = AuthHelper.hashPassword(password.toCharArray());
            config.enableGlobalPassword = true;
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
     * @param source executioner of the command
     * @param uuid   uuid of the player to delete account for
     * @return 0
     */
    private static int removeAccount(ServerCommandSource source, String uuid) {
        THREADPOOL.submit(() -> {
            DB.deleteUserData(uuid);
            playerCacheMap.remove(uuid);
        });

        langConfig.userdataDeleted.send(source);
        return 1; // Success
    }

    /**
     * Creates account for player.
     *
     * @param source   executioner of the command
     * @param uuid     uuid of the player to create account for
     * @param password new password for the player account
     * @return 0
     */
    private static int registerUser(ServerCommandSource source, String uuid, String password) {
        THREADPOOL.submit(() -> {
            PlayerCache playerCache;
            if (playerCacheMap.containsKey(uuid)) {
                playerCache = playerCacheMap.get(uuid);
            } else {
                playerCache = PlayerCache.fromJson(null, uuid);
            }

            playerCacheMap.put(uuid, playerCache);
            playerCacheMap.get(uuid).password = AuthHelper.hashPassword(password.toCharArray());

            langConfig.userdataUpdated.send(source);
        });
        return 0;
    }

    /**
     * Force-updates the player's password.
     *
     * @param source   executioner of the command
     * @param uuid     uuid of the player to update data for
     * @param password new password for the player
     * @return 0
     */
    private static int updatePassword(ServerCommandSource source, String uuid, String password) {
        THREADPOOL.submit(() -> {
            PlayerCache playerCache;
            if (playerCacheMap.containsKey(uuid)) {
                playerCache = playerCacheMap.get(uuid);
            } else {
                playerCache = PlayerCache.fromJson(null, uuid);
            }

            playerCacheMap.put(uuid, playerCache);
            if (playerCacheMap.get(uuid).password.isEmpty()) {
                langConfig.userNotRegistered.send(source);
                return;
            }
            playerCacheMap.get(uuid).password = AuthHelper.hashPassword(password.toCharArray());

            langConfig.userdataUpdated.send(source);
        });
        return 0;
    }

    /**
     * Return offline uuid for player in lowercase
     *
     * @param source executioner of the command
     * @param player player to get uuid from
     * @return 0
     */
    private static int getOfflineUuid(ServerCommandSource source, String player) {
        UUID uuid = Uuids.getOfflinePlayerUuid(player.toLowerCase(Locale.ROOT));

        langConfig.offlineUuid.send(source, player, Text.literal("[" + uuid + "]").setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
                .withColor(Formatting.YELLOW)));
        return 1;
    }

    /**
     * List of registered uuid
     *
     * @param source executioner of the command
     * @return 0
     */
    public static int getRegisteredPlayers(ServerCommandSource source) {
        THREADPOOL.submit(() -> {
            if (langConfig.registeredPlayers.enabled) {
                int i = 0;
                MutableText message = langConfig.registeredPlayers.get();
                for (var entry : playerCacheMap.entrySet()) {
                    if (!entry.getValue().password.isEmpty()) {
                        i++;
                        message.append(Text.translatable("\n" + i + ": [" + entry.getKey() + "]")
                                .setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey())))
                                .formatted(Formatting.YELLOW));
                    }
                }
                source.sendMessage(message);
            }
        });
        return 1;
    }


    /**
     * Add player in forcedOfflinePlayers list
     *
     * @param source executioner of the command
     * @param player player to add in list
     * @return 0
     */
    private static int addPlayerToForcedOffline(ServerCommandSource source, String player) {
        THREADPOOL.submit(() -> {
            technicalConfig.forcedOfflinePlayers.add(player.toLowerCase(Locale.ROOT));
            technicalConfig.confirmedOnlinePlayers.remove(player.toLowerCase(Locale.ROOT));
            technicalConfig.save();
        });

        langConfig.addToForcedOffline.send(source);
        return 1;
    }

}
