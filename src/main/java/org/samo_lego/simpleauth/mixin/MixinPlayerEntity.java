package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    // Thanks to PR https://github.com/FabricMC/fabric/pull/260 and AbusedLib https://github.com/abused/AbusedLib
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("INVOKE"), cancellable = true)
    private void dropItem(ItemStack stack, boolean dropAtFeet, boolean saveThrower, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            // Canceling the item drop, as well as giving the items back to player (and updating inv with packet)
            player.giveItemStack(stack);
            player.inventory.updateItems();
            cir.cancel();
        }
    }
}