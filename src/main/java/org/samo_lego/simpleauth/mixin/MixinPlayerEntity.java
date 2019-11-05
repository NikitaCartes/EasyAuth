package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {
    /*@Inject(method = "dropItem", at = @At("HEAD"))
    private void dropItem(ItemStack itemStack_1, boolean boolean_1) {
    }*/
}
