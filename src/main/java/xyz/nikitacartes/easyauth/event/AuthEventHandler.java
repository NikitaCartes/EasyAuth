package xyz.nikitacartes.easyauth.event;

//? if < 1.21.9 {
/*import com.mojang.authlib.GameProfile;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
//? if >= 1.20.2 {
import net.minecraft.network.protocol.common.*;
//?}
//? if >= 1.20.5 {
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
//?} else {
/*import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
*///?}
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
//? if >= 1.21.9 {
import net.minecraft.server.players.NameAndId;
//?}
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
//? if < 1.21.2 {
/*import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
*///?}
import xyz.nikitacartes.easyauth.integrations.ClientModBridge;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.storage.database.DBReadException;
import xyz.nikitacartes.easyauth.integrations.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.proxy.ProxyBridgeProtocol;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;
import xyz.nikitacartes.easyauth.utils.PlayersCache;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;
import xyz.nikitacartes.easyauth.utils.Utils;

import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    // Admin status is a cheap in-memory op-list lookup, but it's checked on every custom packet,
    // so cache it with a short TTL. The TTL bounds how long an op/deop change stays invisible.
    private record AdminFlag(boolean isAdmin, long checkedAtNanos) {}
    private static final Map<UUID, AdminFlag> administratorCache = new ConcurrentHashMap<>();
    private static final long ADMIN_TTL_NANOS = 60_000_000_000L; // 1m

    // Housekeeping packets an unauthenticated player must always be allowed to send.
    private static final Set<Class<?>> ALWAYS_ALLOWED = new HashSet<>();

    // Inventory/slot packets, allowed together under allowItemMoving.
    private static final Set<Class<?>> ITEM_MOVING = Set.of(
            ServerboundContainerClickPacket.class,
            ServerboundSetCreativeModeSlotPacket.class,
            ServerboundSetCarriedItemPacket.class,
            ServerboundContainerClosePacket.class,
            ServerboundContainerButtonClickPacket.class);

    static {
        ALWAYS_ALLOWED.add(ServerboundKeepAlivePacket.class);
        ALWAYS_ALLOWED.add(ServerboundResourcePackPacket.class);
        ALWAYS_ALLOWED.add(ServerboundAcceptTeleportationPacket.class);
        ALWAYS_ALLOWED.add(ServerboundChatSessionUpdatePacket.class);
        ALWAYS_ALLOWED.add(ServerboundChatAckPacket.class);
        ALWAYS_ALLOWED.add(ServerboundClientCommandPacket.class);
        ALWAYS_ALLOWED.add(ServerboundCommandSuggestionPacket.class);
        ALWAYS_ALLOWED.add(ServerboundChatCommandPacket.class);
        ALWAYS_ALLOWED.add(ServerboundPingRequestPacket.class);
        ALWAYS_ALLOWED.add(ServerboundMoveVehiclePacket.class);
        ALWAYS_ALLOWED.add(ServerboundPlayerInputPacket.class);
        //? if >= 1.21.5 {
        ALWAYS_ALLOWED.add(ServerboundPlayerLoadedPacket.class);
        //?}
        //? if >= 1.21.2 {
        ALWAYS_ALLOWED.add(ServerboundClientTickEndPacket.class);
        //?}
        //? if >= 1.20.5 {
        ALWAYS_ALLOWED.add(ServerboundCookieResponsePacket.class);
        ALWAYS_ALLOWED.add(ServerboundChatCommandSignedPacket.class);
        //?}
        //? if >= 1.20.2 {
        ALWAYS_ALLOWED.add(ServerboundPongPacket.class);
        ALWAYS_ALLOWED.add(ServerboundClientInformationPacket.class);
        ALWAYS_ALLOWED.add(ServerboundChunkBatchReceivedPacket.class);
        ALWAYS_ALLOWED.add(ServerboundConfigurationAcknowledgedPacket.class);
        //?} else {
        /*ALWAYS_ALLOWED.add(ServerboundPongPacket.class);
        *///?}
    }

    public static boolean isAllowedPacket(ServerPlayer player, Packet<?> packet) {
        Class<?> packetClass = packet.getClass();

        if (ALWAYS_ALLOWED.contains(packetClass)) {
            return true;
        }

        // Movement packets are handled separately
        if (packet instanceof ServerboundMovePlayerPacket) {
            return true;
        }

        if (extendedConfig.allowChat && packetClass == ServerboundChatPacket.class) {
            return true;
        }

        if (extendedConfig.allowBlockInteraction && packetClass == ServerboundUseItemOnPacket.class) {
            return true;
        }

        if (extendedConfig.allowEntityInteraction && packetClass == ServerboundInteractPacket.class) {
            return true;
        }

        if (extendedConfig.allowItemUsing && packetClass == ServerboundUseItemPacket.class) {
            return true;
        }

        if (packetClass == ServerboundSwingPacket.class) {
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

        if (extendedConfig.allowItemMoving && ITEM_MOVING.contains(packetClass)) {
            return true;
        }

        if (packet instanceof ServerboundCustomPayloadPacket) {
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            if (extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player)) {
                return true;
            }

            //? if >= 1.20.5 {
            String customPacketIdentifier = ((ServerboundCustomPayloadPacket) packet).payload().type().id().toString();
            //?} else if >= 1.20.2 {
            /*String customPacketIdentifier = ((ServerboundCustomPayloadPacket) packet).payload().id().toString();
            *///?} else {
            /*String customPacketIdentifier = ((ServerboundCustomPayloadPacket) packet).getIdentifier().toString();
             *///?}

            // Always allow the AuthMe proxy-bridge channel for unauthenticated players: the proxy's
            // perform.login is what triggers their passwordless auto-login, so it must not be blocked.
            if (proxyConfig != null && proxyConfig.enabled
                    && ProxyBridgeProtocol.CHANNEL.equals(customPacketIdentifier)) {
                return true;
            }

            // Companion-mod credentials channel (ClientModBridge): its whole point is to
            // arrive before authentication, so it must not be blocked here.
            if ("easyauth:auth".equals(customPacketIdentifier)) {
                return true;
            }

            if (isAllowedCustomPacket(customPacketIdentifier)) {
                return true;
            }

            if (config.debug) {
                LogDebug("Blocked custom packet " + customPacketIdentifier);
            }
        }

        //? if >= 1.21.6 {
        if (packet instanceof ServerboundCustomClickActionPacket customClickActionPacket) {
            // Always allow EasyAuth's own dialog responses: the login/register windows are
            // shown to unauthenticated players, whose reply would otherwise be blocked here.
            if (customClickActionPacket.id().getNamespace().equals("easyauth")) {
                return true;
            }
            if (extendedConfig.allowCustomPackets) {
                return true;
            }

            return extendedConfig.allowCustomPacketsForNonOp && !isAdministratorCached(player);
        }
        //?}

        return false;
    }

    public static boolean isAdministratorCached(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        long now = System.nanoTime();
        AdminFlag flag = administratorCache.get(playerUuid);
        if (flag != null && now - flag.checkedAtNanos() < ADMIN_TTL_NANOS) {
            return flag.isAdmin();
        }
        boolean isAdmin = StoneCutterUtils.isAdministrator(player.server.getPlayerList(), player);
        administratorCache.put(playerUuid, new AdminFlag(isAdmin, now));
        return isAdmin;
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
     * Whether a clientbound packet about to be sent to the player should be suppressed because the
     * player is not authenticated and {@code hide-chat} is enabled.
     * System messages are left untouched.
     */
    public static boolean shouldHideClientboundChat(ServerPlayer player, Packet<?> packet) {
        if (!extendedConfig.hideChat || player == null) {
            return false;
        }
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return false;
        }
        return packet instanceof ClientboundPlayerChatPacket
                || packet instanceof ClientboundDisguisedChatPacket;
    }

    /**
     * Player pre-join.
     * Returns text as a reason for disconnect or null to pass
     *
     * @param profile NameAndId|GameProfile of the player
     * @param manager PlayerList
     * @return Component if player should be disconnected
     */
    //? if >= 1.21.9 {
    public static Component checkCanPlayerJoinServer(NameAndId profile, PlayerList manager, SocketAddress socketAddress) {
    //?} else {
    /*public static Component checkCanPlayerJoinServer(GameProfile profile, PlayerList manager, SocketAddress socketAddress) {
    *///?}
        // Runtime equivalent of the startup DB halt: no database -> don't let anyone in (fail-close).
        if (DB == null || DB.isClosed()) {
            return langConfig.error.database.getNonTranslatable();
        }

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
                return langConfig.account.alreadyOnline.getNonTranslatable(incomingPlayerUsername);
            }
        }

        // Checking if player username is valid. The pattern is generated when the config is (re)loaded.
        // Premium players bypass: their username is already validated by Mojang auth.
        PlayerEntryV1 cachedEntry = PlayersCache.get(incomingPlayerUsername);
        boolean isPremiumPlayer = manager.getServer().usesAuthentication() && cachedEntry != null && cachedEntry.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE;
        Matcher matcher = usernamePattern.matcher(incomingPlayerUsername);

        if (!(matcher.matches() || isPremiumPlayer || (extendedConfig.floodgateBypassRegex && FloodgateApiHelper.isFloodgatePlayer(StoneCutterUtils.getId(profile))))) {
            return langConfig.account.usernameInvalid.getNonTranslatable(extendedConfig.usernameRegexp);
        }
        // If the player name and registered name are different, kick the player if differentUsernameCase is enabled
        // Create in case of Floodgate player
        PlayerEntryV1 playerEntryV1;
        try {
            playerEntryV1 = PlayersCache.getOrLoadOrRegister(incomingPlayerUsername);
        } catch (DBReadException e) {
            return langConfig.error.database.getNonTranslatable();
        }

        if (!extendedConfig.allowCaseInsensitiveUsername && !playerEntryV1.username.equals(incomingPlayerUsername)) {
            return langConfig.account.usernameCaseMismatch.getNonTranslatable(incomingPlayerUsername);
        }

        if (config.maxLoginTries != -1 && playerEntryV1.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
            return langConfig.session.tooManyAttempts.getNonTranslatable();
        }

        // Check concurrent session limit per IP
        boolean isOnlinePlayer = config.premiumAutoLogin && (onlinePlayer != null) && ((PlayerAuth) onlinePlayer).easyAuth$isUsingMojangAccount();
        if (IpLimitManager.isConcurrentSessionLimitExceeded(manager.getServer(), ip, isOnlinePlayer)) {
            LogDebug("Player " + incomingPlayerUsername + " blocked: concurrent session limit exceeded for IP " + ip);
            IpLimitManager.notifyAdmins(manager.getServer(), ip, incomingPlayerUsername);
            return langConfig.error.sessionLimitExceeded.getNonTranslatable();
        }

        return null;
    }

    public static void loadPlayerData(ServerPlayer player, Connection connection) {
        PlayerAuth playerAuth = (PlayerAuth) player;

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

        // 0 = follow server default; otherwise clamp so a player can only shorten their session, never exceed the admin's policy.
        long sessionTimeout = cache.sessionTimeout == 0 ? config.sessionTimeout : Math.min(cache.sessionTimeout, config.sessionTimeout);
        if (playerAuth.easyAuth$canSkipAuth()) {
            playerAuth.easyAuth$setAuthenticated(true);

            update = false;
        } else if (Utils.sameResolvedIp(cache.lastIp, playerAuth.easyAuth$getIpAddress()) && cache.lastAuthenticatedDate.plusSeconds(sessionTimeout).isAfter(ZonedDateTime.now())) {
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

        // Companion-mod hello: announce capabilities + this player's auth state (must go out in
        // every branch below, so it sits before them).
        ClientModBridge.sendHello(player,
                playerAuth.easyAuth$canSkipAuth() || playerAuth.easyAuth$isAuthenticated()
                        || isSkipAllAuthChecksApplicable(player));

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.session.onlineAccount.send(player);
            return;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.valid.send(player);
            return;
        } else if (isSkipAllAuthChecksApplicable(player)) {
            return;
        }

        if (extendedConfig.disableRegistration && playerAuth.easyAuth$getPlayerEntryV1().password.isEmpty()) {
            player.connection.disconnect(langConfig.registration.registrationDisabled.get());
            return;
        }

        // Tries to rescue player from nether portal
        if (extendedConfig.tryPortalRescue) {
            BlockPos pos = player.blockPosition();
            player.teleportTo(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
            var world = StoneCutterUtils.getServerWorld(player);
            if (world.getBlockState(pos).getBlock().equals(Blocks.NETHER_PORTAL) || world.getBlockState(pos.above()).getBlock().equals(Blocks.NETHER_PORTAL)) {
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
        ClientModBridge.onPlayerLeave(playerUuid);

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
    //? if >= 1.21.2 {
    public static InteractionResult onUseItem(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemUsing) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }
    //?} else {
    /*public static InteractionResultHolder<ItemStack> onUseItem(Player player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemUsing) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }

        return InteractionResultHolder.pass(ItemStack.EMPTY);
    }
    *///?}

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
        //? if >= 1.20.2 {
        if (extendedConfig.forcedOfflineUuid && netHandler.authenticatedProfile != null) {
            //? if >= 1.21.9 {
            netHandler.authenticatedProfile = UUIDUtil.createOfflineProfile(netHandler.authenticatedProfile.name());
            //?} else {
            /*netHandler.authenticatedProfile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(netHandler.authenticatedProfile.getName()), netHandler.authenticatedProfile.getName());
            *///?}
        }
        //?} else {
        /*if (extendedConfig.forcedOfflineUuid && netHandler.gameProfile != null) {
            netHandler.gameProfile = netHandler.createFakeProfile(netHandler.gameProfile);
        }
        *///?}
    }

}
