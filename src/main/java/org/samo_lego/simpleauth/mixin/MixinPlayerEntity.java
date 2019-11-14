package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {
    // Thanks to AbusedLib https://github.com/abused/AbusedLib
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
    private void dropItem(ItemStack stack, boolean boolean_1, boolean boolean_2, CallbackInfoReturnable<ItemStack> info) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if(DropItemCallback.EVENT.invoker().onDropItem(player)) {
            player.giveItemStack(stack);
            info.setReturnValue(ItemStack.EMPTY);
        }
    }
}
