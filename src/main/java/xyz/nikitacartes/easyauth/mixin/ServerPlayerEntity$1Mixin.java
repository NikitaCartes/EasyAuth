package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(targets = "net.minecraft.server.network.ServerPlayerEntity$1")
public class ServerPlayerEntity$1Mixin {

    @Final
    @Shadow
    ServerPlayerEntity field_29182;

    @Inject(method = "updateState(Lnet/minecraft/screen/ScreenHandler;Lnet/minecraft/util/collection/DefaultedList;Lnet/minecraft/item/ItemStack;[I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void updateStateMixin(CallbackInfo ci) {
        if (extendedConfig.hideInventory && !((PlayerAuth) field_29182).easyAuth$isAuthenticated()) {
            ci.cancel();
        }
    }
}
