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
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

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

    private static final int PROTOCOL_VERSION = 1;
    /** Kept in sync with the whitelist in AuthEventHandler.isAllowedPacket. */
    public static final String AUTH_CHANNEL = "easyauth:auth";

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

    //? if >=1.20.5 {
    /** S2C capability + auth-state announce, sent right after join. */
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

    /** C2S credentials; empty otp = none. */
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
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(AuthPayload.TYPE, AuthPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);*/
        //?}
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE,
                (payload, context) -> receive(context.player(), payload.password(), payload.otp()));
        //?} else {
        /*// Legacy channel API: the handler runs off-thread, so re-dispatch to the server thread.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(AUTH_ID,
                (server, player, handler, buf, sender) -> {
                    String password = buf.readUtf();
                    String otp = buf.readUtf();
                    server.execute(() -> receive(player, password, otp));
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
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC,
                (payload, context) -> receive((ServerPlayer) context.player(), payload.password(), payload.otp()));
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (easyauth:hello / " + AUTH_CHANNEL + ")");
    }*/
    //?}

    /** No-op for clients without the companion mod (they never declared the channel). */
    public static void sendHello(ServerPlayer player, boolean authenticated, boolean registered) {
        if (!initialized) {
            return;
        }
        //? if fabric {
        //? if >=1.20.5 {
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, HelloPayload.TYPE)) {
            return;
        }
        //?} else {
        /*if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, HELLO_ID)) {
            return;
        }*/
        //?}
        //?} else {
        /*if (!player.connection.hasChannel(HelloPayload.TYPE)) {
            return;
        }*/
        //?}
        boolean canAutoLogin = extendedConfig.clientMod.allowAutoLogin;
        boolean canAutoRegister = extendedConfig.clientMod.allowAutoRegister
                && !extendedConfig.disableRegistration
                && !config.enableGlobalPassword;
        //? if >=1.20.5 {
        HelloPayload hello = new HelloPayload(PROTOCOL_VERSION, canAutoLogin, canAutoRegister, registered, authenticated);
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
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, HELLO_ID, buf);*/
        //?}
    }

    // Runs on the server thread (fabric payload receivers are re-dispatched there; the legacy
    // handler re-dispatches via server.execute; NeoForge via executesOn(MAIN)).
    private static void receive(ServerPlayer player, String password, String otp) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        if (playerAuth.easyAuth$isAuthenticated()) {
            return;
        }
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (entry == null) {
            return;
        }
        String username = StoneCutterUtils.getUsername(player);
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
}
