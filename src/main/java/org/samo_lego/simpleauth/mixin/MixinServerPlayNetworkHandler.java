package org.samo_lego.simpleauth.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.event.entity.player.ChatCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerMoveCallback;
import org.samo_lego.simpleauth.event.item.TakeItemCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_HELD_ITEMS;

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
    private void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket, CallbackInfo ci) {
        ActionResult result = ChatCallback.EVENT.invoker().onPlayerChat(this.player, chatMessageC2SPacket);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onPlayerAction(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if(packet.getAction() == SWAP_HELD_ITEMS) {
            ActionResult result = TakeItemCallback.EVENT.invoker().onTakeItem(this.player);
            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }
    @Inject(
            method="onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket, CallbackInfo ci) {
        ActionResult result = PlayerMoveCallback.EVENT.invoker().onPlayerMove(this.player);
        if (result == ActionResult.FAIL) {
            // A bit ugly, I know. (we need to update player position)
            this.player.requestTeleport(this.player.getX(), this.player.getY(), this.player.getZ());
            ci.cancel();
        }
    }
}
