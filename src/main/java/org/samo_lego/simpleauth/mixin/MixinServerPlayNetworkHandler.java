package org.samo_lego.simpleauth.mixin;

import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.event.entity.player.ChatCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerMoveCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method = "onGameMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket_1, CallbackInfo ci) {
        ActionResult result = ChatCallback.EVENT.invoker().onPlayerChat(player, chatMessageC2SPacket_1);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }
    // onClickWindow, onPickFromInventory todo*/
}
