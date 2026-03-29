package xyz.nikitacartes.easyauth.event;

import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.BlockPos;
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

    public static boolean isAllowedPacket(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ServerboundKeepAlivePacket
                || packet instanceof ServerboundResourcePackPacket
                || packet instanceof ServerboundAcceptTeleportationPacket
                || packet instanceof ServerboundChatSessionUpdatePacket
                || packet instanceof ServerboundChatAckPacket
                || packet instanceof ServerboundClientCommandPacket
                || packet instanceof ServerboundCommandSuggestionPacket
                || packet instanceof ServerboundChatCommandPacket
                || packet instanceof ServerboundPingRequestPacket
                || packet instanceof ServerboundPlayerLoadedPacket
                || packet instanceof ServerboundClientTickEndPacket
                || packet instanceof ServerboundCookieResponsePacket
                || packet instanceof ServerboundChatCommandSignedPacket
                || packet instanceof ServerboundPongPacket
                || packet instanceof ServerboundClientInformationPacket
                || packet instanceof ServerboundChunkBatchReceivedPacket
                || packet instanceof ServerboundConfigurationAcknowledgedPacket
        ) {
            return true;
        }

        // Movement packets are handled separately
        if (packet instanceof ServerboundMovePlayerPacket ||
                packet instanceof ServerboundMovePlayerPacket.PosRot ||
                packet instanceof ServerboundMovePlayerPacket.Rot ||
                packet instanceof ServerboundMovePlayerPacket.StatusOnly ||
                packet instanceof ServerboundMovePlayerPacket.Pos ||
                packet instanceof ServerboundMoveVehiclePacket ||
                packet instanceof ServerboundPlayerInputPacket) {
            return true;
        }

        if (extendedConfig.allowChat && packet instanceof ServerboundChatPacket) {
            return true;
        }

        if (extendedConfig.allowBlockInteraction && packet instanceof ServerboundUseItemOnPacket) {
            return true;
        }

        if (extendedConfig.allowEntityInteraction && packet instanceof ServerboundInteractPacket) {
            return true;
        }

        if (extendedConfig.allowItemUsing && packet instanceof ServerboundUseItemPacket) {
            return true;
        }

        if (packet instanceof ServerboundSwingPacket) {
            return extendedConfig.allowBlockInteraction || extendedConfig.allowEntityInteraction || extendedConfig.allowEntityAttacking;
        }
        
        if (packet instanceof ServerboundPlayerActionPacket actionPacket) {
            var action = actionPacket.getAction();
            if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ||
                    action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
                    action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                return extendedConfig.allowBlockBreaking;
            }
            if (action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS ||
                    action == ServerboundPlayerActionPacket.Action.DROP_ITEM) {
                return extendedConfig.allowItemDropping;
            }
            if (action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                return extendedConfig.allowItemMoving;
            }
            if (action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
                return extendedConfig.allowItemUsing;
            }
            return false;
        }

        if (extendedConfig.allowItemMoving && (
                packet instanceof ServerboundContainerClickPacket ||
                packet instanceof ServerboundSetCreativeModeSlotPacket ||
                packet instanceof ServerboundSetCarriedItemPacket ||
                packet instanceof ServerboundContainerClosePacket ||
                packet instanceof ServerboundContainerButtonClickPacket
        )) {
            return true;
        }

        if (packet instanceof ServerboundCustomPayloadPacket) {
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            if (extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player)) {
                return true;
            }

            String customPacketIdentifier = ((ServerboundCustomPayloadPacket) packet).payload().type().id().toString();

            if (isAllowedCustomPacket(customPacketIdentifier)) {
                return true;
            }

            if (config.debug) {
                LogDebug("Blocked custom packet " + customPacketIdentifier);
            }
        }

        if (packet instanceof ServerboundCustomClickActionPacket) {
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            return extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player);
        }

        return false;
    }

    public static boolean isAdministratorCached(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        return administratorCache.computeIfAbsent(playerUuid, ignored -> StoneCutterUtils.isAdministrator(player.server.getPlayerList(), player));
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
    public static Component checkCanPlayerJoinServer(NameAndId profile, PlayerList manager, SocketAddress socketAddress) {
        // Getting the player. By this point, the player's game profile has been authenticated so the UUID is legitimate.
        String incomingPlayerUsername = StoneCutterUtils.getName(profile);
        Player onlinePlayer = manager.getPlayerByName(incomingPlayerUsername);

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

    public static void loadPlayerData(ServerPlayer player, Connection connection) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        UUID playerUuid = player.getUUID();
        PlayerList playerManager = player.server.getPlayerList();
        administratorCache.put(playerUuid, StoneCutterUtils.isAdministrator(playerManager, player));

        // Create in case of Carpet player
        String username = StoneCutterUtils.getUsername(player);
        PlayerEntryV1 cache = PlayersCache.getOrCreate(username);
        boolean update = false;
        if (cache.uuid == null) {
            cache.uuid = player.getUUID();
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
    public static void onPlayerJoin(ServerPlayer player) {
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
            BlockPos pos = player.blockPosition();
            player.randomTeleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5, false);
            if (player.getInBlockState().getBlock().equals(Blocks.NETHER_PORTAL) || StoneCutterUtils.getServerWorld(player).getBlockState(player.blockPosition().above()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                // Faking portal blocks to be air
                ClientboundBlockUpdatePacket feetPacket = new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState());
                player.connection.send(feetPacket);

                ClientboundBlockUpdatePacket headPacket = new ClientboundBlockUpdatePacket(pos.above(), Blocks.AIR.defaultBlockState());
                player.connection.send(headPacket);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
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

    public static boolean isSkipAllAuthChecksApplicable(ServerPlayer player) {
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
    public static InteractionResult onPlayerCommand(ServerPlayer player, String command) {
        // Getting the message to then be able to check it
        if (extendedConfig.allowCommands) {
            return InteractionResult.PASS;
        }
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (command == null) {
            return InteractionResult.PASS;
        }
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return InteractionResult.PASS;
        }

        if (command.startsWith("login ")
                || command.startsWith("register ")
                || (extendedConfig.aliases.login && command.startsWith("l "))
                || (extendedConfig.aliases.register && command.startsWith("reg "))) {
            return InteractionResult.PASS;
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
                return InteractionResult.PASS;
            }
        }
        LogDebug("Player " + username + " tried to execute command " + command + " without being authenticated.");
        ((PlayerAuth) player).easyAuth$sendAuthMessage();
        return InteractionResult.FAIL;
    }

    // Player chatting
    public static InteractionResult onPlayerChat(ServerPlayer player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowChat) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // Player movement
    public static InteractionResult onPlayerMove(ServerPlayer player) {
        // Player will fall if enabled (prevent fly kick)
        // Otherwise, movement should be disabled
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowMovement) {
            UUID playerUuid = player.getUUID();
            long now = System.nanoTime();
            long lastAcceptedPacket = lastAcceptedPacketByPlayer.getOrDefault(playerUuid, 0L);
            if (now >= lastAcceptedPacket + extendedConfig.teleportationTimeoutMs * 1_000_000L) {
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                lastAcceptedPacketByPlayer.put(playerUuid, now);
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // Using a block (right-click function)
    public static InteractionResult onUseBlock(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // Breaking a block
    public static boolean onBreakBlock(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockBreaking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return false;
        }
        return true;
    }

    // Using an item
    public static InteractionResult onUseItem(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemUsing) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    // Dropping an item
    public static InteractionResult onDropItem(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemDropping) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // Changing inventory (item moving etc.)
    public static InteractionResult onTakeItem(ServerPlayer player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemMoving) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    // Attacking an entity
    public static InteractionResult onAttackEntity(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityAttacking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    // Interacting with entity
    public static InteractionResult onUseEntity(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    public static void onPreLogin(ServerLoginPacketListenerImpl netHandler) {
        if (extendedConfig.forcedOfflineUuid && netHandler.authenticatedProfile != null) {
            netHandler.authenticatedProfile = UUIDUtil.createOfflineProfile(netHandler.authenticatedProfile.name());
        }
    }

}