//~ resource_location
package xyz.nikitacartes.easyauth.proxy;
//? if fabric {

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >= 1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
*///?}
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.config.ProxyConfigV1;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

/**
 * EasyAuth side of the (unofficial) AuthMeReloaded proxy-bridge on channel {@code authme:main}.
 *
 * <p>Lets a premium player auto-login without a password when an AuthMe Velocity plugin has
 * cryptographically verified them on the proxy. The backend
 * runs {@code online-mode=false}; trust comes entirely from the HMAC-signed {@code perform.login}.
 *
 * <p>Receive is via Fabric {@link ServerPlayNetworking}, so messages only arrive in play phase with a
 * live {@link ServerPlayer} (after {@code placeNewPlayer} → after EasyAuth's join handling). That
 * leaves no join/perform.login race, so no queue is needed: the carrier of a
 * {@code perform.login} is exactly the player to log in. Send is a raw clientbound payload on the
 * player's connection (Velocity intercepts {@code authme:main} and never forwards it to the client).
 *
 * <p>Fabric only (NeoForge has no comparable AuthMe proxy mod). On MC &lt; 1.20.5 the legacy raw-buffer
 * networking API is used; from 1.20.5 the modern {@code CustomPacketPayload} API is used.
 */
public final class ProxyBridge {

    private static volatile boolean initialized = false;

    /** Names enrolled via {@code /premium} but not yet confirmed by a proxy verification (transient). */
    private static final Set<String> pendingPremium = ConcurrentHashMap.newKeySet();

    private static final Identifier CHANNEL_ID = channelId();

    private ProxyBridge() {
    }

    private static Identifier channelId() {
        //? if >= 1.21 {
        return Identifier.fromNamespaceAndPath(ProxyBridgeProtocol.CHANNEL_NAMESPACE, ProxyBridgeProtocol.CHANNEL_PATH);
        //?} else {
        /*return new Identifier(ProxyBridgeProtocol.CHANNEL_NAMESPACE, ProxyBridgeProtocol.CHANNEL_PATH);
        *///?}
    }

    //? if >= 1.20.5 {
    /** Raw AuthMe message: the bytes after the channel id (a {@code DataOutput} stream). */
    public record AuthMePayload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AuthMePayload> TYPE =
                new CustomPacketPayload.Type<AuthMePayload>(CHANNEL_ID);

        public static final StreamCodec<FriendlyByteBuf, AuthMePayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()),
                buf -> {
                    byte[] b = new byte[buf.readableBytes()];
                    buf.readBytes(b);
                    return new AuthMePayload(b);
                });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    //?}

    /** Registers the channel + receiver. Call once at mod init (payload types must register early). */
    public static void init() {
        if (initialized) {
            return;
        }
        //? if >= 1.20.5 {
        // fabric-networking-api-v1 6.x (MC 26.1+) renamed the play accessors.
        //? if >= 26.1 {
        PayloadTypeRegistry.serverboundPlay().register(AuthMePayload.TYPE, AuthMePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AuthMePayload.TYPE, AuthMePayload.CODEC);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(AuthMePayload.TYPE, AuthMePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AuthMePayload.TYPE, AuthMePayload.CODEC);
        *///?}
        ServerPlayNetworking.registerGlobalReceiver(AuthMePayload.TYPE, (payload, context) -> receive(context.player(), payload.data()));
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(CHANNEL_ID, (server, player, handler, buf, responseSender) -> {
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);
            server.execute(() -> receive(player, b));
        });
        *///?}
        initialized = true;
        LogInfo("AuthMe proxy-bridge channel registered (" + ProxyBridgeProtocol.CHANNEL + ")");
    }

    public static boolean isEnabled() {
        ProxyConfigV1 cfg = settings();
        return cfg != null && cfg.enabled && cfg.proxySharedSecret != null && !cfg.proxySharedSecret.isEmpty();
    }

    // --- Incoming (proxy -> backend) ---

    private static void receive(ServerPlayer player, byte[] data) {
        if (!isEnabled() || player == null) {
            return;
        }
        String type = ProxyBridgeProtocol.readType(data);
        if (type == null) {
            return;
        }
        switch (type) {
            case ProxyBridgeProtocol.TYPE_PROXY_STARTED -> onProxyStarted(player, data);
            case ProxyBridgeProtocol.TYPE_PERFORM_LOGIN -> onPerformLogin(player, data);
            default -> { /* types the backend does not consume */ }
        }
    }

    private static void onPerformLogin(ServerPlayer player, byte[] data) {
        String secret = settings().proxySharedSecret;
        ProxyBridgeProtocol.PerformLogin login =
                ProxyBridgeProtocol.parseAndVerifyPerformLogin(data, secret, System.currentTimeMillis());
        String playerName = StoneCutterUtils.getUsername(player);
        if (login == null) {
            LogDebug("Rejected proxy perform.login for " + playerName + " (bad HMAC, expired, or malformed)");
            return;
        }
        // The proxy routes perform.login through the target player's own connection; names must match.
        if (!login.name().equals(playerName.toLowerCase(Locale.ROOT))) {
            LogWarn("Ignoring proxy perform.login: name mismatch (message=" + login.name() + ", connection=" + playerName + ")");
            return;
        }

        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();

        // A non-null verified UUID means the proxy actually completed Mojang verification this session:
        // promote a pending enrollment to a confirmed premium account.
        if (login.verifiedPremiumUuid() != null && entry != null
                && entry.onlineAccount != PlayerEntryV1.OnlineAccount.TRUE) {
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
            entry.update();
            pendingPremium.remove(login.name());
            sendTo(player, ProxyBridgeProtocol.premiumSet(playerName));
            LogInfo("Confirmed premium account for " + playerName + " (verified by proxy)");
        }

        if (!playerAuth.easyAuth$isAuthenticated()) {
            playerAuth.easyAuth$restoreTrueLocation();
            playerAuth.easyAuth$setAuthenticated(true);
            if (entry != null) {
                entry.lastAuthenticatedDate = ZonedDateTime.now();
                entry.lastIp = playerAuth.easyAuth$getIpAddress();
                entry.loginTries = 0;
                entry.update();
            }
            langConfig.session.loginSuccess.send(player);
            LogLogin("Player " + playerName + " auto-logged in via AuthMe proxy-bridge");
        }

        // Always ACK so the proxy stops retrying, even if the player was already authenticated.
        sendTo(player, ProxyBridgeProtocol.performLoginAck(playerName));
    }

    private static void onProxyStarted(ServerPlayer carrier, byte[] data) {
        String proxyName = ProxyBridgeProtocol.readProxyStartedArgument(data);
        if (proxyName == null) {
            return;
        }
        LogInfo("Proxy '" + proxyName + "' started; sending premium list");
        MinecraftServer server = carrier.server;
        Set<String> pendingSnapshot = new HashSet<>(pendingPremium);
        THREADPOOL.execute(() -> {
            List<String> premium = collectConfirmedPremium();
            server.execute(() -> {
                ServerPlayer freshCarrier = pickCarrier(server, carrier);
                if (freshCarrier == null) {
                    LogWarn("Cannot send premium list to proxy '" + proxyName + "': no online carrier player");
                    return;
                }
                sendPremiumList(freshCarrier, premium);
                for (String name : pendingSnapshot) {
                    sendTo(freshCarrier, ProxyBridgeProtocol.premiumPendingSet(name));
                }
                LogInfo("Sent premium list (" + premium.size() + " confirmed, " + pendingSnapshot.size()
                        + " pending) to proxy '" + proxyName + "'");
            });
        });
    }

    // --- Command-driven (backend -> proxy) ---

    /**
     * Enroll a player into pending premium: the proxy will force Mojang verification on their next
     * connect, and a successful {@code perform.login} will then confirm them. Run on the server thread.
     */
    public static void enrollPending(ServerPlayer player) {
        String name = StoneCutterUtils.getUsername(player).toLowerCase(Locale.ROOT);
        pendingPremium.add(name);
        sendTo(player, ProxyBridgeProtocol.premiumPendingSet(name));
        LogInfo("Enrolled " + name + " for pending premium verification");
    }

    /** Remove a player from premium (pending and confirmed) and tell the proxy. Run on the server thread. */
    public static void unenroll(ServerPlayer player) {
        String name = StoneCutterUtils.getUsername(player).toLowerCase(Locale.ROOT);
        pendingPremium.remove(name);
        sendTo(player, ProxyBridgeProtocol.premiumUnset(name));
        LogInfo("Removed premium for " + name);
    }

    // --- Helpers ---

    private static List<String> collectConfirmedPremium() {
        if (DB == null || DB.isClosed()) {
            return new ArrayList<>();
        }
        return DB.getPremiumUsernames();
    }

    private static void sendPremiumList(ServerPlayer carrier, List<String> names) {
        int chunkSize = 1000;
        int total = names.size();
        if (total == 0) {
            sendTo(carrier, ProxyBridgeProtocol.premiumListChunk(0, true, ""));
            return;
        }
        int numChunks = (total + chunkSize - 1) / chunkSize;
        for (int i = 0; i < numChunks; i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, total);
            String csv = String.join(",", names.subList(from, to));
            sendTo(carrier, ProxyBridgeProtocol.premiumListChunk(i, i == numChunks - 1, csv));
        }
    }

    private static ServerPlayer pickCarrier(MinecraftServer server, ServerPlayer preferred) {
        if (preferred != null && preferred.connection != null) {
            return preferred;
        }
        return server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
    }

    private static void sendTo(ServerPlayer player, byte[] data) {
        //? if >= 1.20.5 {
        player.connection.send(new ClientboundCustomPayloadPacket(new AuthMePayload(data)));
        //?} else {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBytes(data);
        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
        *///?}
    }

    private static ProxyConfigV1 settings() {
        return EasyAuth.proxyConfig;
    }
}
//?}
