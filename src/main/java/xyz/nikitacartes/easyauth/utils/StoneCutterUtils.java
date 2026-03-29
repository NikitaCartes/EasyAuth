package xyz.nikitacartes.easyauth.utils;

import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

public class StoneCutterUtils {

    public static String getUsername(ServerPlayerEntity player) {
        return player.getNameForScoreboard();
    }

    public static Vec3d getPosition(ServerPlayerEntity player) {
        return player.getEntityPos();
    }

    public static void teleport(ServerPlayerEntity player, LastLocation lastLocation, ServerWorld fallbackWorld) {
        player.teleport(
                lastLocation.dimension == null ? fallbackWorld : player.server.getWorld(lastLocation.dimension),
                lastLocation.position.getX(),
                lastLocation.position.getY(),
                lastLocation.position.getZ(),
                EnumSet.noneOf(PositionFlag.class),
                lastLocation.yaw,
                lastLocation.pitch,
                true);
    }

    public static World getWorld(Entity entity) {
        return entity.getEntityWorld();
    }

    public static ServerWorld getServerWorld(ServerPlayerEntity player) {
        return player.getEntityWorld();
    }

    public static void killPlayer(ServerPlayerEntity player) {
        player.kill(StoneCutterUtils.getServerWorld(player));

        StoneCutterUtils.getServerWorld(player).getScoreboard().forEachScore(ScoreboardCriterion.DEATH_COUNT, player, (score) -> score.setScore(score.getScore() - 1));
    }

    public static String getName(PlayerConfigEntry profile) {
        return profile.name();
    }

    public static String getName(GameProfile profile) {
        return profile.name();
    }

    public static String getName(PlayerEntity player) {
        return getName(player.getGameProfile());
    }

    public static UUID getId(PlayerConfigEntry profile) {
        return profile.id();
    }

    public static UUID getId(GameProfile profile) {
        return profile.id();
    }

    public static void readRootVehicle(ServerPlayerEntity player, ReadView rootVehicle) {
        player.readRootVehicle(rootVehicle);
    }
    public static void startRiding(ServerPlayerEntity player, Entity entity) {
        player.startRiding(entity, true, false);
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isAdministrator(PlayerManager playerManager, ServerPlayerEntity player) {
        return isAdministrator(playerManager, player.getGameProfile());
    }

    public static boolean isAdministrator(PlayerManager playerManager, GameProfile profile) {
        OperatorEntry operatorEntry = playerManager.getOpList().get(new PlayerConfigEntry(profile));
        return isAdministrator(operatorEntry);
    }

    private static boolean isAdministrator(OperatorEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }
        return operatorEntry.getLevel() == LeveledPermissionPredicate.GAMEMASTERS ||
                operatorEntry.getLevel() == LeveledPermissionPredicate.ADMINS ||
                operatorEntry.getLevel() == LeveledPermissionPredicate.OWNERS;
    }

    public static boolean isOperator(PlayerManager playerManager, ServerPlayerEntity player) {
        return isOperator(playerManager, player.getGameProfile());
    }

    public static boolean isOperator(PlayerManager playerManager, GameProfile profile) {
        OperatorEntry operatorEntry = playerManager.getOpList().get(new PlayerConfigEntry(profile));
        return isOperator(operatorEntry);
    }

    private static boolean isOperator(OperatorEntry operatorEntry) {
        if (operatorEntry == null) {
            return false;
        }
        return operatorEntry.getLevel() == LeveledPermissionPredicate.ADMINS ||
                operatorEntry.getLevel() == LeveledPermissionPredicate.OWNERS;
    }
}