package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.utils.LastLocation;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Preparing")
public abstract class PrepareSpawnTask$PreparingMixin {

    @Final
    @Shadow
    PrepareSpawnTask this$0;

    @Final
    @Mutable
    @Shadow
    private ServerLevel spawnLevel;

    @Final
    @Mutable
    @Shadow
    private Vec2 spawnAngle;

    @ModifyVariable(method = "tick()Lnet/minecraft/server/network/config/PrepareSpawnTask$Ready;", at = @At("STORE"), ordinal = 0)
    private Vec3 saveRealCoordinates(Vec3 original) {
        PrepareSpawnTaskInterface prepareSpawnTask = (PrepareSpawnTaskInterface) this.this$0;

        if (config.hidePlayerCoords && !prepareSpawnTask.easyAuth$getAuthenticated()) {
            if (prepareSpawnTask.easyAuth$getSpawnData() != null) {
                return new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
            }

            LastLocation lastLocation = new LastLocation(spawnLevel.dimension(), original, spawnAngle);
            prepareSpawnTask.easyAuth$setSpawnData(lastLocation);

            spawnLevel = prepareSpawnTask.easyAuth$getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(config.worldSpawn.dimension)));
            spawnAngle = new Vec2(config.worldSpawn.yaw, config.worldSpawn.pitch);

            LogDebug(String.format("Saving position of player %s as %s", prepareSpawnTask.easyAuth$getPlayer().name(), lastLocation));

            return new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
        } else {
            return original;
        }
    }
}
