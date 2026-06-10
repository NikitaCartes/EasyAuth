//? if >= 1.21.9 {
package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.core.registries.Registries;
//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.utils.LastLocation;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Preparing")
public abstract class PrepareSpawnTask$PreparingMixin {

    // NeoForge (and Fabric 26.1+) name the synthetic outer-class reference `this$0`;
    // older Fabric uses the intermediary `field_61135`. The alias lets one declaration match both.
    @Final
    @Shadow(remap = false, aliases = {"this$0"})
    PrepareSpawnTask field_61135;

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
        PrepareSpawnTaskInterface prepareSpawnTask = (PrepareSpawnTaskInterface) this.field_61135;

        if (config.hidePlayerCoords && !prepareSpawnTask.easyAuth$getAuthenticated()) {
            if (prepareSpawnTask.easyAuth$getSpawnData() != null) {
                return new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
            }

            LastLocation lastLocation = new LastLocation(spawnLevel.dimension(), original, spawnAngle);
            prepareSpawnTask.easyAuth$setSpawnData(lastLocation);

            ResourceKey<Level> worldKey = ResourceKey.create(
                Registries.DIMENSION,
                //? if >= 1.21.11 {
                Identifier.parse(config.worldSpawn.dimension)
                //?} else {
                /*ResourceLocation.parse(config.worldSpawn.dimension)
                *///?}
            );
            spawnLevel = prepareSpawnTask.easyAuth$getServer().getLevel(worldKey);
            spawnAngle = new Vec2(config.worldSpawn.yaw, config.worldSpawn.pitch);

            LogDebug(String.format("Saving position of player %s as %s", prepareSpawnTask.easyAuth$getPlayer().name(), lastLocation));

            return new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z);
        } else {
            return original;
        }
    }
}
//?}