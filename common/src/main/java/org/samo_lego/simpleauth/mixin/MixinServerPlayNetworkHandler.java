package org.samo_lego.simpleauth.mixin;

import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(
            method = "method_31286(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;updateLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerChat(String message, CallbackInfo ci) {
        ActionResult result = AuthEventHandler.onPlayerChat(this.player, message);
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
        if(packet.getAction() == SWAP_ITEM_WITH_OFFHAND) {
            ActionResult result = AuthEventHandler.onTakeItem(this.player);
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
        ActionResult result = AuthEventHandler.onPlayerMove(player);
        if (result == ActionResult.FAIL) {
            // A bit ugly, I know. (we need to update player position)
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.yaw, player.pitch);
            ci.cancel();
        }
    }

    @Inject(
            method = "onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        ActionResult result = AuthEventHandler.onTakeItem(this.player);

        if (result == ActionResult.FAIL) {
            // Canceling the item taking
            // Updating is not working yet
            ci.cancel();
        }
    }
}
