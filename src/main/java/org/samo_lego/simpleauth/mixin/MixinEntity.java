package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    /*@Inject(method = "canSeePlayer", at = @At("HEAD"), cancellable = true)
    private void canSeePlayer(PlayerEntity playerEntity_1, CallbackInfoReturnable<Boolean> info) {
        if(!CanSeePlayerCallback.EVENT.invoker().canSeePlayer((ServerPlayerEntity) playerEntity_1))
            info.setReturnValue(false);

    }*/
}
