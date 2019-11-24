package org.samo_lego.simpleauth.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.event.entity.player.OnChatCallback;
import org.samo_lego.simpleauth.event.entity.player.OnPlayerMoveCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    // TODO
    @Inject(method = "onChatMessage", at = @At(value = "HEAD"), cancellable = true)
    private void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket_1, CallbackInfoReturnable cir) {
        ActionResult result = OnChatCallback.EVENT.invoker().onPlayerChat(player, chatMessageC2SPacket_1);

        System.out.println("Mixined");
        if (result == ActionResult.FAIL) {
            cir.cancel();
        }
    }

    // TODO
    @Inject(method="onPlayerMove", at = @At(value = "HEAD"), cancellable = true)
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket_1, CallbackInfo cir) {
        ActionResult result = OnPlayerMoveCallback.EVENT.invoker().onPlayerMove(player);

        System.out.println("Player move");
        if (result == ActionResult.FAIL) {
            cir.cancel();
        }
    }
}
