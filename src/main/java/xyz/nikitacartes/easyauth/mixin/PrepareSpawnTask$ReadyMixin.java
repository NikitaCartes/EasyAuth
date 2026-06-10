//? if >= 1.21.9 {
package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.config;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public abstract class PrepareSpawnTask$ReadyMixin {

    // NeoForge (and Fabric 26.1+) name the synthetic outer-class reference `this$0`;
    // older Fabric uses the intermediary `field_61141`. The alias lets one declaration match both.
    @Final
    @Shadow(remap = false, aliases = {"this$0"})
    PrepareSpawnTask field_61141;

    // NeoForge (always Mojmap) and deobfuscated Fabric 26.1 use the official lambda name;
    // only obfuscated Fabric (<26.1) uses the intermediary lambda name.
    //? if neoforge || >=26.1 {
    /*@WrapOperation(method = "lambda$spawn$1(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/storage/ValueInput;)V",
    *///?} else {
    @WrapOperation(method = "method_72303(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/storage/ValueInput;)V",
    //?}
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;loadAndSpawnParentVehicle(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayer instance, ValueInput view, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, view);
    }

    @ModifyVariable(method = "spawn(Lnet/minecraft/network/Connection;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("STORE"), ordinal = 0)
    private ServerPlayer saveRealCoordinates(ServerPlayer original) {
        PlayerAuth player = (PlayerAuth) original;

        player.easyAuth$setLastLocation(((PrepareSpawnTaskInterface) field_61141).easyAuth$getSpawnData());
        player.easyAuth$setSkipAuth();
        player.easyAuth$savePlayerInfo();

        return original;
    }
}
//?}