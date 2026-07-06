package xyz.nikitacartes.easyauth.integrations;
//? if >= 26.1 {

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
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
 * client never has to guess between the two). Compliant clients also honour the capability
 * flags, which is what makes the {@code client-mod} options in extended.conf enforceable.
 *
 * <p>MC 26.1+ on both loaders (the companion mod does not exist below that; older versions keep
 * working through the client's command-tree fallback). The payload records and the receive
 * handler are loader-agnostic; only registration and sending differ (Fabric networking API vs
 * NeoForge {@link RegisterPayloadHandlersEvent} + {@link PacketDistributor}).
 */
public final class ClientModBridge {

    private static final int PROTOCOL_VERSION = 1;
    /** Kept in sync with the whitelist in AuthEventHandler.isAllowedPacket. */
    public static final String AUTH_CHANNEL = "easyauth:auth";

    private static volatile boolean initialized = false;

    private ClientModBridge() {
    }

    /** S2C capability + auth-state announce, sent right after join. */
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

    /** C2S credentials; empty otp = none. */
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
        PayloadTypeRegistry.serverboundPlay().register(AuthPayload.TYPE, AuthPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE,
                (payload, context) -> receive(context.player(), payload));
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (easyauth:hello / " + AUTH_CHANNEL + ")");
    }
    //?} else {
    /*// NeoForge registers through the mod bus (onRegisterPayloads); nothing to do at mod init.
    public static void init() {
    }

    /^*
     * Registers the channels + receiver. Wired from EasyAuthNeoForge's mod bus. Dedicated
     * servers only: on a client instance the companion mod owns these payload ids, and
     * registering them twice would crash (Fabric's init() guards the same way with env==SERVER).
     * ponytail: LAN hosts therefore have no packet auth — the command fallback covers them.
     ^/
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        if (initialized || FMLEnvironment.getDist() != Dist.DEDICATED_SERVER) {
            return;
        }
        // optional() so vanilla / non-companion clients are not rejected for lacking the channel.
        PayloadRegistrar registrar = event.registrar(String.valueOf(PROTOCOL_VERSION))
                .optional().executesOn(HandlerThread.MAIN);
        registrar.playToClient(HelloPayload.TYPE, HelloPayload.CODEC); // send-only from the server
        registrar.playToServer(AuthPayload.TYPE, AuthPayload.CODEC,
                (payload, context) -> receive((ServerPlayer) context.player(), payload));
        initialized = true;
        LogInfo("EasyAuth Client packet channels registered (easyauth:hello / " + AUTH_CHANNEL + ")");
    }
    *///?}

    /** No-op for clients without the companion mod (they never declared the channel). */
    public static void sendHello(ServerPlayer player, boolean authenticated, boolean registered) {
        //? if fabric {
        if (!initialized || !ServerPlayNetworking.canSend(player, HelloPayload.TYPE)) {
            return;
        }
        //?} else {
        /*if (!initialized || !player.connection.hasChannel(HelloPayload.TYPE)) {
            return;
        }
        *///?}
        boolean canAutoLogin = extendedConfig.clientMod.allowAutoLogin;
        boolean canAutoRegister = extendedConfig.clientMod.allowAutoRegister
                && !extendedConfig.disableRegistration
                && !config.enableGlobalPassword;
        HelloPayload hello = new HelloPayload(PROTOCOL_VERSION, canAutoLogin, canAutoRegister, registered, authenticated);
        //? if fabric {
        ServerPlayNetworking.send(player, hello);
        //?} else {
        /*PacketDistributor.sendToPlayer(player, hello);*/
        //?}
    }

    // Runs on the server thread (fabric receivers are re-dispatched there; NeoForge via executesOn(MAIN)).
    private static void receive(ServerPlayer player, AuthPayload payload) {
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
                RegisterCommand.register(player.createCommandSourceStack(), payload.password(), payload.password());
            } else {
                if (!extendedConfig.clientMod.allowAutoLogin) {
                    LogDebug("Ignoring packet login from " + username + " (disabled in extended.conf)");
                    return;
                }
                LoginCommand.login(player.createCommandSourceStack(), payload.password(),
                        payload.otp().isEmpty() ? null : payload.otp());
            }
        } catch (CommandSyntaxException e) {
            // getPlayerOrException cannot fail for a player-created source
        }
    }
}
//?}
