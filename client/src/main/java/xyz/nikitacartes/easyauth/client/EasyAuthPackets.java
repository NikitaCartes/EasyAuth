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
 * Wire twin of the server's {@code ClientModBridge}: {@code easyauth:hello} (S2C capability +
 * auth-state announce) and {@code easyauth:auth} (C2S credentials; the server routes them to
 * register or login by account state).
 *
 * <p>Works on every supported version (loader-specific classes are used fully-qualified to avoid
 * per-era import juggling). {@code >=1.20.5}: CustomPacketPayload API (Fabric
 * serverboundPlay/clientboundPlay at 26.1+, playC2S/playS2C below; NeoForge registrar + client send
 * via ClientPacketDistributor at 1.21.9+, PacketDistributor below). {@code <1.20.5} (Fabric only —
 * NeoForge starts at 1.21): the legacy ResourceLocation+FriendlyByteBuf channel API. The wire
 * format MUST stay in sync with ClientModBridge.
 */
public final class EasyAuthPackets {

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

    //? if >=1.20.5 {
    public record HelloPayload(int protocolVersion, boolean canAutoLogin, boolean canAutoRegister,
                               boolean registered, boolean authenticated) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> TYPE = new CustomPacketPayload.Type<>(HELLO_ID);

        public static final StreamCodec<FriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.protocolVersion());
                    buf.writeBoolean(payload.canAutoLogin());
                    buf.writeBoolean(payload.canAutoRegister());
                    buf.writeBoolean(payload.registered());
                    buf.writeBoolean(payload.authenticated());
                },
                buf -> new HelloPayload(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                        buf.readBoolean(), buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AuthPayload(String password, String otp) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AuthPayload> TYPE = new CustomPacketPayload.Type<>(AUTH_ID);

        public static final StreamCodec<FriendlyByteBuf, AuthPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.password());
                    buf.writeUtf(payload.otp());
                },
                buf -> new AuthPayload(buf.readUtf(), buf.readUtf()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    //?}

    private static void onHello(boolean canAutoLogin, boolean canAutoRegister, boolean registered, boolean authenticated) {
        RuleEngine.onHello(canAutoLogin, canAutoRegister, registered, authenticated);
    }

    //? if fabric {
    /** Call once from the client entrypoint (payload types must register early). */
    public static void init() {
        //? if >=1.20.5 {
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(AuthPayload.TYPE, AuthPayload.CODEC);*/
        //?}
        // Fabric play receivers run on the client main thread, same as the tick/join events.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE,
                (payload, context) -> onHello(payload.canAutoLogin(), payload.canAutoRegister(), payload.registered(), payload.authenticated()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so re-dispatch to the client thread.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(HELLO_ID,
                (client, handler, buf, sender) -> {
                    buf.readVarInt(); // protocol version (unused)
                    boolean canAutoLogin = buf.readBoolean();
                    boolean canAutoRegister = buf.readBoolean();
                    boolean registered = buf.readBoolean();
                    boolean authenticated = buf.readBoolean();
                    client.execute(() -> onHello(canAutoLogin, canAutoRegister, registered, authenticated));
                });*/
        //?}
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); init() is unused there.
    public static void init() {
    }

    public static void onRegisterPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        // Version "1" must match ClientModBridge's registrar (server side) for NeoForge to
        // negotiate the channel. optional() so the companion can still connect to servers
        // without it; executesOn(MAIN) so onHello runs on the client thread like Fabric's receiver.
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar("1")
                .optional().executesOn(net.neoforged.neoforge.network.registration.HandlerThread.MAIN);
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC,
                (payload, context) -> onHello(payload.canAutoLogin(), payload.canAutoRegister(), payload.registered(), payload.authenticated()));
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
        //? if >=1.20.5 {
        AuthPayload payload = new AuthPayload(password, otp == null ? "" : otp);
        //? if fabric {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        //?} else if >=1.21.9 {
        /*net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        *///?} else {
        /*net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        *///?}
        //?} else {
        /*net.minecraft.network.FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeUtf(password);
        buf.writeUtf(otp == null ? "" : otp);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(AUTH_ID, buf);*/
        //?}
    }
}
