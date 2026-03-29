package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void easyAuth$onHandlePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        boolean isServerPlayNetworkHandler = listener instanceof ServerPlayNetworkHandler;
        if (extendedConfig.allowAllPackets || (isServerPlayNetworkHandler && AuthEventHandler.isSkipAllAuthChecksApplicable(((ServerPlayNetworkHandler) listener).player))) {
            return;
        }
        if (isServerPlayNetworkHandler) {
            ServerPlayerEntity player = ((ServerPlayNetworkHandler) listener).player;
            if (!((PlayerAuth) player).easyAuth$isAuthenticated()) {
                if (!AuthEventHandler.isAllowedPacket(player, packet)) {
                    ci.cancel();
                }
            }
        }
    }
}
