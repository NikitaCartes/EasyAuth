//~ resource_location
package xyz.nikitacartes.easyauth.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

/**
 * Wire twin of the server's {@code ClientModBridge} (protocol 2): {@code easyauth:hello} (S2C
 * capability + auth-state announce + one-time passkey challenge), {@code easyauth:auth} (C2S
 * credentials with a mode selector: password / session token / passkey signature / passkey
 * enrollment) and {@code easyauth:result} (S2C auth outcome + session-token issue/rotation).
 *
 * <p>Works on every supported version (loader-specific classes are used fully-qualified to avoid
 * per-era import juggling). {@code >=1.20.5}: CustomPacketPayload API (Fabric
 * serverboundPlay/clientboundPlay at 26.1+, playC2S/playS2C below; NeoForge registrar + client send
 * via ClientPacketDistributor at 1.21.9+, PacketDistributor below). {@code <1.20.5} (Fabric only —
 * NeoForge starts at 1.21): the legacy ResourceLocation+FriendlyByteBuf channel API. The wire
 * format MUST stay in sync with ClientModBridge.
 */
public final class EasyAuthPackets {

    // C2S easyauth:auth modes (mirror of ClientModBridge).
    public static final int MODE_PASSWORD = 0;
    public static final int MODE_TOKEN = 1;
    public static final int MODE_PASSKEY = 2;
    public static final int MODE_REGISTER_PASSKEY = 3;

    // S2C easyauth:result codes (mirror of ClientModBridge).
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_TOKEN_REJECTED = 1;
    public static final int RESULT_PASSKEY_REJECTED = 2;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private EasyAuthPackets() {
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

    private static void onHello(boolean canAutoLogin, boolean canAutoRegister, boolean registered,
                                boolean authenticated, boolean canSessionToken, boolean canPasskey,
                                boolean hasPasskey, byte[] challenge) {
        RuleEngine.onHello(canAutoLogin, canAutoRegister, registered, authenticated,
                canSessionToken, canPasskey, hasPasskey, challenge);
    }

    //? if fabric {
    /** Call once from the client entrypoint (payload types must register early). */
    public static void init() {
        //? if >=1.20.5 {
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ResultPayload.TYPE, ResultPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(AuthPayload.TYPE, AuthPayload.CODEC);*/
        //?}
        // Fabric play receivers run on the client main thread, same as the tick/join events.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE,
                (payload, context) -> onHello(payload.canAutoLogin(), payload.canAutoRegister(), payload.registered(),
                        payload.authenticated(), payload.canSessionToken(), payload.canPasskey(),
                        payload.hasPasskey(), payload.challenge()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ResultPayload.TYPE,
                (payload, context) -> RuleEngine.onResult(payload.code(), payload.sessionToken()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so read everything, then re-dispatch to the client thread.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(HELLO_ID,
                (client, handler, buf, sender) -> {
                    buf.readVarInt(); // protocol version (unused)
                    boolean canAutoLogin = buf.readBoolean();
                    boolean canAutoRegister = buf.readBoolean();
                    boolean registered = buf.readBoolean();
                    boolean authenticated = buf.readBoolean();
                    boolean canSessionToken = buf.readBoolean();
                    boolean canPasskey = buf.readBoolean();
                    boolean hasPasskey = buf.readBoolean();
                    byte[] challenge = buf.readByteArray();
                    client.execute(() -> onHello(canAutoLogin, canAutoRegister, registered, authenticated,
                            canSessionToken, canPasskey, hasPasskey, challenge));
                });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(RESULT_ID,
                (client, handler, buf, sender) -> {
                    int code = buf.readVarInt();
                    String sessionToken = buf.readUtf();
                    client.execute(() -> RuleEngine.onResult(code, sessionToken));
                });*/
        //?}
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); init() is unused there.
    public static void init() {
    }

    public static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        // Version "2" must match ClientModBridge's registrar (server side, PROTOCOL_VERSION) for
        // NeoForge to negotiate the channel. optional() so the companion can still connect to
        // servers without it; executesOn(MAIN) so handlers run on the client thread like Fabric's.
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar("2")
                .optional().executesOn(net.neoforged.neoforge.network.registration.HandlerThread.MAIN);
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC,
                (payload, context) -> onHello(payload.canAutoLogin(), payload.canAutoRegister(), payload.registered(),
                        payload.authenticated(), payload.canSessionToken(), payload.canPasskey(),
                        payload.hasPasskey(), payload.challenge()));
        registrar.playToClient(ResultPayload.TYPE, ResultPayload.CODEC,
                (payload, context) -> RuleEngine.onResult(payload.code(), payload.sessionToken()));
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC, (payload, context) -> {}); // send-only
    }*/
    //?}

    /** Whether the server declared the credentials channel (EasyAuth companion support). */
    public static boolean serverSupportsPacketAuth() {
        //? if fabric {
        //? if >=1.20.5 {
        return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(AuthPayload.TYPE);
        //?} else {
        /*return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(AUTH_ID);*/
        //?}
        //?} else {
        /*net.minecraft.client.multiplayer.ClientPacketListener connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(AuthPayload.TYPE);*/
        //?}
    }

    public static void sendCredentials(String password, String otp) {
        sendAuth(MODE_PASSWORD, password, otp == null ? "" : otp, "", EMPTY_BYTES, EMPTY_BYTES);
    }

    public static void sendToken(String token) {
        sendAuth(MODE_TOKEN, "", "", token, EMPTY_BYTES, EMPTY_BYTES);
    }

    public static void sendPasskey(byte[] publicKey, byte[] signature) {
        sendAuth(MODE_PASSKEY, "", "", "", publicKey, signature);
    }

    public static void sendRegisterPasskey(byte[] publicKey) {
        sendAuth(MODE_REGISTER_PASSKEY, "", "", "", publicKey, EMPTY_BYTES);
    }

    private static void sendAuth(int mode, String password, String otp, String token,
                                 byte[] publicKey, byte[] signature) {
        //? if >=1.20.5 {
        AuthPayload payload = new AuthPayload(mode, password, otp, token, publicKey, signature);
        //? if fabric {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        //?} else if >=1.21.9 {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        *///?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        *///?}
        //?} else {
        /*net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(mode);
        buf.writeUtf(password);
        buf.writeUtf(otp);
        buf.writeUtf(token);
        buf.writeByteArray(publicKey);
        buf.writeByteArray(signature);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(AUTH_ID, buf);*/
        //?}
    }
}
