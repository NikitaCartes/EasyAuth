package xyz.nikitacartes.easyauth.utils;

import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

public class StoneCutterUtils {

    public static String getUsername(ServerPlayer player) {
        return player.getScoreboardName();
    }

    public static Vec3 getPosition(ServerPlayer player) {
        return player.position();
    }

    public static void teleport(ServerPlayer player, LastLocation lastLocation, ServerLevel fallbackWorld) {
        player.teleportTo(
                lastLocation.dimension == null ? fallbackWorld : player.server.getLevel(lastLocation.dimension),
                lastLocation.position.x(),
                lastLocation.position.y(),
                lastLocation.position.z(),
                EnumSet.noneOf(Relative.class),
                lastLocation.yaw,
                lastLocation.pitch,
                true);
    }

    public static Level getWorld(Entity entity) {
        return entity.level();
    }

    public static ServerLevel getServerWorld(ServerPlayer player) {
        return player.level();
    }

    public static void killPlayer(ServerPlayer player) {
        player.kill(StoneCutterUtils.getServerWorld(player));

        StoneCutterUtils.getServerWorld(player).getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, player, (score) -> score.set(score.get() - 1));
    }

    public static String getName(NameAndId profile) {
        return profile.name();
    }

    public static String getName(GameProfile profile) {
        return profile.name();
    }

    public static String getName(Player player) {
        return getName(player.getGameProfile());
    }

    public static UUID getId(NameAndId profile) {
        return profile.id();
    }

    public static UUID getId(GameProfile profile) {
        return profile.id();
    }

    public static void readRootVehicle(ServerPlayer player, ValueInput rootVehicle) {
        player.loadAndSpawnParentVehicle(rootVehicle);
    }
    public static void startRiding(ServerPlayer player, Entity entity) {
        player.startRiding(entity, true, false);
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isAdministrator(PlayerList playerManager, ServerPlayer player) {
        return isAdministrator(playerManager, player.getGameProfile());
    }

    public static boolean isAdministrator(PlayerList playerManager, GameProfile profile) {
        ServerOpListEntry operatorEntry = playerManager.getOps().get(new NameAndId(profile));
        return isAdministrator(operatorEntry);
    }

    private static boolean isAdministrator(ServerOpListEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }
        return operatorEntry.permissions() == LevelBasedPermissionSet.GAMEMASTER ||
                operatorEntry.permissions() == LevelBasedPermissionSet.ADMIN ||
                operatorEntry.permissions() == LevelBasedPermissionSet.OWNER;
    }

    public static boolean isOperator(PlayerList playerManager, ServerPlayer player) {
        return isOperator(playerManager, player.getGameProfile());
    }

    public static boolean isOperator(PlayerList playerManager, GameProfile profile) {
        ServerOpListEntry operatorEntry = playerManager.getOps().get(new NameAndId(profile));
        return isOperator(operatorEntry);
    }

    private static boolean isOperator(ServerOpListEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }
        return operatorEntry.permissions() == LevelBasedPermissionSet.ADMIN ||
                operatorEntry.permissions() == LevelBasedPermissionSet.OWNER;
    }
}