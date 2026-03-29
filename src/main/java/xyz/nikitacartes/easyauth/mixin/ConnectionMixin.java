package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static void easyAuth$onHandlePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        boolean isServerPlayNetworkHandler = listener instanceof ServerGamePacketListenerImpl;
        if (extendedConfig.allowAllPackets || (isServerPlayNetworkHandler && AuthEventHandler.isSkipAllAuthChecksApplicable(((ServerGamePacketListenerImpl) listener).player))) {
            return;
        }
        if (isServerPlayNetworkHandler) {
            ServerPlayer player = ((ServerGamePacketListenerImpl) listener).player;
            if (!((PlayerAuth) player).easyAuth$isAuthenticated()) {
                if (!AuthEventHandler.isAllowedPacket(player, packet)) {
                    ci.cancel();
                }
            }
        }
    }
}
