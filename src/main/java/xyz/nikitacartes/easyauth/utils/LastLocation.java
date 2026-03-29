package xyz.nikitacartes.easyauth.utils;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LastLocation {

    public RegistryKey<World> dimension;
    public Vec3d position;
    public float yaw;
    public float pitch;

    public String toString() {
        return String.format("LastLocation{dimension=%s, position=%s, yaw=%s, pitch=%s}", dimension, position, yaw, pitch);
    }

    public LastLocation(RegistryKey<World> dimension, Vec3d position, Vec2f rotation) {
        this.dimension = dimension;
        this.position = position;
        this.yaw = rotation.x;
        this.pitch = rotation.y;
    }

    public LastLocation() {
    }
}
