package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReturnValue(method = "isInvisible()Z", at = @At("RETURN"))
    public boolean easyAuth$isInvisible(boolean original) {
        return original;
    }

    //? if >= 1.21.2 {
    @ModifyReturnValue(method = "isInvulnerableToBase(Lnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("RETURN"))
    //?} else {
    /*@ModifyReturnValue(method = "isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("RETURN"))
    *///?}
    public boolean easyAuth$isInvulnerable(boolean original, DamageSource source) {
        return original;
    }
}
