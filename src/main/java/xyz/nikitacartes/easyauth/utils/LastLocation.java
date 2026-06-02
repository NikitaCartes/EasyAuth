package xyz.nikitacartes.easyauth.utils;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LastLocation {

    public ResourceKey<Level> dimension;
    public Vec3 position;
    public float yaw;
    public float pitch;

    public String toString() {
        return String.format("LastLocation{dimension=%s, position=%s, yaw=%s, pitch=%s}", dimension, position, yaw, pitch);
    }

    public LastLocation(ResourceKey<Level> dimension, Vec3 position, Vec2 rotation) {
        this.dimension = dimension;
        this.position = position;
        this.yaw = rotation.x;
        this.pitch = rotation.y;
    }

    public LastLocation() {
    }
}
