package xyz.nikitacartes.easyauth.event;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.integrations.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;
import xyz.nikitacartes.easyauth.utils.PlayersCache;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {

    public static Pattern usernamePattern;

    private static final Map<UUID, Long> lastAcceptedPacketByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> administratorCache = new ConcurrentHashMap<>();

    public static boolean isAllowedPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (packet instanceof KeepAliveC2SPacket
                || packet instanceof ResourcePackStatusC2SPacket
                || packet instanceof TeleportConfirmC2SPacket
                || packet instanceof PlayerSessionC2SPacket
                || packet instanceof MessageAcknowledgmentC2SPacket
                || packet instanceof ClientStatusC2SPacket
                || packet instanceof RequestCommandCompletionsC2SPacket
                || packet instanceof CommandExecutionC2SPacket
                || packet instanceof QueryPingC2SPacket
                || packet instanceof PlayerLoadedC2SPacket
                || packet instanceof ClientTickEndC2SPacket
                || packet instanceof CookieResponseC2SPacket
                || packet instanceof ChatCommandSignedC2SPacket
                || packet instanceof CommonPongC2SPacket
                || packet instanceof ClientOptionsC2SPacket
                || packet instanceof AcknowledgeChunksC2SPacket
                || packet instanceof AcknowledgeReconfigurationC2SPacket
        ) {
            return true;
        }

        // Movement packets are handled separately
        if (packet instanceof PlayerMoveC2SPacket ||
                packet instanceof PlayerMoveC2SPacket.Full ||
                packet instanceof PlayerMoveC2SPacket.LookAndOnGround ||
                packet instanceof PlayerMoveC2SPacket.OnGroundOnly ||
                packet instanceof PlayerMoveC2SPacket.PositionAndOnGround ||
                packet instanceof VehicleMoveC2SPacket ||
                packet instanceof PlayerInputC2SPacket) {
            return true;
        }

        if (extendedConfig.allowChat && packet instanceof ChatMessageC2SPacket) {
            return true;
        }

        if (extendedConfig.allowBlockInteraction && packet instanceof PlayerInteractBlockC2SPacket) {
            return true;
        }

        if (extendedConfig.allowEntityInteraction && packet instanceof PlayerInteractEntityC2SPacket) {
            return true;
        }

        if (extendedConfig.allowItemUsing && packet instanceof PlayerInteractItemC2SPacket) {
            return true;
        }

        if (packet instanceof HandSwingC2SPacket) {
            return extendedConfig.allowBlockInteraction || extendedConfig.allowEntityInteraction || extendedConfig.allowEntityAttacking;
        }
        
        if (packet instanceof PlayerActionC2SPacket actionPacket) {
            var action = actionPacket.getAction();
            if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK ||
                    action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK ||
                    action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                return extendedConfig.allowBlockBreaking;
            }
            if (action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS ||
                    action == PlayerActionC2SPacket.Action.DROP_ITEM) {
                return extendedConfig.allowItemDropping;
            }
            if (action == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                return extendedConfig.allowItemMoving;
            }
            if (action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                return extendedConfig.allowItemUsing;
            }
            return false;
        }

        if (extendedConfig.allowItemMoving && (
                packet instanceof ClickSlotC2SPacket ||
                packet instanceof CreativeInventoryActionC2SPacket ||
                packet instanceof UpdateSelectedSlotC2SPacket ||
                packet instanceof CloseHandledScreenC2SPacket ||
                packet instanceof ButtonClickC2SPacket
        )) {
            return true;
        }

        if (packet instanceof CustomPayloadC2SPacket) {
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            if (extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player)) {
                return true;
            }

            String customPacketIdentifier = ((CustomPayloadC2SPacket) packet).payload().getId().id().toString();

            if (isAllowedCustomPacket(customPacketIdentifier)) {
                return true;
            }

            if (config.debug) {
                LogDebug("Blocked custom packet " + customPacketIdentifier);
            }
        }

        if (packet instanceof CustomClickActionC2SPacket) {
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            return extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player);
        }

        return false;
    }

    public static boolean isAdministratorCached(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        return administratorCache.computeIfAbsent(playerUuid, ignored -> StoneCutterUtils.isAdministrator(player.server.getPlayerManager(), player));
    }

    private static boolean isAllowedCustomPacket(String packetIdentifier) {
        if (packetIdentifier == null || extendedConfig.allowedCustomPackets == null) {
            return false;
        }

        for (String allowedPacketIdentifier : extendedConfig.allowedCustomPackets) {
            if (packetIdentifier.equals(allowedPacketIdentifier)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Player pre-join.
     * Returns text as a reason for disconnect or null to pass
     *
     * @param profile PlayerConfigEntry|GameProfile of the player
     * @param manager PlayerManager
     * @return Text if player should be disconnected
     */
    public static Text checkCanPlayerJoinServer(PlayerConfigEntry profile, PlayerManager manager, SocketAddress socketAddress) {
        // Getting the player. By this point, the player's game profile has been authenticated so the UUID is legitimate.
        String incomingPlayerUsername = StoneCutterUtils.getName(profile);
        PlayerEntity onlinePlayer = manager.getPlayer(incomingPlayerUsername);

        String ip = socketAddress.toString();
        if (ip.contains("/")) {
            ip = ip.substring(ip.indexOf(47) + 1);
        }

        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(58));
        }

        // Player needs to be kicked, since there's already a player with that name
        // playing on the server
        if ((onlinePlayer != null) && ((PlayerAuth) onlinePlayer).easyAuth$isAuthenticated() && extendedConfig.preventAnotherLocationKick) {

            // if joining from same IP, allow the player to join
            if (!((PlayerAuth) onlinePlayer).easyAuth$getIpAddress().equals(ip)) {
                return langConfig.playerAlreadyOnline.getNonTranslatable(incomingPlayerUsername);
            }
        }

        // Checking if player username is valid. The pattern is generated when the config is (re)loaded.
        Matcher matcher = usernamePattern.matcher(incomingPlayerUsername);

        if (!(matcher.matches() || (extendedConfig.floodgateBypassRegex && FloodgateApiHelper.isFloodgatePlayer(StoneCutterUtils.getId(profile))))) {
            return langConfig.disallowedUsername.getNonTranslatable(extendedConfig.usernameRegexp);
        }
        // If the player name and registered name are different, kick the player if differentUsernameCase is enabled
        // Create in case of Floodgate player
        PlayerEntryV1 playerEntryV1 = PlayersCache.getOrLoadOrRegister(incomingPlayerUsername);

        if (!extendedConfig.allowCaseInsensitiveUsername && !playerEntryV1.username.equals(incomingPlayerUsername)) {
            return langConfig.differentUsernameCase.getNonTranslatable(incomingPlayerUsername);
        }

        if (config.maxLoginTries != -1 && playerEntryV1.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
            return langConfig.loginTriesExceeded.getNonTranslatable();
        }

        // Check concurrent session limit per IP
        boolean isOnlinePlayer = config.premiumAutoLogin && (onlinePlayer != null) && ((PlayerAuth) onlinePlayer).easyAuth$isUsingMojangAccount();
        if (IpLimitManager.isConcurrentSessionLimitExceeded(manager.getServer(), ip, isOnlinePlayer)) {
            LogDebug("Player " + incomingPlayerUsername + " blocked: concurrent session limit exceeded for IP " + ip);
            IpLimitManager.notifyAdmins(manager.getServer(), ip, incomingPlayerUsername);
            return langConfig.sessionLimitExceeded.getNonTranslatable();
        }

        return null;
    }

    public static void loadPlayerData(ServerPlayerEntity player, ClientConnection connection) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        UUID playerUuid = player.getUuid();
        PlayerManager playerManager = player.server.getPlayerManager();
        administratorCache.put(playerUuid, StoneCutterUtils.isAdministrator(playerManager, player));

        // Create in case of Carpet player
        String username = StoneCutterUtils.getUsername(player);
        PlayerEntryV1 cache = PlayersCache.getOrCreate(username);
        boolean update = false;
        if (cache.uuid == null) {
            cache.uuid = player.getUuid();
            update = true;
        }
        playerAuth.easyAuth$setPlayerEntryV1(cache);

        playerAuth.easyAuth$setIpAddress(connection);
        playerAuth.easyAuth$setSkipAuth();

        if (config.vanishUntilAuth) {
            ((PlayerAuth) player).easyAuth$wasVanished(VanishIntegration.isVanished(player));
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            playerAuth.easyAuth$setAuthenticated(true);

            update = false;
        } else if (cache.lastIp.equals(playerAuth.easyAuth$getIpAddress()) && cache.lastAuthenticatedDate.plusSeconds(config.sessionTimeout).isAfter(ZonedDateTime.now())) {
            playerAuth.easyAuth$setAuthenticated(true);

            cache.lastAuthenticatedDate = ZonedDateTime.now();
            update = true;
        }

        if (update) {
            cache.update();
        }

        if (isSkipAllAuthChecksApplicable(player)) {
            playerAuth.easyAuth$setAuthenticated(true);
        }

        if (config.vanishUntilAuth && !playerAuth.easyAuth$isAuthenticated()) {
            VanishIntegration.setVanished(player, true);
        }
    }

    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.onlinePlayerLogin.send(player);
            return;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.validSession.send(player);
            return;
        } else if (isSkipAllAuthChecksApplicable(player)) {
            return;
        }

        // Tries to rescue player from nether portal
        if (extendedConfig.tryPortalRescue) {
            BlockPos pos = player.getBlockPos();
            player.teleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5, false);
            if (player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL) || StoneCutterUtils.getServerWorld(player).getBlockState(player.getBlockPos().up()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                // Faking portal blocks to be air
                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);

                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        administratorCache.remove(playerUuid);
        lastAcceptedPacketByPlayer.remove(playerUuid);

        PlayerAuth playerAuth = (PlayerAuth) player;
        if (playerAuth.easyAuth$canSkipAuth())
            return;

        if (playerAuth.easyAuth$isAuthenticated()) {
            PlayerEntryV1 playerCache = playerAuth.easyAuth$getPlayerEntryV1();
            playerCache.lastAuthenticatedDate = ZonedDateTime.now();
            playerCache.update();
            return;
        }
        if (config.hidePlayerCoords) {
            ((PlayerAuth) player).easyAuth$restoreTrueLocation();
        }
    }

    public static boolean isSkipAllAuthChecksApplicable(ServerPlayerEntity player) {
        if (!extendedConfig.skipAllAuthChecks) {
            return false;
        }

        PlayerAuth playerAuth = (PlayerAuth) player;
        if (extendedConfig.skipAllAuthChecksNotForRegisteredPlayers && !playerAuth.easyAuth$getPlayerEntryV1().password.isEmpty()) {
            return false;
        }

        if (extendedConfig.skipAllAuthChecksNotForOperators && isAdministratorCached(player)) {
            return false;
        }

        return true;
    }

    // Player execute command
    public static ActionResult onPlayerCommand(ServerPlayerEntity player, String command) {
        // Getting the message to then be able to check it
        if (extendedConfig.allowCommands) {
            return ActionResult.PASS;
        }
        if (player == null) {
            return ActionResult.PASS;
        }
        if (command == null) {
            return ActionResult.PASS;
        }
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return ActionResult.PASS;
        }

        if (command.startsWith("login ")
                || command.startsWith("register ")
                || (extendedConfig.aliases.login && command.startsWith("l "))
                || (extendedConfig.aliases.register && command.startsWith("reg "))) {
            return ActionResult.PASS;
        }

        String normalizedCommand = command.trim();
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }

        String lowercaseCommand = normalizedCommand.toLowerCase(Locale.ENGLISH);
        if (lowercaseCommand.equals("op")
                || lowercaseCommand.startsWith("op ")
                || lowercaseCommand.equals("minecraft:op")
                || lowercaseCommand.startsWith("minecraft:op ")
                || lowercaseCommand.equals("deop")
                || lowercaseCommand.startsWith("deop ")
                || lowercaseCommand.equals("minecraft:deop")
                || lowercaseCommand.startsWith("minecraft:deop ")) {
            administratorCache.clear();
        }

        String username = StoneCutterUtils.getUsername(player);
        for (String allowedCommand : extendedConfig.allowedCommands) {
            if (command.startsWith(allowedCommand)) {
                LogDebug("Player " + username + " executed command " + command + " without being authenticated.");
                return ActionResult.PASS;
            }
        }
        LogDebug("Player " + username + " tried to execute command " + command + " without being authenticated.");
        ((PlayerAuth) player).easyAuth$sendAuthMessage();
        return ActionResult.FAIL;
    }

    // Player chatting
    public static ActionResult onPlayerChat(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowChat) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(ServerPlayerEntity player) {
        // Player will fall if enabled (prevent fly kick)
        // Otherwise, movement should be disabled
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowMovement) {
            UUID playerUuid = player.getUuid();
            long now = System.nanoTime();
            long lastAcceptedPacket = lastAcceptedPacketByPlayer.getOrDefault(playerUuid, 0L);
            if (now >= lastAcceptedPacket + extendedConfig.teleportationTimeoutMs * 1_000_000L) {
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                lastAcceptedPacketByPlayer.put(playerUuid, now);
            }
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Breaking a block
    public static boolean onBreakBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockBreaking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return false;
        }
        return true;
    }

    // Using an item
    public static ActionResult onUseItem(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemUsing) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemDropping) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemMoving) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityAttacking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    public static void onPreLogin(ServerLoginNetworkHandler netHandler) {
        if (extendedConfig.forcedOfflineUuid && netHandler.profile != null) {
            netHandler.profile = Uuids.getOfflinePlayerProfile(netHandler.profile.name());
        }
    }

}