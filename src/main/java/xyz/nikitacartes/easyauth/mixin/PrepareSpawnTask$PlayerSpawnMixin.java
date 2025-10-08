package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.PrepareSpawnTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static xyz.nikitacartes.easyauth.EasyAuth.config;

@Mixin(PrepareSpawnTask.PlayerSpawn.class)
public abstract class PrepareSpawnTask$PlayerSpawnMixin {

    @Final
    @Shadow
    PrepareSpawnTask field_61141;

    @WrapOperation(method = "method_72303(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/storage/ReadView;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;readRootVehicle(Lnet/minecraft/storage/ReadView;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayerEntity instance, ReadView view, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, view);
    }

    @ModifyVariable(method = "onReady(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ConnectedClientData;)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At("STORE"), ordinal = 0)
    private ServerPlayerEntity saveRealCoordinates(ServerPlayerEntity original) {
        PlayerAuth player = (PlayerAuth) original;

        player.easyAuth$setLastLocation(((PrepareSpawnTaskInterface) field_61141).easyAuth$getSpawnData());
        player.easyAuth$setSkipAuth();
        player.easyAuth$savePlayerInfo();

        return original;
    }
}
