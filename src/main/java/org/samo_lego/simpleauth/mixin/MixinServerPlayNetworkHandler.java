package org.samo_lego.simpleauth.mixin;

import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.class)
public abstract class MixinServerPlayNetworkHandler {

    @Inject(method = "read", at = @At("RETURN"))
    private void read(PacketByteBuf packetByteBuf_1, CallbackInfo ci) {
        System.out.println("Packet "+packetByteBuf_1);
    }
}
