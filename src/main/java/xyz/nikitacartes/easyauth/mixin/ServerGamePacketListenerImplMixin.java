package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND;
import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    // Afaik we don't really care if this is cancelled before or after the validateMessage
    // In case the player is not allowed to send message anyway then doing it before should save resources
    @Inject(
            method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;unpackAndApplyLastSeen(Lnet/minecraft/network/chat/LastSeenMessages$Update;)Ljava/util/Optional;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void onPlayerChat(ServerboundChatPacket packet, CallbackInfo ci) {
        InteractionResult result = AuthEventHandler.onPlayerChat(this.player);
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handlePlayerAction(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == SWAP_ITEM_WITH_OFFHAND) {
            InteractionResult result = AuthEventHandler.onTakeItem(this.player);
            if (result == InteractionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(ServerboundMovePlayerPacket playerMoveC2SPacket, CallbackInfo ci) {
        InteractionResult result = AuthEventHandler.onPlayerMove(player);
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSetCreativeModeSlot(Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    public void onCreativeInventoryAction(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        InteractionResult result = AuthEventHandler.onTakeItem(this.player);

        if (result == InteractionResult.FAIL) {
            // Canceling the item taking
            // Updating is not working yet
            ci.cancel();
        }
    }

    @WrapMethod(method = "handleChatSessionUpdate(Lnet/minecraft/network/protocol/game/ServerboundChatSessionUpdatePacket;)V")
    private void onPlayerSessionWrap(ServerboundChatSessionUpdatePacket packet, Operation<Void> original) {
        if (!extendedConfig.forcedOfflineUuid || !((PlayerAuth) this.player).easyAuth$isUsingMojangAccount()) {
            original.call(packet);
        }
    }

    @Inject(method = "onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("TAIL"))
    private void onPlayerDisconnectUnVanish(DisconnectionDetails details, CallbackInfo ci) {
        PlayerAuth playerAuth = (PlayerAuth) this.player;
        if (playerAuth.easyAuth$canSkipAuth() || playerAuth.easyAuth$isAuthenticated()) {
            return;
        }
        if (config.vanishUntilAuth) {
            VanishIntegration.setVanished(this.player, playerAuth.easyAuth$wasVanished());
        }
    }
}
