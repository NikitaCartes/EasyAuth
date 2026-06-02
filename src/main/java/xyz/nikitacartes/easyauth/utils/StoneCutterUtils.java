package xyz.nikitacartes.easyauth.utils;

import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
//? if >= 1.21.11 {
import net.minecraft.server.permissions.LevelBasedPermissionSet;
//?}
//? if >= 1.21.2 {
import net.minecraft.world.entity.Relative;
//?}
//? if < 1.21.6 {
/*import net.minecraft.nbt.CompoundTag;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
//? if < 1.21.6 {
/*import net.minecraft.world.entity.EntityType;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if >= 1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
//?}
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
//? if >= 1.21.9 {
import net.minecraft.server.players.NameAndId;
//?}

import java.util.EnumSet;
//? if < 1.21.6 {
/*import java.util.Iterator;
*///?}
import java.util.Objects;
//? if < 1.21.6 {
/*import java.util.Optional;
*///?}
import java.util.UUID;

public class StoneCutterUtils {

    public static String getUsername(ServerPlayer player) {
        //? if >= 1.20.3 {
        return player.getScoreboardName();
        //?} else {
         /*return player.getName().getString();
        *///?}
    }

    public static Vec3 getPosition(ServerPlayer player) {
        //? if >= 1.21.9 {
        return player.position();
        //?} else {
        /*return player.position();
         *///?}
    }

    public static void teleport(ServerPlayer player, LastLocation lastLocation, ServerLevel fallbackWorld) {
        //? if >= 1.21.2 {
        ServerLevel targetWorld = Objects.requireNonNull(lastLocation.dimension == null ? fallbackWorld : player.server.getLevel(lastLocation.dimension));
        player.teleportTo(
            targetWorld,
                lastLocation.position.x,
                lastLocation.position.y,
                lastLocation.position.z,
                EnumSet.noneOf(Relative.class),
                lastLocation.yaw,
                lastLocation.pitch,
                true);
        //?} else {
            /*player.teleportTo(
                lastLocation.dimension == null ? fallbackWorld : player.server.getLevel(lastLocation.dimension),
                lastLocation.position.x,
                lastLocation.position.y,
                lastLocation.position.z,
                lastLocation.yaw,
                lastLocation.pitch);
         *///?}
    }

    public static Level getWorld(Entity entity) {
        //? if >= 1.20 {
        return entity.level();
        //?} else {
         /*return entity.getLevel();
        *///?}

    }

    public static ServerLevel getServerWorld(ServerPlayer player) {
        //? if >= 1.21.9 {
        return player.level();
        //?} else if >= 1.21.6 {
         /*return player.level();
        *///?} else if >= 1.20 {
         /*return player.serverLevel();
        *///?} else {
         /*return player.getLevel();
        *///?}
    }

    public static void killPlayer(ServerPlayer player) {
        //? if >= 1.21.2 {
        player.kill(Objects.requireNonNull(StoneCutterUtils.getServerWorld(player)));
        //?} else {
        /*player.kill();
         *///?}

        //? if >= 1.20.3 {
        StoneCutterUtils.getServerWorld(player).getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, player, (score) -> score.set(score.get() - 1));
        //?} else {
        /*player.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, getUsername(player), (score) -> score.setScore(score.getScore() - 1));
         *///?}
    }

    //? if >= 1.21.9 {
    public static String getName(NameAndId profile) {
        return profile.name();
    }
    //?}

    public static String getName(GameProfile profile) {
        //? if >= 1.21.9 {
        return profile.name();
        //?} else {
        /*return profile.getName();
        *///?}
    }

    public static String getName(Player player) {
        return getName(player.getGameProfile());
    }

    //? if >= 1.21.9 {
    public static UUID getId(NameAndId profile) {
        return profile.id();
    }
    //?}

    public static UUID getId(GameProfile profile) {
        //? if >= 1.21.9 {
        return profile.id();
        //?} else {
        /*return profile.getId();
         *///?}
    }

    //? if >= 1.21.6 {
    public static void readRootVehicle(ServerPlayer player, ValueInput rootVehicle) {
        player.loadAndSpawnParentVehicle(Objects.requireNonNull(rootVehicle));
    }
    //?} else {
    /*public static void readRootVehicle(ServerPlayer player, CompoundTag rootVehicle) {
        //? if >= 1.21.5 {
        player.loadAndSpawnParentVehicle(rootVehicle);
        //?} else >= 1.21.2 {
        /^player.loadAndSpawnParentVehicle(Optional.of(rootVehicle));
         ^///?} else {
            /^CompoundTag nbtCompound = rootVehicle.getCompound("RootVehicle");
            //? if > 1.19.4 {
             Entity entity = EntityType.loadEntityRecursive(nbtCompound.getCompound("Entity"), player.serverLevel(), (vehicle) -> !player.serverLevel().addWithUUID(vehicle) ? null : vehicle);
            //?} else {
            /^¹Entity entity = EntityType.loadEntityRecursive(nbtCompound.getCompound("Entity"), player.getLevel(), (vehicle) -> !player.getLevel().addWithUUID(vehicle) ? null : vehicle);
            ¹^///?}
            if (entity != null) {
                UUID uUID;
                if (nbtCompound.hasUUID("Attach")) {
                    uUID = nbtCompound.getUUID("Attach");
                } else {
                    uUID = null;
                }

                Iterator var23;
                Entity entity2;
                if (entity.getUUID().equals(uUID)) {
                    player.startRiding(entity, true);
                } else {
                    var23 = entity.getIndirectPassengers().iterator();

                    while(var23.hasNext()) {
                        entity2 = (Entity)var23.next();
                        if (entity2.getUUID().equals(uUID)) {
                            player.startRiding(entity2, true);
                            break;
                        }
                    }
                }
            }
            ^///?}
    }
    *///?}

    public static void startRiding(ServerPlayer player, Entity entity) {
        //? if >= 1.21.9 {
        player.startRiding(Objects.requireNonNull(entity), true, false);
        //?} else {
        /*player.startRiding(entity, true);
         *///?}
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isAdministrator(PlayerList playerManager, ServerPlayer player) {
        return isAdministrator(playerManager, player.getGameProfile());
    }

    public static boolean isAdministrator(PlayerList playerManager, GameProfile profile) {
        //? if >= 1.21.9 {
        ServerOpListEntry operatorEntry = playerManager.getOps().get(new NameAndId(Objects.requireNonNull(profile)));
        //?} else {
        /*ServerOpListEntry operatorEntry = playerManager.getOps().get(profile);
         *///?}
        return isAdministrator(operatorEntry);
    }

    private static boolean isAdministrator(ServerOpListEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }

        //? if >= 1.21.11 {
        return operatorEntry.permissions() == LevelBasedPermissionSet.GAMEMASTER ||
            operatorEntry.permissions() == LevelBasedPermissionSet.ADMIN ||
            operatorEntry.permissions() == LevelBasedPermissionSet.OWNER;
        //?} else {
        /*return operatorEntry.getLevel() >= 2;
         *///?}
    }

    public static boolean isOperator(PlayerList playerManager, ServerPlayer player) {
        return isOperator(playerManager, player.getGameProfile());
    }

    public static boolean isOperator(PlayerList playerManager, GameProfile profile) {
        //? if >= 1.21.9 {
        ServerOpListEntry operatorEntry = playerManager.getOps().get(new NameAndId(Objects.requireNonNull(profile)));
        //?} else {
        /*ServerOpListEntry operatorEntry = playerManager.getOps().get(profile);
         *///?}
        return isOperator(operatorEntry);
    }

    private static boolean isOperator(ServerOpListEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }

        //? if >= 1.21.11 {
        return operatorEntry.permissions() == LevelBasedPermissionSet.ADMIN ||
            operatorEntry.permissions() == LevelBasedPermissionSet.OWNER;
        //?} else {
        /*return operatorEntry.getLevel() >= 3;
         *///?}
    }

}
