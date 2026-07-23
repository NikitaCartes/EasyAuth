package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReturnValue(method = "isInvisible()Z", at = @At("RETURN"))
    public boolean easyAuth$isInvisible(boolean original) {
        return original;
    }

    // Damage gate. The isInvulnerable() getter is never read by the damage code, which
    // tests the `invulnerable` field directly here. Method was renamed in 1.21.2.
    //? if >= 1.21.2 {
    @ModifyReturnValue(method = "isAlwaysInvulnerableTo(Lnet/minecraft/entity/damage/DamageSource;)Z", at = @At("RETURN"))
    //?} else {
    /*@ModifyReturnValue(method = "isInvulnerableTo(Lnet/minecraft/entity/damage/DamageSource;)Z", at = @At("RETURN"))
    *///?}
    public boolean easyAuth$isInvulnerable(boolean original, DamageSource source) {
        return original;
    }
}
