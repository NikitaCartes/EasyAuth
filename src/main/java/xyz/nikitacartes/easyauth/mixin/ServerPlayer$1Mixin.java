package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$1")
public class ServerPlayer$1Mixin {

    //? if >= 1.21.5 {
    @Final
    @Shadow(remap = false, aliases = {"this$0"})
    ServerPlayer field_58075;

    @Inject(method = "sendInitialData(Lnet/minecraft/world/inventory/AbstractContainerMenu;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;[I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void updateStateMixin(CallbackInfo ci) {
        if (extendedConfig.hideInventory && !((PlayerAuth) field_58075).easyAuth$isAuthenticated()) {
            ci.cancel();
        }
    }
    //?} else {

    /*@Final
    @Shadow(remap = false, aliases = {"this$0"})
    ServerPlayer field_29182;

    @Inject(method = "sendInitialData(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;[I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void updateStateMixin(CallbackInfo ci) {
        if (extendedConfig.hideInventory && !((PlayerAuth) field_29182).easyAuth$isAuthenticated()) {
            ci.cancel();
        }
    }
    *///?}
}
