package xyz.nikitacartes.easyauth.mixin;
//? if >= 1.21.6 {

import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.dialog.DialogHandler;

/**
 * Routes dialog submissions to {@link DialogHandler}. The method is implemented on the common
 * listener but only game-phase connections (which carry a player) are relevant. Injecting at
 * RETURN means we run once, on the server thread (the network-thread call rescheduled itself).
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class CustomClickActionMixin {

    @Inject(method = "handleCustomClickAction(Lnet/minecraft/network/protocol/common/ServerboundCustomClickActionPacket;)V", at = @At("RETURN"))
    private void easyauth$onCustomClick(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        if ((Object) this instanceof ServerGamePacketListenerImpl listener) {
            DialogHandler.handle(listener.player, packet.id(), packet.payload());
        }
    }
}
//?}
