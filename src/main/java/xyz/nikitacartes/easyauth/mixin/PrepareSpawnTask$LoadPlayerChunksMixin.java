package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.PrepareSpawnTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.utils.LastLocation;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(targets = "net.minecraft.server.network.PrepareSpawnTask$LoadPlayerChunks")
public abstract class PrepareSpawnTask$LoadPlayerChunksMixin {

    @Final
    @Shadow
    PrepareSpawnTask field_61135;

    @Final
    @Mutable
    @Shadow
    private ServerWorld world;

    @Final
    @Mutable
    @Shadow
    private Vec2f rotation;

    @ModifyVariable(method = "tryFinish()Lnet/minecraft/server/network/PrepareSpawnTask$PlayerSpawn;", at = @At("STORE"), ordinal = 0)
    private Vec3d saveRealCoordinates(Vec3d original) {
        PrepareSpawnTaskInterface field_61135 = (PrepareSpawnTaskInterface) this.field_61135;

        if (config.hidePlayerCoords && !field_61135.easyAuth$getAuthenticated()) {
            if (field_61135.easyAuth$getSpawnData() != null) {
                return new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
            }

            LastLocation lastLocation = new LastLocation(world.getRegistryKey(), original, rotation);
            field_61135.easyAuth$setSpawnData(lastLocation);

            world = field_61135.easyAuth$getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.worldSpawn.dimension)));
            rotation = new Vec2f(config.worldSpawn.yaw, config.worldSpawn.pitch);

            LogDebug(String.format("Saving position of player %s as %s", field_61135.easyAuth$getPlayer().name(), lastLocation));

            return new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
        } else {
            return original;
        }
    }
}
