//~ resource_location
package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.FabricPermissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.integrations.MojangApi.isValidUsername;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getUsername;

public class AuthCommand {
    /**
     * Registers the "/auth" command
     *
     * @param dispatcher
     */
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("auth")
                .requires(FabricPermissions.require("easyauth.commands.auth.root", 3))
                .executes(ctx -> openAdminPanel(ctx.getSource()))
                .then(literal("gui")
                        .requires(FabricPermissions.require("easyauth.commands.auth.root", 3))
                        .executes(ctx -> openAdminPanel(ctx.getSource()))
                )
                .then(literal("reload")
                        .requires(FabricPermissions.require("easyauth.commands.auth.reload", 3))
                        .executes(ctx -> reloadConfig(ctx.getSource()))
                )
                .then(literal("setGlobalPassword")
                        .requires(FabricPermissions.require("easyauth.commands.auth.setGlobalPassword", 4))
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
                        .requires(FabricPermissions.require("easyauth.commands.auth.setSpawn", 3))
                        .executes(ctx -> setSpawn(
                                ctx.getSource(),
                                StoneCutterUtils.getWorld(ctx.getSource().getEntityOrException()).dimension().identifier(),
                                ctx.getSource().getEntityOrException().getX(),
                                ctx.getSource().getEntityOrException().getY(),
                                ctx.getSource().getEntityOrException().getZ(),
                                ctx.getSource().getEntityOrException().getYRot(),
                                ctx.getSource().getEntityOrException().getXRot()
                        ))
                        .then(argument("dimension", DimensionArgument.dimension())
                                .then(argument("position", BlockPosArgument.blockPos())
                                        .executes(ctx -> setSpawn(
                                                        ctx.getSource(),
                                                        DimensionArgument.getDimension(ctx, "dimension").dimension().identifier(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "position").getX(),
                                                        // +1 to not spawn player in ground
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "position").getY(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "position").getZ(),
                                                        90,
                                                        0
                                                )
                                        )
                                        .then(argument("angle", RotationArgument.rotation())
                                                .executes(ctx -> setSpawn(
                                                                ctx.getSource(),
                                                                DimensionArgument.getDimension(ctx, "dimension").dimension().identifier(),
                                                                 BlockPosArgument.getLoadedBlockPos(ctx, "position").getX(),
                                                                // +1 to not spawn player in ground
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "position").getY(),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "position").getZ(),
                                                                //? if >= 1.21.2 {
                                                                RotationArgument.getRotation(ctx, "angle").getRotation(ctx.getSource()).y,
                                                                RotationArgument.getRotation(ctx, "angle").getRotation(ctx.getSource()).x
                                                                //?} else {
                                                                /*RotationArgument.getRotation(ctx, "angle").getRotation(ctx.getSource()).y,
                                                                RotationArgument.getRotation(ctx, "angle").getRotation(ctx.getSource()).x
                                                                *///?}
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(literal("remove")
                        .requires(FabricPermissions.require("easyauth.commands.auth.remove", 3))
                        .then(argument("username", word())
                                .executes(ctx -> removeAccount(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("register")
                        .requires(FabricPermissions.require("easyauth.commands.auth.register", 3))
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
                        .requires(FabricPermissions.require("easyauth.commands.auth.update", 3))
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
                        .requires(FabricPermissions.require("easyauth.commands.auth.list", 3))
                        .executes(ctx -> getRegisteredPlayers(ctx.getSource()))
                )
                .then(literal("markAsOffline")
                        .requires(FabricPermissions.require("easyauth.commands.auth.markAsOffline", 3))
                        .then(argument("username", word())
                                .executes(ctx -> markAsOffline(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("markAsOnline")
                        .requires(FabricPermissions.require("easyauth.commands.auth.markAsOnline", 3))
                        .then(argument("username", word())
                                .executes(ctx -> markAsOnline(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("getPlayerInfo")
                        .requires(FabricPermissions.require("easyauth.commands.auth.getPlayerInfo", 3))
                        .then(argument("username", word())
                                .executes(ctx -> getPlayerInfo(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("getOnlinePlayers")
                        .requires(FabricPermissions.require("easyauth.commands.auth.getOnlinePlayers", 3))
                        .executes(ctx -> getOnlinePlayers(ctx.getSource()))
                )
                .then(literal("setUuid")
                        .requires(FabricPermissions.require("easyauth.commands.auth.setUuid", 4))
                        .then(argument("username", word())
                                .then(argument("uuid", string())
                                        .executes(ctx -> setUuid(
                                                ctx.getSource(),
                                                getString(ctx, "username"),
                                                getString(ctx, "uuid")
                                        ))
                                )
                        )
                )
                .then(literal("clearUuid")
                        .requires(FabricPermissions.require("easyauth.commands.auth.clearUuid", 4))
                        .then(argument("username", word())
                                .executes(ctx -> clearUuid(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("getUuid")
                        .requires(FabricPermissions.require("easyauth.commands.auth.getUuid", 3))
                        .then(argument("username", word())
                                .executes(ctx -> getUuid(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
        );
    }

    // Opens the admin panel window, or prints the subcommand hint when dialogs are off.
    private static int openAdminPanel(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        //? if >= 1.21.6 {
        if (dialogConfig.enabled && dialogConfig.admin) {
            xyz.nikitacartes.easyauth.dialog.AuthDialogs.openAdminMenu(source.getPlayerOrException());
            return 1;
        }
        //?}
        langConfig.dialog.admin.usage.send(source);
        return 1;
    }

    // Sets the login spawn to the caller's current position (the panel's "set spawn here" button).
    public static int setSpawnHere(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return setSpawn(source,
                StoneCutterUtils.getWorld(source.getEntityOrException()).dimension().identifier(),
                source.getEntityOrException().getX(),
                source.getEntityOrException().getY(),
                source.getEntityOrException().getZ(),
                source.getEntityOrException().getYRot(),
                source.getEntityOrException().getXRot());
    }

    /**
     * Reloads the config file.
     *
     * @param sender executioner of the command
     * @return 0
     */
    public static int reloadConfig(CommandSourceStack sender) {
        reloadConfigs(sender.getServer());

        langConfig.admin.configReloaded.send(sender);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sets global password.
     *
     * @param source   executioner of the command
     * @param password password that will be set
     * @param singleUse whether the global password is single-use
     * @return 0
     */
    public static int setGlobalPassword(CommandSourceStack source, String password, boolean singleUse) {
        // Writing the global pass to config
        technicalConfig.globalPassword = AuthHelper.hashPassword(password.toCharArray());
        config.enableGlobalPassword = true;
        config.singleUseGlobalPassword = singleUse;
        technicalConfig.save();
        config.save();

        reloadConfigs(source.getServer());

        langConfig.password.globalSet.send(source);
        return 1;
    }

    /**
     * @param source executioner of the command
     * @param world  world id of global spawn
     * @param x      x coordinate of the global spawn
     * @param y      y coordinate of the global spawn
     * @param z      z coordinate of the global spawn
     * @param yaw    player yaw (y rotation)
     * @param pitch  player pitch (x rotation)
     * @return 0
     */
    public static int setSpawn(CommandSourceStack source, Identifier world, double x, double y, double z, float yaw, float pitch) {
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

        langConfig.admin.spawnSet.send(source);
        return 1;
    }

    /**
     * Deletes (unregisters) player's account.
     *
     * @param source   executioner of the command
     * @param username username of the player to delete account for
     * @return 0
     */
    public static int removeAccount(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerEntry = DB.getUserData(username);
            if (playerEntry == null) {
                langConfig.registration.notRegistered.send(source);
                return;
            }

            if (DB.deleteUserData(username)) {
                langConfig.account.dataDeleted.send(source);
            } else {
                langConfig.error.database.send(source);
            }
        });

        ServerPlayer playerEntity = source.getServer().getPlayerList().getPlayerByName(username);
        if (playerEntity != null && getUsername(playerEntity).equals(username)) {
            ((PlayerAuth) playerEntity).easyAuth$setAuthenticated(false);
            ((PlayerAuth) playerEntity).easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
            playerEntity.connection.disconnect(langConfig.account.dataDeleted.get());
        }

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
    public static int registerUser(CommandSourceStack source, String username, String password) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserDataOrCreate(username);
            playerData.password = AuthHelper.hashPassword(password.toCharArray());
            playerData.registrationDate = ZonedDateTime.now();
            playerData.update();

            langConfig.account.dataUpdated.send(source);
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
    public static int updatePassword(CommandSourceStack source, String username, String password) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserData(username);
            if (playerData == null || playerData.password.isEmpty()) {
                langConfig.registration.notRegistered.send(source);
                return;
            }
            String newPasswordHash = AuthHelper.hashPassword(password.toCharArray());
            playerData.password = newPasswordHash;
            playerData.update();
            
            // Also update the cached PlayerEntryV1 if the player is online
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(username);
            if (player != null) {
                PlayerEntryV1 cachedEntry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
                if (cachedEntry != null) {
                    cachedEntry.password = newPasswordHash;
                }
            }
            
            langConfig.account.dataUpdated.send(source);
        });
        return 0;
    }

    /**
     * List of registered username
     *
     * @param source executioner of the command
     * @return 0
     */
    public static int getRegisteredPlayers(CommandSourceStack source) {
        THREADPOOL.submit(() -> {
            if (langConfig.admin.registeredPlayers.enabled) {
                AtomicInteger i = new AtomicInteger();
                MutableComponent message = langConfig.admin.registeredPlayers.get();
                DB.getAllData().forEach((username, playerData) -> {
                    if (playerData == null || playerData.password == null) {
                        return;
                    }
                    i.getAndIncrement();
                    message.append(Component.translatable(username)
                            //? if >= 1.21.5 {
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(username)))
                            //?} else {
                            /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, username)))
                            *///?}
                            .withStyle(ChatFormatting.YELLOW))
                            .append(", ");
                });
                source.sendSystemMessage(message);
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
    public static int markAsOffline(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserDataOrCreate(username);
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
            entry.update();
        });

        langConfig.admin.markedOffline.send(source, username);
        return 1;
    }

    /**
     * Set player as player with online account
     *
     * @param source   executioner of the command
     * @param username player to add in list
     * @return 0
     */
    public static int markAsOnline(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            try {
                if (!isValidUsername(username)) {
                    langConfig.account.onlineNotFound.send(source);
                    return;
                }
            } catch (IOException e) {
                langConfig.error.mojangUnavailable.send(source);
                return;
            }

            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(username);
            PlayerEntryV1 entry;
            if (player != null) {
                entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
            } else {
                entry = DB.getUserDataOrCreate(username);
            }
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
            entry.update();

            langConfig.admin.markedOnline.send(source, username);
        });
        return 1;
    }

    /**
     * Retrieves information about a player from the database.
     *
     * @param source   executioner of the command
     * @param username username of the player to get information for
     * @return 0
     */
    public static int getPlayerInfo(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 playerData = DB.getUserData(username);
            if (playerData == null) {
                langConfig.registration.notRegistered.send(source);
                return;
            }
            // Send player information to the source
            source.sendSystemMessage(Component.literal("Player Info: " + playerData.toJson()));
        });
        return 1;
    }

    /**
     * Gets info about all online players
     *
     * @param source executioner of the command
     */
    public static int getOnlinePlayers(CommandSourceStack source) {
        THREADPOOL.submit(() -> {
            MutableComponent message = Component.literal("");
            source.getServer().getPlayerList().getPlayers().forEach(player -> {
                String username = getUsername(player);
                PlayerEntryV1 playerData = DB.getUserData(username);
                PlayerAuth playerAuth = (PlayerAuth) player;

                message.append(Component.translatable(username).withStyle(ChatFormatting.YELLOW)).append(": ");
                if (playerData == null) {
                    message.append(Component.literal("No data found\n"));
                    return;
                }
                message.append(Component.literal("authenticated: " + playerAuth.easyAuth$isAuthenticated() + "; Mojang account: " + playerAuth.easyAuth$isUsingMojangAccount() + "\n"));
            });
            source.sendSystemMessage(message);
        });
        return 1;
    }

    /**
     * Sets a forced UUID for a player.
     * This UUID will be used instead of the default offline/online UUID when the player joins.
     *
     * @param source   executioner of the command
     * @param username username of the player
     * @param uuidStr  the UUID to force for this player
     * @return 1 on success
     */
    public static int setUuid(CommandSourceStack source, String username, String uuidStr) {
        THREADPOOL.submit(() -> {
            // Validate UUID format
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                langConfig.uuid.invalidFormat.send(source, uuidStr);
                return;
            }

            PlayerEntryV1 entry = DB.getUserDataOrCreate(username);
            entry.forcedUuid = uuid.toString();
            entry.update();

            langConfig.uuid.forcedSet.send(source, username, uuid.toString());

            // Kick the player if online so they rejoin with the new UUID
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(username);
            if (player != null) {
                player.connection.disconnect(langConfig.uuid.changed.get());
            }
        });
        return 1;
    }

    /**
     * Clears the forced UUID for a player, reverting to default behavior.
     *
     * @param source   executioner of the command
     * @param username username of the player
     * @return 1 on success
     */
    public static int clearUuid(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserData(username);
            if (entry == null) {
                langConfig.registration.notRegistered.send(source);
                return;
            }

            if (entry.forcedUuid == null || entry.forcedUuid.isEmpty()) {
                langConfig.uuid.noForced.send(source, username);
                return;
            }

            entry.forcedUuid = null;
            entry.update();

            langConfig.uuid.forcedCleared.send(source, username);

            // Kick the player if online so they rejoin with the default UUID
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(username);
            if (player != null) {
                player.connection.disconnect(langConfig.uuid.changed.get());
            }
        });
        return 1;
    }

    /**
     * Gets UUID information for a player.
     *
     * @param source   executioner of the command
     * @param username username of the player
     * @return 1 on success
     */
    public static int getUuid(CommandSourceStack source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserData(username);
            
            UUID offlineUuid = UUIDUtil.createOfflinePlayerUUID(username);
            
            MutableComponent message = Component.literal("");
            message.append(Component.literal("UUID info for ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(username).withStyle(ChatFormatting.YELLOW));
            message.append(Component.literal(":\n").withStyle(ChatFormatting.GRAY));
            
            // Offline UUID
            message.append(Component.literal("  Offline UUID: ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(offlineUuid.toString()).withStyle(ChatFormatting.WHITE)
                    //? if >= 1.21.5 {
                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(offlineUuid.toString())))
                    //?} else {
                    /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, offlineUuid.toString())))
                    *///?}
            );
            message.append(Component.literal("\n"));
            
            // Forced UUID
            message.append(Component.literal("  Forced UUID: ").withStyle(ChatFormatting.GRAY));
            if (entry != null && entry.forcedUuid != null && !entry.forcedUuid.isEmpty()) {
                message.append(Component.literal(entry.forcedUuid).withStyle(ChatFormatting.GREEN)
                        //? if >= 1.21.5 {
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(entry.forcedUuid)))
                        //?} else {
                        /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.forcedUuid)))
                        *///?}
                );
            } else {
                message.append(Component.literal("(none)").withStyle(ChatFormatting.DARK_GRAY));
            }
            message.append(Component.literal("\n"));
            
            // Current UUID (if online)
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(username);
            message.append(Component.literal("  Current UUID: ").withStyle(ChatFormatting.GRAY));
            if (player != null) {
                String currentUuid = player.getStringUUID();
                message.append(Component.literal(currentUuid).withStyle(ChatFormatting.AQUA)
                        //? if >= 1.21.5 {
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(currentUuid)))
                        //?} else {
                        /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, currentUuid)))
                        *///?}
                );
            } else {
                message.append(Component.literal("(player offline)").withStyle(ChatFormatting.DARK_GRAY));
            }
            
            source.sendSystemMessage(message);
        });
        return 1;
    }
}
