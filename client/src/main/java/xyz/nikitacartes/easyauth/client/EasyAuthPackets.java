package xyz.nikitacartes.easyauth.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
*///?}

/**
 * Wire twin of the server's {@code ClientModBridge}: {@code easyauth:hello} (S2C capability +
 * auth-state announce) and {@code easyauth:auth} (C2S credentials; the server routes them to
 * register or login by account state). The payload records are loader-agnostic; only registration
 * and sending differ (Fabric networking API vs NeoForge mod-bus registrar + ClientPacketDistributor).
 */
public final class EasyAuthPackets {

    private EasyAuthPackets() {
    }

    public record HelloPayload(int protocolVersion, boolean canAutoLogin, boolean canAutoRegister,
                               boolean registered, boolean authenticated) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("easyauth", "hello"));

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
        public static final CustomPacketPayload.Type<AuthPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("easyauth", "auth"));

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

    private static void onHello(HelloPayload payload) {
        RuleEngine.onHello(payload.canAutoLogin(), payload.canAutoRegister(),
                payload.registered(), payload.authenticated());
    }

    //? if fabric {
    /** Call once from the client entrypoint (payload types must register early). */
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        // Fabric play receivers run on the client main thread, same as the tick/join events.
        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) -> onHello(payload));
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); init() is unused there.
    public static void init() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Version "1" must match ClientModBridge's registrar (server side) for NeoForge to
        // negotiate the channel. optional() so the companion can still connect to servers
        // without it; executesOn(MAIN) so onHello runs on the client thread like Fabric's receiver.
        PayloadRegistrar registrar = event.registrar("1").optional().executesOn(HandlerThread.MAIN);
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC, (payload, context) -> onHello(payload));
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC, (payload, context) -> {}); // send-only
    }
    *///?}

    /** Whether the server declared the credentials channel (EasyAuth on 26.1+). */
    public static boolean serverSupportsPacketAuth() {
        //? if fabric {
        return ClientPlayNetworking.canSend(AuthPayload.TYPE);
        //?} else {
        /*ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(AuthPayload.TYPE);*/
        //?}
    }

    public static void sendCredentials(String password, String otp) {
        AuthPayload payload = new AuthPayload(password, otp == null ? "" : otp);
        //? if fabric {
        ClientPlayNetworking.send(payload);
        //?} else {
        /*ClientPacketDistributor.sendToServer(payload);*/
        //?}
    }
}
