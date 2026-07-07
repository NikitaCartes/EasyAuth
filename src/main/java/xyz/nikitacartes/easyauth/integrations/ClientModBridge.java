//~ resource_location
package xyz.nikitacartes.easyauth.integrations;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import xyz.nikitacartes.easyauth.commands.LoginCommand;
import xyz.nikitacartes.easyauth.commands.RegisterCommand;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogLogin;

/**
 * Packet bridge for the EasyAuth Client companion mod (see {@code CLIENT_MOD_PLAN.md} §6).
 *
 * <p>After join the server sends {@code easyauth:hello} with its capabilities and the player's
 * auth state; the companion replies on {@code easyauth:auth} with the stored credentials and the
 * server routes them to the same handlers as /login and /register (by account state, so the
 * client never has to guess between the two).
 *
 * <p>Works on every supported version. Three networking eras (loader-specific classes are used
 * fully-qualified to avoid per-era import juggling): {@code >=1.20.5} uses the CustomPacketPayload
 * API (Fabric {@code PayloadTypeRegistry.serverboundPlay/clientboundPlay} at 26.1+, the older
 * {@code playC2S/playS2C} names below; NeoForge {@code RegisterPayloadHandlersEvent}); {@code <1.20.5}
 * (Fabric only — NeoForge starts at 1.21) uses the legacy {@code ResourceLocation}+{@code FriendlyByteBuf}
 * channel API. The wire format (field order in the codecs and the legacy read/write) is identical
 * across eras and MUST stay in sync with the client's EasyAuthPackets.
 */
public final class ClientModBridge {

    private static final int PROTOCOL_VERSION = 2;
    /** Kept in sync with the whitelist in AuthEventHandler.isAllowedPacket. */
    public static final String AUTH_CHANNEL = "easyauth:auth";

    // C2S easyauth:auth modes.
    public static final int MODE_PASSWORD = 0;
    public static final int MODE_TOKEN = 1;
    public static final int MODE_PASSKEY = 2;
    public static final int MODE_REGISTER_PASSKEY = 3;

    // S2C easyauth:result codes. Rejections tell the client to fall back to the next
    // credential (passkey -> token -> password); password failures keep using chat messages.
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_TOKEN_REJECTED = 1;
    public static final int RESULT_PASSKEY_REJECTED = 2;

    /** Signed alongside the challenge for domain separation (no cross-protocol signature reuse). */
    private static final byte[] PASSKEY_DOMAIN = "easyauth-passkey-v1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Ed25519 is in every stock JRE 15+, but a stripped runtime may lack SunEC — degrade to no passkeys. */
    private static final boolean ED25519_AVAILABLE = checkEd25519();

    // One-time login challenges by player UUID, issued in sendHello and consumed by the first
    // passkey attempt. Entries are dropped on player leave; bounded by the online player count.
    private static final Map<UUID, byte[]> challenges = new ConcurrentHashMap<>();

    private static volatile boolean initialized = false;

    private ClientModBridge() {
    }

    private static Identifier id(String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath("easyauth", path);
        //?} else {
        /*return new Identifier("easyauth", path);*/
        //?}
    }

    static final Identifier HELLO_ID = id("hello");
    static final Identifier AUTH_ID = id("auth");
    static final Identifier RESULT_ID = id("result");

    //? if >=1.20.5 {
    /** S2C capability + auth-state announce, sent right after join. Challenge is empty unless a passkey login is possible. */
    public record HelloPayload(int protocolVersion, boolean canAutoLogin, boolean canAutoRegister,
                               boolean registered, boolean authenticated,
                               boolean canSessionToken, boolean canPasskey, boolean hasPasskey,
                               byte[] challenge) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> TYPE = new CustomPacketPayload.Type<>(HELLO_ID);

        public static final StreamCodec<FriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.protocolVersion());
                    buf.writeBoolean(payload.canAutoLogin());
                    buf.writeBoolean(payload.canAutoRegister());
                    buf.writeBoolean(payload.registered());
                    buf.writeBoolean(payload.authenticated());
                    buf.writeBoolean(payload.canSessionToken());
                    buf.writeBoolean(payload.canPasskey());
                    buf.writeBoolean(payload.hasPasskey());
                    buf.writeByteArray(payload.challenge());
                },
                buf -> new HelloPayload(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                        buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                        buf.readBoolean(), buf.readByteArray()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * C2S credentials; the mode selects which fields matter (the rest stay empty):
     * PASSWORD -> password/otp, TOKEN -> token, PASSKEY -> publicKey/signature,
     * REGISTER_PASSKEY (authenticated players only) -> publicKey.
     */
    public record AuthPayload(int mode, String password, String otp, String token,
                              byte[] publicKey, byte[] signature) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AuthPayload> TYPE = new CustomPacketPayload.Type<>(AUTH_ID);

        public static final StreamCodec<FriendlyByteBuf, AuthPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.mode());
                    buf.writeUtf(payload.password());
                    buf.writeUtf(payload.otp());
                    buf.writeUtf(payload.token());
                    buf.writeByteArray(payload.publicKey());
                    buf.writeByteArray(payload.signature());
                },
                buf -> new AuthPayload(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                        buf.readByteArray(), buf.readByteArray()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** S2C auth outcome; sessionToken is non-empty only on SUCCESS with tokens enabled (issue/rotation). */
    public record ResultPayload(int code, String sessionToken) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ResultPayload> TYPE = new CustomPacketPayload.Type<>(RESULT_ID);

        public static final StreamCodec<FriendlyByteBuf, ResultPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.code());
                    buf.writeUtf(payload.sessionToken());
                },
                buf -> new ResultPayload(buf.readVarInt(), buf.readUtf()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    //?}

    //? if fabric {
    /**
     * Registers the channels + receiver. Dedicated servers only: on a client instance the
     * companion mod owns these payload ids, and registering them twice would crash.
     * ponytail: LAN hosts therefore have no packet auth — the command fallback covers them.
     */
    public static void init() {
        if (initialized || net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.SERVER) {
            return;
        }
        //? if >=1.20.5 {
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(AuthPayload.TYPE, AuthPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ResultPayload.TYPE, ResultPayload.CODEC);*/
        //?}
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE,
                (payload, context) -> receive(context.player(), payload.mode(), payload.password(), payload.otp(),
                        payload.token(), payload.publicKey(), payload.signature()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so read everything, then re-dispatch to the server thread.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(AUTH_ID,
                (server, player, handler, buf, sender) -> {
                    int mode = buf.readVarInt();
                    String password = buf.readUtf();
                    String otp = buf.readUtf();
                    String token = buf.readUtf();
                    byte[] publicKey = buf.readByteArray();
                    byte[] signature = buf.readByteArray();
                    server.execute(() -> receive(player, mode, password, otp, token, publicKey, signature));
                });*/
        //?}
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (easyauth:hello / " + AUTH_CHANNEL + ")");
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); nothing to do at mod init.
    public static void init() {
    }

    public static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        //? if >=1.21.9 {
        /^if (initialized || net.neoforged.fml.loading.FMLEnvironment.getDist() != net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            return;
        }
        ^///?} else {
        if (initialized || net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            return;
        }
        //?}
        // optional() so vanilla / non-companion clients are not rejected for lacking the channel.
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar(String.valueOf(PROTOCOL_VERSION))
                .optional().executesOn(net.neoforged.neoforge.network.registration.HandlerThread.MAIN);
        // 3-arg (no-op handler) works on every NeoForge target; the 2-arg send-only overload is 1.21.9+.
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC, (payload, context) -> {});
        registrar.playToClient(ResultPayload.TYPE, ResultPayload.CODEC, (payload, context) -> {});
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC,
                (payload, context) -> receive((ServerPlayer) context.player(), payload.mode(), payload.password(),
                        payload.otp(), payload.token(), payload.publicKey(), payload.signature()));
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (easyauth:hello / " + AUTH_CHANNEL + ")");
    }*/
    //?}

    /** Whether this player's client declared the companion channels (checked via the hello channel). */
    private static boolean hasCompanion(ServerPlayer player) {
        //? if fabric {
        //? if >=1.20.5 {
        return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, HelloPayload.TYPE);
        //?} else {
        /*return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, HELLO_ID);*/
        //?}
        //?} else {
        /*return player.connection.hasChannel(HelloPayload.TYPE);*/
        //?}
    }

    /** No-op for clients without the companion mod (they never declared the channel). */
    public static void sendHello(ServerPlayer player, boolean authenticated) {
        if (!initialized || !hasCompanion(player)) {
            return;
        }
        PlayerEntryV1 entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
        boolean registered = entry != null && !entry.password.isEmpty();
        boolean canAutoLogin = extendedConfig.clientMod.allowAutoLogin;
        boolean canAutoRegister = extendedConfig.clientMod.allowAutoRegister
                && !extendedConfig.disableRegistration
                && !config.enableGlobalPassword;
        boolean canSessionToken = extendedConfig.clientMod.allowSessionToken;
        boolean canPasskey = extendedConfig.clientMod.allowPasskey && ED25519_AVAILABLE;
        boolean hasPasskey = entry != null && entry.hasPasskeys();
        byte[] challenge = EMPTY_BYTES;
        if (canPasskey && hasPasskey && !authenticated) {
            challenge = new byte[32];
            RANDOM.nextBytes(challenge);
            challenges.put(player.getUUID(), challenge);
        }
        //? if >=1.20.5 {
        HelloPayload hello = new HelloPayload(PROTOCOL_VERSION, canAutoLogin, canAutoRegister, registered,
                authenticated, canSessionToken, canPasskey, hasPasskey, challenge);
        //? if fabric {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, hello);
        //?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, hello);*/
        //?}
        //?} else {
        /*FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(PROTOCOL_VERSION);
        buf.writeBoolean(canAutoLogin);
        buf.writeBoolean(canAutoRegister);
        buf.writeBoolean(registered);
        buf.writeBoolean(authenticated);
        buf.writeBoolean(canSessionToken);
        buf.writeBoolean(canPasskey);
        buf.writeBoolean(hasPasskey);
        buf.writeByteArray(challenge);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, HELLO_ID, buf);*/
        //?}
    }

    private static void sendResult(ServerPlayer player, int code, String sessionToken) {
        if (!initialized || !hasCompanion(player)) {
            return;
        }
        //? if >=1.20.5 {
        ResultPayload result = new ResultPayload(code, sessionToken);
        //? if fabric {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, result);
        //?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, result);*/
        //?}
        //?} else {
        /*FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(code);
        buf.writeUtf(sessionToken);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, RESULT_ID, buf);*/
        //?}
    }

    /**
     * Called on the server thread after every successful explicit authentication (/login command,
     * dialog, or any packet mode) and after /register. Issues a fresh session token (rotating the
     * previous one) for companion clients and confirms the login so the client can enroll a passkey.
     */
    public static void onAuthSuccess(ServerPlayer player) {
        if (!initialized || !hasCompanion(player)) {
            return;
        }
        PlayerEntryV1 entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
        if (entry == null) {
            return;
        }
        String token = "";
        if (extendedConfig.clientMod.allowSessionToken) {
            token = entry.issueSessionToken(extendedConfig.clientMod.sessionTokenTtl);
            entry.update();
        }
        sendResult(player, RESULT_SUCCESS, token);
    }

    /** Drops the pending login challenge (if any) when the player disconnects. */
    public static void onPlayerLeave(UUID playerUuid) {
        challenges.remove(playerUuid);
    }

    // Runs on the server thread (fabric payload receivers are re-dispatched there; the legacy
    // handler re-dispatches via server.execute; NeoForge via executesOn(MAIN)).
    private static void receive(ServerPlayer player, int mode, String password, String otp,
                                String token, byte[] publicKey, byte[] signature) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (entry == null) {
            return;
        }
        String username = StoneCutterUtils.getUsername(player);
        if (playerAuth.easyAuth$isAuthenticated()) {
            // The only packet accepted after login is the passkey enrollment.
            if (mode == MODE_REGISTER_PASSKEY) {
                registerPasskey(entry, publicKey, username);
            }
            return;
        }
        switch (mode) {
            case MODE_PASSWORD -> receivePassword(player, entry, password, otp, username);
            case MODE_TOKEN -> receiveToken(player, playerAuth, entry, token, username);
            case MODE_PASSKEY -> receivePasskey(player, playerAuth, entry, publicKey, signature, username);
            default -> LogDebug("Ignoring companion packet mode " + mode + " from unauthenticated " + username);
        }
    }

    private static void receivePassword(ServerPlayer player, PlayerEntryV1 entry, String password, String otp, String username) {
        try {
            if (entry.password.isEmpty()) {
                // Register path re-checks disable-registration etc. inside RegisterCommand.
                if (!extendedConfig.clientMod.allowAutoRegister || config.enableGlobalPassword) {
                    LogDebug("Ignoring packet registration from " + username + " (disabled in extended.conf)");
                    return;
                }
                RegisterCommand.register(player.createCommandSourceStack(), password, password);
            } else {
                if (!extendedConfig.clientMod.allowAutoLogin) {
                    LogDebug("Ignoring packet login from " + username + " (disabled in extended.conf)");
                    return;
                }
                LoginCommand.login(player.createCommandSourceStack(), password, otp.isEmpty() ? null : otp);
            }
        } catch (CommandSyntaxException e) {
            // getPlayerOrException cannot fail for a player-created source
        }
    }

    private static void receiveToken(ServerPlayer player, PlayerAuth playerAuth, PlayerEntryV1 entry,
                                     String token, String username) {
        String ip = playerAuth.easyAuth$getIpAddress();
        if (!extendedConfig.clientMod.allowSessionToken || IpLimitManager.isLoginRateLimitExceeded(ip)
                || !entry.verifySessionToken(token)) {
            // Stale token is normal (rotated by another device / expired) — tell the client to fall back.
            LogLogin("Player " + username + " presented a rejected session token");
            sendResult(player, RESULT_TOKEN_REJECTED, "");
            return;
        }
        LogLogin("Player " + username + " logged in with a session token");
        completeAuth(player, playerAuth, entry, ip);
    }

    private static void receivePasskey(ServerPlayer player, PlayerAuth playerAuth, PlayerEntryV1 entry,
                                       byte[] publicKey, byte[] signature, String username) {
        String ip = playerAuth.easyAuth$getIpAddress();
        // The challenge is one-time: consumed by the first attempt, reissued only on rejoin.
        byte[] challenge = challenges.remove(player.getUUID());
        boolean valid = extendedConfig.clientMod.allowPasskey
                && !IpLimitManager.isLoginRateLimitExceeded(ip)
                && challenge != null
                && !entry.password.isEmpty()
                && entry.passkeys != null
                && entry.passkeys.contains(Base64.getEncoder().encodeToString(publicKey))
                && verifyPasskeySignature(publicKey, signature, challenge);
        if (!valid) {
            LogLogin("Player " + username + " presented a rejected passkey");
            sendResult(player, RESULT_PASSKEY_REJECTED, "");
            return;
        }
        LogLogin("Player " + username + " logged in with a passkey");
        completeAuth(player, playerAuth, entry, ip);
    }

    private static void registerPasskey(PlayerEntryV1 entry, byte[] publicKey, String username) {
        if (!extendedConfig.clientMod.allowPasskey || !ED25519_AVAILABLE || entry.password.isEmpty()) {
            return;
        }
        if (publicKey.length == 0 || publicKey.length > 128 || !isValidEd25519Key(publicKey)) {
            LogDebug("Rejecting malformed passkey from " + username);
            return;
        }
        entry.addPasskey(Base64.getEncoder().encodeToString(publicKey));
        entry.update();
        LogInfo("Player " + username + " registered a passkey");
    }

    // Mirror of LoginCommand.applyLoginResult's success branch for the credential-less packet
    // modes (token/passkey). The join-time kick-window check already gates these players.
    private static void completeAuth(ServerPlayer player, PlayerAuth playerAuth, PlayerEntryV1 entry, String ip) {
        langConfig.session.loginSuccess.send(player);
        playerAuth.easyAuth$restoreTrueLocation();
        playerAuth.easyAuth$setAuthenticated(true);
        entry.lastAuthenticatedDate = ZonedDateTime.now();
        entry.loginTries = 0;
        String oldIp = entry.lastIp;
        entry.lastIp = ip;
        entry.update();
        IpLimitManager.clearLoginAttempts(ip);
        if (!oldIp.equals(entry.lastIp)) {
            IpLimitManager.invalidateCache(oldIp);
            IpLimitManager.invalidateCache(entry.lastIp);
        }
        onAuthSuccess(player);
    }

    private static boolean verifyPasskeySignature(byte[] publicKey, byte[] signature, byte[] challenge) {
        try {
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKey));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(PASSKEY_DOMAIN);
            verifier.update(challenge);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | RuntimeException e) {
            LogDebug("Passkey signature verification failed: " + e);
            return false;
        }
    }

    private static boolean isValidEd25519Key(byte[] publicKey) {
        try {
            KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKey));
            return true;
        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        }
    }

    private static boolean checkEd25519() {
        try {
            KeyFactory.getInstance("Ed25519");
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
