package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.simpleauth.SimpleAuth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class MixinPlayerInventory {

    @Final
    @Shadow
    public PlayerEntity player;

    @Inject(method="setInvStack(ILnet/minecraft/item/ItemStack;)V", at = @At(value = "HEAD"), cancellable = true)
    private void setInvStack(int slot, ItemStack stack, CallbackInfo ci) {
        /*ServerPlayerEntity player = (ServerPlayerEntity) this.player;
        ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            player.inventory
            player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(
                            -2,
                            player.inventory.selectedSlot,
                            player.inventory.getInvStack(player.inventory.selectedSlot))
            );
            // Packet for mouse-dropping inventory update
            player.currentScreenHandler.sendContentUpdates();
            ci.cancel();
        }*/
        System.out.println("Setting inv stack: " + slot + "(slot) " + stack.getCount()+ "(amount)" + stack.getItem().getTranslationKey() + "(item)");
    }

    // Thank you Giselbaer for the help provided!
    @Inject(method = "takeInvStack(II)Lnet/minecraft/item/ItemStack;", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void takeInvStack(int slot, int amount, CallbackInfoReturnable<ItemStack> cir, List<ItemStack> list) {

        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player)) {
            // Getting back the default item count
            int initialCount = player.inventory.getInvStack(slot).getCount() + amount;
            // Getting itemstack that would be returned
            ItemStack stack = list != null && !list.get(slot).isEmpty() ? Inventories.splitStack(list, slot, amount) : ItemStack.EMPTY;
            System.out.println("takeInvStack: " + slot + "(slot) " + amount + "(amount)" + stack.getItem().getTranslationKey() + "(item)");

            // Setting stack value back to default (before it was taken or dropped)
            stack.setCount(initialCount);

            // Setting the stack back to inventory
            player.inventory.setInvStack(slot, stack);

            cir.setReturnValue(stack);
        }
    }
}
