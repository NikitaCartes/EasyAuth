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
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
*///?}
//? if <1.20.2 {
/*import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
*///?}
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
*///?}
import xyz.nikitacartes.easyauth.commands.LoginCommand;
import xyz.nikitacartes.easyauth.commands.RegisterCommand;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.protocol.ClientModProtocol;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.IpLimitManager;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogLogin;

/**
 * Packet bridge for the EasyAuth Client companion mod.
 *
 * <p>After join the server sends {@code easyauth:hello} with its capabilities and the player's
 * auth state; the companion replies on {@code easyauth:auth} with the stored credentials and the
 * server routes them to the same handlers as /login and /register (by account state, so the
 * client never has to guess between the two).
 *
 * <p>Loader-specific classes are used fully-qualified to avoid per-era import juggling:
 * {@code >=1.20.5} uses the CustomPacketPayload API, {@code <1.20.5} (Fabric only — NeoForge
 * starts at 1.21) the legacy {@code ResourceLocation}+{@code FriendlyByteBuf} channel API.
 * The wire format itself (field order, constants) lives in the shared
 * {@link ClientModProtocol}, compiled into both mods, so the two sides cannot drift.
 */
public final class ClientModBridge {

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Ed25519 is in every stock JRE 15+, but a stripped runtime may lack SunEC — degrade to no passkeys. */
    private static final boolean ED25519_AVAILABLE = checkEd25519();

    // One-time login challenges by player UUID, issued in sendHello and consumed by the first
    // passkey attempt. Entries are dropped on player leave; bounded by the online player count.
    private static final Map<UUID, byte[]> challenges = new ConcurrentHashMap<>();

    // Players whose join-time hello could not go out because their companion channels were not
    // declared yet: pre-configuration-phase clients (<1.20.2) send minecraft:register only after
    // entering play. The channel-register hook in init() retries from here; cleaned up on leave.
    private static final Set<UUID> helloPending = ConcurrentHashMap.newKeySet();

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
    // Thin payload wrappers for the CustomPacketPayload API; the wire format is ClientModProtocol's.
    public record HelloPayload(ClientModProtocol.Hello hello) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> TYPE = new CustomPacketPayload.Type<>(HELLO_ID);

        public static final StreamCodec<FriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
                (buf, payload) -> payload.hello().write(buf),
                buf -> new HelloPayload(ClientModProtocol.Hello.read(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AuthPayload(ClientModProtocol.Auth auth) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AuthPayload> TYPE = new CustomPacketPayload.Type<>(AUTH_ID);

        public static final StreamCodec<FriendlyByteBuf, AuthPayload> CODEC = StreamCodec.of(
                (buf, payload) -> payload.auth().write(buf),
                buf -> new AuthPayload(ClientModProtocol.Auth.read(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResultPayload(ClientModProtocol.Result result) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ResultPayload> TYPE = new CustomPacketPayload.Type<>(RESULT_ID);

        public static final StreamCodec<FriendlyByteBuf, ResultPayload> CODEC = StreamCodec.of(
                (buf, payload) -> payload.result().write(buf),
                buf -> new ResultPayload(ClientModProtocol.Result.read(buf)));

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
        if (initialized || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }
        //? if >=1.20.5 {
        //? if >=26.1 {
        PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(AuthPayload.TYPE, AuthPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResultPayload.TYPE, ResultPayload.CODEC);*/
        //?}
        ServerPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE,
                (payload, context) -> receive(context.player(), payload.auth()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so read everything, then re-dispatch to the server thread.
        ServerPlayNetworking.registerGlobalReceiver(AUTH_ID,
                (server, player, handler, buf, sender) -> {
                    ClientModProtocol.Auth auth = ClientModProtocol.Auth.read(buf);
                    server.execute(() -> receive(player, auth));
                });*/
        //?}
        //? if <1.20.2 {
        /*// Without the configuration phase the client declares its channels only after
        // placeNewPlayer — after onPlayerJoin already ran — so the join-time hello found no
        // companion. Resend it when the late minecraft:register arrives, or auto-login never
        // starts on these versions (the client suppresses its command fallback while waiting).
        // S2CPlayChannelEvents = server-side event for the client's receivable-channel announce.
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(HELLO_ID)) {
                server.execute(() -> {
                    ServerPlayer player = handler.player;
                    if (helloPending.remove(player.getUUID())) {
                        sendHello(player, AuthEventHandler.isEffectivelyAuthenticated(player));
                    }
                });
            }
        });*/
        //?}
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (" + ClientModProtocol.HELLO_CHANNEL + " / " + ClientModProtocol.AUTH_CHANNEL + ")");
    }
    //?} else {
    /*public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        //? if >=1.21.9 {
        /^if (initialized || FMLEnvironment.getDist() != Dist.DEDICATED_SERVER) {
            return;
        }
        ^///?} else {
        if (initialized || FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            return;
        }
        //?}
        // optional() so vanilla / non-companion clients are not rejected for lacking the channel.
        PayloadRegistrar registrar = event.registrar(String.valueOf(ClientModProtocol.PROTOCOL_VERSION))
                .optional().executesOn(HandlerThread.MAIN);
        // 3-arg (no-op handler) works on every NeoForge target; the 2-arg send-only overload is 1.21.9+.
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC, (payload, context) -> {});
        registrar.playToClient(ResultPayload.TYPE, ResultPayload.CODEC, (payload, context) -> {});
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC,
                (payload, context) -> receive((ServerPlayer) context.player(), payload.auth()));
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (" + ClientModProtocol.HELLO_CHANNEL + " / " + ClientModProtocol.AUTH_CHANNEL + ")");
    }*/
    //?}

    /**
     * Whether the pre-auth packet firewall should let the credentials channel through: the bridge
     * is registered and at least one packet mode that unauthenticated players use is enabled
     * (passkey enrollment happens after login, so it needs no pre-auth allowance).
     */
    public static boolean acceptsPreAuthPackets() {
        return initialized && (extendedConfig.clientMod.allowAutoLogin
                || extendedConfig.clientMod.allowAutoRegister
                || extendedConfig.clientMod.allowSessionToken
                || extendedConfig.clientMod.allowPasskey);
    }

    /** Whether this player's client declared the companion channels (checked via the hello channel). */
    private static boolean hasCompanion(ServerPlayer player) {
        //? if fabric {
        //? if >=1.20.5 {
        return ServerPlayNetworking.canSend(player, HelloPayload.TYPE);
        //?} else {
        /*return ServerPlayNetworking.canSend(player, HELLO_ID);*/
        //?}
        //?} else {
        /*return player.connection.hasChannel(HelloPayload.TYPE);*/
        //?}
    }

    /** No-op for clients without the companion mod (they never declared the channel). */
    public static void sendHello(ServerPlayer player, boolean authenticated) {
        if (!initialized) {
            return;
        }
        if (!hasCompanion(player)) {
            helloPending.add(player.getUUID());
            return;
        }
        helloPending.remove(player.getUUID());
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
        ClientModProtocol.Hello hello = new ClientModProtocol.Hello(ClientModProtocol.PROTOCOL_VERSION,
                canAutoLogin, canAutoRegister, registered, authenticated, canSessionToken, canPasskey,
                hasPasskey, challenge);
        //? if >=1.20.5 {
        //? if fabric {
        ServerPlayNetworking.send(player, new HelloPayload(hello));
        //?} else {
        /*PacketDistributor.sendToPlayer(player, new HelloPayload(hello));*/
        //?}
        //?} else {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        hello.write(buf);
        ServerPlayNetworking.send(player, HELLO_ID, buf);*/
        //?}
    }

    private static void sendResult(ServerPlayer player, int code, String sessionToken) {
        if (!initialized || !hasCompanion(player)) {
            return;
        }
        ClientModProtocol.Result result = new ClientModProtocol.Result(code, sessionToken);
        //? if >=1.20.5 {
        //? if fabric {
        ServerPlayNetworking.send(player, new ResultPayload(result));
        //?} else {
        /*PacketDistributor.sendToPlayer(player, new ResultPayload(result));*/
        //?}
        //?} else {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        result.write(buf);
        ServerPlayNetworking.send(player, RESULT_ID, buf);*/
        //?}
    }

    /**
     * Called on the server thread after every successful explicit authentication (/login command,
     * dialog, or any packet mode) and after /register. Issues a fresh session token (rotating the
     * previous one) for companion clients and confirms the login so the client can enroll a passkey.
     * Only mutates the entry — the caller persists it, so each login stays a single DB write
     * (see LoginCommand.finishLogin and the post-write confirmation in RegisterCommand).
     *
     * @return true when a token was issued, i.e. the entry was mutated and needs persisting
     */
    public static boolean onAuthSuccess(ServerPlayer player) {
        if (!initialized || !hasCompanion(player)) {
            return false;
        }
        PlayerEntryV1 entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
        if (entry == null) {
            return false;
        }
        String token = "";
        if (extendedConfig.clientMod.allowSessionToken) {
            token = entry.issueSessionToken(extendedConfig.clientMod.sessionTokenTtl);
        }
        sendResult(player, ClientModProtocol.RESULT_SUCCESS, token);
        return !token.isEmpty();
    }

    /** Drops the pending login challenge and hello state (if any) when the player disconnects. */
    public static void onPlayerLeave(UUID playerUuid) {
        challenges.remove(playerUuid);
        helloPending.remove(playerUuid);
    }

    // Runs on the server thread (fabric payload receivers are re-dispatched there; the legacy
    // handler re-dispatches via server.execute; NeoForge via executesOn(MAIN)).
    private static void receive(ServerPlayer player, ClientModProtocol.Auth auth) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (entry == null) {
            return;
        }
        String username = StoneCutterUtils.getUsername(player);
        if (playerAuth.easyAuth$isAuthenticated()) {
            // The only packet accepted after login is the passkey enrollment.
            if (auth.mode() == ClientModProtocol.MODE_REGISTER_PASSKEY) {
                registerPasskey(entry, auth.publicKey(), username);
            }
            return;
        }
        switch (auth.mode()) {
            case ClientModProtocol.MODE_PASSWORD -> receivePassword(player, entry, auth.password(), auth.otp(), username);
            case ClientModProtocol.MODE_TOKEN -> receiveToken(player, playerAuth, entry, auth.token(), username);
            case ClientModProtocol.MODE_PASSKEY -> receivePasskey(player, playerAuth, entry, auth.publicKey(), auth.signature(), username);
            default -> LogDebug("Ignoring companion packet mode " + auth.mode() + " from unauthenticated " + username);
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
        if (!extendedConfig.clientMod.allowSessionToken || IpLimitManager.isLoginRateLimitExceeded(ip)) {
            // Mode unavailable / rate limited — no failed-attempt penalty, same as the command path.
            LogLogin("Player " + username + " sent a session token while token login is unavailable");
            sendResult(player, ClientModProtocol.RESULT_TOKEN_REJECTED, "");
            return;
        }
        if (!entry.verifySessionToken(token)) {
            // Stale token is normal (rotated by another device / expired) — tell the client to fall
            // back. Result goes out first so the client drops the token even if the failed attempt
            // (counted like a wrong password) takes the player over maxLoginTries and kicks.
            LogLogin("Player " + username + " presented a rejected session token");
            sendResult(player, ClientModProtocol.RESULT_TOKEN_REJECTED, "");
            LoginCommand.recordFailedAttempt(player, entry);
            return;
        }
        LogLogin("Player " + username + " logged in with a session token");
        LoginCommand.finishLogin(player, playerAuth, entry, ip);
    }

    private static void receivePasskey(ServerPlayer player, PlayerAuth playerAuth, PlayerEntryV1 entry,
                                       byte[] publicKey, byte[] signature, String username) {
        String ip = playerAuth.easyAuth$getIpAddress();
        // The challenge is one-time: consumed by the first attempt, reissued only on rejoin.
        byte[] challenge = challenges.remove(player.getUUID());
        if (!extendedConfig.clientMod.allowPasskey || IpLimitManager.isLoginRateLimitExceeded(ip)) {
            // Mode unavailable / rate limited — no failed-attempt penalty, same as the command path.
            LogLogin("Player " + username + " sent a passkey while passkey login is unavailable");
            sendResult(player, ClientModProtocol.RESULT_PASSKEY_REJECTED, "");
            return;
        }
        boolean valid = challenge != null
                && !entry.password.isEmpty()
                && entry.passkeys != null
                && entry.passkeys.contains(Base64.getEncoder().encodeToString(publicKey))
                && verifyPasskeySignature(publicKey, signature, challenge);
        if (!valid) {
            // Result goes out first so the client prunes the dead key even if the failed attempt
            // (counted like a wrong password) takes the player over maxLoginTries and kicks.
            LogLogin("Player " + username + " presented a rejected passkey");
            sendResult(player, ClientModProtocol.RESULT_PASSKEY_REJECTED, "");
            LoginCommand.recordFailedAttempt(player, entry);
            return;
        }
        LogLogin("Player " + username + " logged in with a passkey");
        LoginCommand.finishLogin(player, playerAuth, entry, ip);
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

    private static boolean verifyPasskeySignature(byte[] publicKey, byte[] signature, byte[] challenge) {
        try {
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(publicKey));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(ClientModProtocol.PASSKEY_DOMAIN);
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
