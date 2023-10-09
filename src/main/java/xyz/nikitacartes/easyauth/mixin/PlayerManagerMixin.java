package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Unique
    private final PlayerManager playerManager = (PlayerManager) (Object) this;

    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);
    }

    @ModifyVariable(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("STORE"), ordinal = 0)
    private RegistryKey<World> onPlayerConnect(RegistryKey<World> world, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
            return RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension));
        }
        return world;
    }

    @ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();

            NbtCompound nbtCompound = playerManager.loadPlayerData(player);
            if(nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                NbtCompound rootVehicle = nbtCompound.getCompound("RootVehicle");
                NbtCompound rootRootVehicle = new NbtCompound();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.containsUuid("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUuid("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", player.getName(), rootVehicle.getUuid("Attach")));
                }
            }

            LogDebug(String.format("Teleporting player %s", player.getName().getString()));
            LogDebug(String.format("Spawn position of player %s is %s", player.getName().getString(), config.worldSpawn));

            args.set(0, config.worldSpawn.x);
            args.set(1, config.worldSpawn.y);
            args.set(2, config.worldSpawn.z);
            args.set(3, config.worldSpawn.yaw);
            args.set(4, config.worldSpawn.pitch);
        }
    }

    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(ClientConnection clientConnection, ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(serverPlayerEntity);
    }

    @Redirect(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;findRespawnPosition(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3d> respawnPlayer(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive, ServerPlayerEntity player, boolean alive2) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return Optional.of(new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z));
        }
        return PlayerEntity.findRespawnPosition(world, pos, angle, forced, alive);
    }

    @Redirect(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getSpawnPointPosition()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos respawnPlayerBlockPos(ServerPlayerEntity instance, ServerPlayerEntity player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return new BlockPos((int) config.worldSpawn.x, (int) config.worldSpawn.y, (int) config.worldSpawn.z);
        }
        return instance.getSpawnPointPosition();
    }

    @Redirect(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getSpawnPointDimension()Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<World> respawnPlayerDimension(ServerPlayerEntity instance, ServerPlayerEntity player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension));
        }
        return instance.getSpawnPointDimension();
    }

    @Redirect(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;startRiding(Lnet/minecraft/entity/Entity;Z)Z"))
    private boolean onPlayerConnectStartRiding(ServerPlayerEntity instance, Entity entity, boolean force, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return false;
        }
        return instance.startRiding(entity, force);
    }

    @Redirect(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;hasVehicle()Z"))
    private boolean onPlayerConnectStartRiding(ServerPlayerEntity instance, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return true;
        }
        return instance.hasVehicle();
    }

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerLeave(serverPlayerEntity);
    }

    @Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress socketAddress, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        // Getting the player that is trying to join the server
        Text returnText = AuthEventHandler.checkCanPlayerJoinServer(profile, playerManager, socketAddress);

        if (returnText != null) {
            // Canceling player joining with the returnText message
            cir.setReturnValue(returnText);
        }
    }

    @Inject(method = "createStatHandler(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/stat/ServerStatHandler;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private void migrateOfflineStats(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir, @Local UUID uUID, @Local ServerStatHandler serverStatHandler, @Local(ordinal = 0) File serverStatsDir) {
        File onlineFile = new File(serverStatsDir, uUID + ".json");
        if (server.isOnlineMode() && !extendedConfig.forcedOfflineUuid && ((PlayerAuth) player).easyAuth$isUsingMojangAccount() && !onlineFile.exists()) {
            String playername = player.getGameProfile().getName();
            File offlineFile = new File(onlineFile.getParent(), Uuids.getOfflinePlayerUuid(playername) + ".json");
            if (!offlineFile.renameTo(onlineFile)) {
                LogWarn("Failed to migrate offline stats (" + offlineFile.getName() + ") for player " + playername + " to online stats (" + onlineFile.getName() + ")");
            } else {
                LogDebug("Migrated offline stats (" + offlineFile.getName() + ") for player " + playername + " to online stats (" + onlineFile.getName() + ")");
            }

            serverStatHandler.file = onlineFile;
        }
    }
}
