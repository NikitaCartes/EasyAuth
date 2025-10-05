//? if >= 1.21.9 {
package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.config;


@Mixin(targets = "net.minecraft.server.network.PrepareSpawnTask$PlayerSpawn")
public abstract class PrepareSpawnTask$PlayerSpawnMixin {

    @WrapOperation(method = "method_72303(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/storage/ReadView;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;readRootVehicle(Lnet/minecraft/storage/ReadView;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayerEntity instance, ReadView view, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, view);
    }
}
//?}