package org.samo_lego.simpleauth.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.simpleauth.SimpleAuth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity {

    @Inject(method = "onSlotUpdate(Lnet/minecraft/screen/ScreenHandler;ILnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        /*ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            // Canceling the item drop, as well as giving the items back to player (and updating inv with packet)
            player.inventory.insertStack(stack);
            player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(
                            -2,
                            player.inventory.selectedSlot,
                            player.inventory.getInvStack(player.inventory.selectedSlot))
            );
            // Packet for mouse-dropping inventory update
            player.currentScreenHandler.sendContentUpdates();
        }*/
            //player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-1, -1, player.inventory.getCursorStack()));
        if(!SimpleAuth.isAuthenticated(player)) {
            System.out.println("onSlotUpdate: " + stack.getItem().getTranslationKey());
            //player.inventory.setInvStack(slotId, stack);
            //ci.cancel();
        }
    }
}