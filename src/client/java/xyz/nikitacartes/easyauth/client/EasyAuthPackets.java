//~ resource_location
package xyz.nikitacartes.easyauth.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.protocol.ClientModProtocol;

/**
 * Client transport for the companion protocol: {@code easyauth:hello} (S2C capability +
 * auth-state announce + one-time passkey challenge), {@code easyauth:auth} (C2S credentials with
 * a mode selector: password / session token / passkey signature / passkey enrollment) and
 * {@code easyauth:result} (S2C auth outcome + session-token issue/rotation). The wire format
 * itself lives in the shared {@link ClientModProtocol}, compiled into both mods, so this side
 * cannot drift from the server's ClientModBridge.
 *
 * <p>Works on every supported version (loader-specific classes are used fully-qualified to avoid
 * per-era import juggling). {@code >=1.20.5}: CustomPacketPayload API (Fabric
 * serverboundPlay/clientboundPlay at 26.1+, playC2S/playS2C below; NeoForge registrar + client send
 * via ClientPacketDistributor at 1.21.9+, PacketDistributor below). {@code <1.20.5} (Fabric only —
 * NeoForge starts at 1.21): the legacy ResourceLocation+FriendlyByteBuf channel API.
 */
public final class EasyAuthPackets {

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

    private static void onHello(ClientModProtocol.Hello hello) {
        RuleEngine.onHello(hello.canAutoLogin(), hello.canAutoRegister(), hello.registered(),
                hello.authenticated(), hello.canSessionToken(), hello.canPasskey(),
                hello.hasPasskey(), hello.challenge());
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
                (payload, context) -> onHello(payload.hello()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ResultPayload.TYPE,
                (payload, context) -> RuleEngine.onResult(payload.result().code(), payload.result().sessionToken()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so read everything, then re-dispatch to the client thread.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(HELLO_ID,
                (client, handler, buf, sender) -> {
                    ClientModProtocol.Hello hello = ClientModProtocol.Hello.read(buf);
                    client.execute(() -> onHello(hello));
                });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(RESULT_ID,
                (client, handler, buf, sender) -> {
                    ClientModProtocol.Result result = ClientModProtocol.Result.read(buf);
                    client.execute(() -> RuleEngine.onResult(result.code(), result.sessionToken()));
                });*/
        //?}
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); init() is unused there.
    public static void init() {
    }

    public static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        // The registrar version must match ClientModBridge's (server side) for NeoForge to
        // negotiate the channel. optional() so the companion can still connect to servers
        // without it; executesOn(MAIN) so handlers run on the client thread like Fabric's.
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar =
                event.registrar(String.valueOf(ClientModProtocol.PROTOCOL_VERSION))
                        .optional().executesOn(net.neoforged.neoforge.network.registration.HandlerThread.MAIN);
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC,
                (payload, context) -> onHello(payload.hello()));
        registrar.playToClient(ResultPayload.TYPE, ResultPayload.CODEC,
                (payload, context) -> RuleEngine.onResult(payload.result().code(), payload.result().sessionToken()));
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
        sendAuth(ClientModProtocol.MODE_PASSWORD, password, otp == null ? "" : otp, "", EMPTY_BYTES, EMPTY_BYTES);
    }

    public static void sendToken(String token) {
        sendAuth(ClientModProtocol.MODE_TOKEN, "", "", token, EMPTY_BYTES, EMPTY_BYTES);
    }

    public static void sendPasskey(byte[] publicKey, byte[] signature) {
        sendAuth(ClientModProtocol.MODE_PASSKEY, "", "", "", publicKey, signature);
    }

    public static void sendRegisterPasskey(byte[] publicKey) {
        sendAuth(ClientModProtocol.MODE_REGISTER_PASSKEY, "", "", "", publicKey, EMPTY_BYTES);
    }

    private static void sendAuth(int mode, String password, String otp, String token,
                                 byte[] publicKey, byte[] signature) {
        ClientModProtocol.Auth auth = new ClientModProtocol.Auth(mode, password, otp, token, publicKey, signature);
        //? if >=1.20.5 {
        AuthPayload payload = new AuthPayload(auth);
        //? if fabric {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        //?} else if >=1.21.9 {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        *///?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        *///?}
        //?} else {
        /*net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        auth.write(buf);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(AUTH_ID, buf);*/
        //?}
    }
}
