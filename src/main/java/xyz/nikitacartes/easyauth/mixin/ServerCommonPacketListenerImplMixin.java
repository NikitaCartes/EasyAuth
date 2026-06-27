package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
//? if >= 1.20.2 {
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

// send(Packet) moved up to ServerCommonPacketListenerImpl in 1.20.2; before that it lived on ServerGamePacketListenerImpl.
//? if >= 1.20.2 {
@Mixin(ServerCommonPacketListenerImpl.class)
//?} else {
/*@Mixin(ServerGamePacketListenerImpl.class)
*///?}
public abstract class ServerCommonPacketListenerImplMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void easyAuth$hideChatFromUnauthenticated(Packet<?> packet, CallbackInfo ci) {
        // On 1.20.2+ this also runs for the configuration listener (shared superclass)
        if (((Object) this) instanceof ServerGamePacketListenerImpl listener
                && AuthEventHandler.shouldHideClientboundChat(listener.player, packet)) {
            ci.cancel();
        }
    }
}
