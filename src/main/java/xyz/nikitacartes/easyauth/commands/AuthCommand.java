package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import xyz.nikitacartes.easyauth.config.deprecated.AuthConfig;
import xyz.nikitacartes.easyauth.integrations.Permissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.integrations.MojangApi.isValidUsername;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogLogin;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getUsername;

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
                                StoneCutterUtils.getWorld(ctx.getSource().getEntityOrThrow()).getRegistryKey().getValue(),
                                ctx.getSource().getEntityOrThrow().getX(),
                                ctx.getSource().getEntityOrThrow().getY(),
                                ctx.getSource().getEntityOrThrow().getZ(),
                                ctx.getSource().getEntityOrThrow().getYaw(),
                                ctx.getSource().getEntityOrThrow().getPitch()
                        ))
                        .then(argument("dimension", DimensionArgumentType.dimension())
                                .then(argument("position", BlockPosArgumentType.blockPos())
                                        .executes(ctx -> setSpawn(
                                                        ctx.getSource(),
                                                        DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue(),
                                                        BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getX(),
                                                        // +1 to not spawn player in ground
                                                        BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getY(),
                                                        BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getZ(),
                                                        90,
                                                        0
                                                )
                                        )
                                        .then(argument("angle", RotationArgumentType.rotation())
                                                .executes(ctx -> setSpawn(
                                                                ctx.getSource(),
                                                                DimensionArgumentType.getDimensionArgument(ctx, "dimension").getRegistryKey().getValue(),
                                                                BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getX(),
                                                                // +1 to not spawn player in ground
                                                                BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getY(),
                                                                BlockPosArgumentType.getLoadedBlockPos(ctx, "position").getZ(),
                                                                //? if >= 1.21.2 {
                                                                RotationArgumentType.getRotation(ctx, "angle").getRotation(ctx.getSource()).y,
                                                                RotationArgumentType.getRotation(ctx, "angle").getRotation(ctx.getSource()).x
                                                                //?} else {
                                                                /*RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).y,
                                                                RotationArgumentType.getRotation(ctx, "angle").toAbsoluteRotation(ctx.getSource()).x
                                                                *///?}
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
                .then(literal("getOnlinePlayers")
                        .requires(Permissions.require("easyauth.commands.auth.getOnlinePlayers", 3))
                        .executes(ctx -> getOnlinePlayers(ctx.getSource()))
                )
                .then(literal("setUuid")
                        .requires(Permissions.require("easyauth.commands.auth.setUuid", 4))
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
                        .requires(Permissions.require("easyauth.commands.auth.clearUuid", 4))
                        .then(argument("username", word())
                                .executes(ctx -> clearUuid(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("getUuid")
                        .requires(Permissions.require("easyauth.commands.auth.getUuid", 3))
                        .then(argument("username", word())
                                .executes(ctx -> getUuid(
                                        ctx.getSource(),
                                        getString(ctx, "username")
                                ))
                        )
                )
                .then(literal("forceLogin")
                        .requires(Permissions.require("easyauth.commands.auth.forceLogin", 3))
                        .then(argument("username", word())
                                .executes(ctx -> forceLogin(
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
        reloadConfigs(sender.getServer());

        langConfig.configurationReloaded.send(sender);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sets global password.
     *
     * @param source    executioner of the command
     * @param password  password that will be set
     * @param singleUse whether the global password is single-use
     * @return 0
     */
    private static int setGlobalPassword(ServerCommandSource source, String password, boolean singleUse) {
        // Writing the global pass to config
        technicalConfig.globalPassword = AuthHelper.hashPassword(password.toCharArray());
        config.enableGlobalPassword = true;
        config.singleUseGlobalPassword = singleUse;
        technicalConfig.save();
        config.save();

        reloadConfigs(source.getServer());

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
            PlayerEntryV1 playerEntry = DB.getUserData(username);
            if (playerEntry == null) {
                langConfig.userNotRegistered.send(source);
                return;
            }

            if (DB.deleteUserData(username)) {
                langConfig.userdataDeleted.send(source);
            } else {
                langConfig.databaseError.send(source);
            }
        });

        ServerPlayerEntity playerEntity = source.getServer().getPlayerManager().getPlayer(username);
        if (playerEntity != null && getUsername(playerEntity).equals(username)) {
            ((PlayerAuth) playerEntity).easyAuth$setAuthenticated(false);
            ((PlayerAuth) playerEntity).easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
            playerEntity.networkHandler.disconnect(langConfig.userdataDeleted.get());
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
            String newPasswordHash = AuthHelper.hashPassword(password.toCharArray());
            playerData.password = newPasswordHash;
            playerData.update();

            // Also update the cached PlayerEntryV1 if the player is online
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(username);
            if (player != null) {
                PlayerEntryV1 cachedEntry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
                if (cachedEntry != null) {
                    cachedEntry.password = newPasswordHash;
                }
            }

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
                                    //? if >= 1.21.5 {
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(username)))
                                    //?} else {
                                    /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, username)))
                                     *///?}
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
            try {
                if (!isValidUsername(username)) {
                    langConfig.accountNotFound.send(source);
                    return;
                }
            } catch (IOException e) {
                langConfig.accountCheckFailed.send(source);
                return;
            }

            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(username);
            PlayerEntryV1 entry;
            if (player != null) {
                entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
            } else {
                entry = DB.getUserDataOrCreate(username);
            }
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
            entry.update();

            langConfig.markAsOnline.send(source, username);
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

    /**
     * Gets info about all online players
     *
     * @param source executioner of the command
     */
    private static int getOnlinePlayers(ServerCommandSource source) {
        THREADPOOL.submit(() -> {
            MutableText message = Text.literal("");
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                String username = getUsername(player);
                PlayerEntryV1 playerData = DB.getUserData(username);
                PlayerAuth playerAuth = (PlayerAuth) player;

                message.append(Text.translatable(username).formatted(Formatting.YELLOW)).append(": ");
                if (playerData == null) {
                    message.append(Text.literal("No data found\n"));
                    return;
                }
                message.append(Text.literal("authenticated: " + playerAuth.easyAuth$isAuthenticated() + "; Mojang account: " + playerAuth.easyAuth$isUsingMojangAccount() + "\n"));
            });
            source.sendMessage(message);
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
    private static int setUuid(ServerCommandSource source, String username, String uuidStr) {
        THREADPOOL.submit(() -> {
            // Validate UUID format
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                langConfig.invalidUuid.send(source, uuidStr);
                return;
            }

            PlayerEntryV1 entry = DB.getUserDataOrCreate(username);
            entry.forcedUuid = uuid.toString();
            entry.update();

            langConfig.uuidSet.send(source, username, uuid.toString());

            // Kick the player if online so they rejoin with the new UUID
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(username);
            if (player != null) {
                player.networkHandler.disconnect(langConfig.uuidChanged.get());
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
    private static int clearUuid(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserData(username);
            if (entry == null) {
                langConfig.userNotRegistered.send(source);
                return;
            }

            if (entry.forcedUuid == null || entry.forcedUuid.isEmpty()) {
                langConfig.noForcedUuid.send(source, username);
                return;
            }

            entry.forcedUuid = null;
            entry.update();

            langConfig.uuidCleared.send(source, username);

            // Kick the player if online so they rejoin with the default UUID
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(username);
            if (player != null) {
                player.networkHandler.disconnect(langConfig.uuidChanged.get());
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
    private static int getUuid(ServerCommandSource source, String username) {
        THREADPOOL.submit(() -> {
            PlayerEntryV1 entry = DB.getUserData(username);

            UUID offlineUuid = Uuids.getOfflinePlayerUuid(username);

            MutableText message = Text.literal("");
            message.append(Text.literal("UUID info for ").formatted(Formatting.GRAY));
            message.append(Text.literal(username).formatted(Formatting.YELLOW));
            message.append(Text.literal(":\n").formatted(Formatting.GRAY));

            // Offline UUID
            message.append(Text.literal("  Offline UUID: ").formatted(Formatting.GRAY));
            message.append(Text.literal(offlineUuid.toString()).formatted(Formatting.WHITE)
                            //? if >= 1.21.5 {
                            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(offlineUuid.toString())))
                    //?} else {
                    /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, offlineUuid.toString())))
                     *///?}
            );
            message.append(Text.literal("\n"));

            // Forced UUID
            message.append(Text.literal("  Forced UUID: ").formatted(Formatting.GRAY));
            if (entry != null && entry.forcedUuid != null && !entry.forcedUuid.isEmpty()) {
                message.append(Text.literal(entry.forcedUuid).formatted(Formatting.GREEN)
                                //? if >= 1.21.5 {
                                .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(entry.forcedUuid)))
                        //?} else {
                        /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.forcedUuid)))
                         *///?}
                );
            } else {
                message.append(Text.literal("(none)").formatted(Formatting.DARK_GRAY));
            }
            message.append(Text.literal("\n"));

            // Current UUID (if online)
            ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(username);
            message.append(Text.literal("  Current UUID: ").formatted(Formatting.GRAY));
            if (player != null) {
                String currentUuid = player.getUuidAsString();
                message.append(Text.literal(currentUuid).formatted(Formatting.AQUA)
                                //? if >= 1.21.5 {
                                .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(currentUuid)))
                        //?} else {
                        /*.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, currentUuid)))
                         *///?}
                );
            } else {
                message.append(Text.literal("(player offline)").formatted(Formatting.DARK_GRAY));
            }

            source.sendMessage(message);
        });
        return 1;
    }

    /**
     * Forces a player to login.
     *
     * @param sender   executioner of the command
     * @param username username of the player
     * @return 1 on success
     */
    public static int forceLogin(ServerCommandSource sender, String username) {
        THREADPOOL.submit(() -> {
            ServerPlayerEntity player = sender.getServer().getPlayerManager().getPlayer(username);
            if (player == null) {
                langConfig.forceLoginPlayerOffline.send(sender, username);
                return;
            }
            PlayerAuth playerAuth = (PlayerAuth) player;
            LogLogin("Admin " + sender.getName() + " attempted to bypass the password login for player " + username);
            if (playerAuth.easyAuth$isAuthenticated()) {
                LogLogin("Player " + username + " is already authenticated");
                langConfig.alreadyAuthenticated.send(sender);
                return;
            }
            PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();
            AuthHelper.PasswordOptions passwordResult = AuthHelper.canForceLogin(playerAuth);
            switch (passwordResult) {
                case CORRECT -> {
                    playerAuth.easyAuth$setAuthenticated(true);
                    playerAuth.easyAuth$restoreTrueLocation();
                    playerData.lastAuthenticatedDate = ZonedDateTime.now();
                    playerData.loginTries = 0;
                    String oldIp = playerData.lastIp;
                    playerData.lastIp = playerAuth.easyAuth$getIpAddress();
                    playerData.update();

                    // Invalidate IP cache if IP changed
                    if (!oldIp.equals(playerData.lastIp)) {
                        IpLimitManager.invalidateCache(oldIp);
                        IpLimitManager.invalidateCache(playerData.lastIp);
                    }

                    langConfig.forceLoginSuccess.send(sender, username);
                    langConfig.successfullyAuthenticated.send(player);
                }
                case WRONG ->  // Indicates the player has been authenticated
                    langConfig.forceLoginIsAuthenticated.send(sender, username);
                
                case NOT_REGISTERED -> 
                    langConfig.userNotRegistered.send(sender);
            }
        });
        return 0;
    }
}
