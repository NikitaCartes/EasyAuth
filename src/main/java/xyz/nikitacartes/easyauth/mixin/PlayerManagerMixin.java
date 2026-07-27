package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
//? if >= 1.21.9 {
import net.minecraft.server.PlayerConfigEntry;
//?}
import net.minecraft.server.PlayerManager;
//? if >= 1.20.2 {
import net.minecraft.server.network.ConnectedClientData;
//?}
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
//? if >= 1.21.6 {
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
//?}
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getName;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Unique
    private final PlayerManager playerManager = (PlayerManager) (Object) this;

    @Final
    @Shadow
    private MinecraftServer server;

    //? if >= 1.21.9 {
    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);

        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            try (ErrorReporter.Logging logging = new ErrorReporter.Logging(player.getErrorReporterContext(), LOGGER)) {

                playerManager.loadPlayerData(new PlayerConfigEntry(player.getGameProfile())).flatMap(compound -> compound.getCompound("RootVehicle")).ifPresent(rootVehicle -> {
                    NbtCompound rootRootVehicle = new NbtCompound();
                    rootRootVehicle.put("RootVehicle", rootVehicle);
                    ReadView readView = NbtReadView.create(logging, player.getRegistryManager(), rootRootVehicle);
                    ((PlayerAuth) player).easyAuth$setRootVehicle(readView);

                    rootVehicle.get("Attach", Uuids.INT_STREAM_CODEC).ifPresent(uUID -> {
                        ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                        LogDebug(String.format("Saving vehicle of player %s as %s", player.getNameForScoreboard(), uUID));
                    });
                });
            }
        }

        ((PlayerAuth) player).easyAuth$setSkipAuth();
    }
    //?} else if >= 1.20.2 {
    /*@Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);
    }
    *///?} else {
    /*@Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);
    }
    *///?}

    //? if >= 1.21.9 {
    //?} else if = 1.21.6 {
    /*@ModifyExpressionValue(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;flatMap(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private  Optional<RegistryKey<World>> onPlayerConnect(Optional<RegistryKey<World>> original, @Local(argsOnly = true) ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(original.orElse(World.OVERWORLD));
            return Optional.of(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.worldSpawn.dimension)));
        }
        return original;
    }
    *///?} else if >= 1.21 {
    /*@ModifyVariable(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At("STORE"), ordinal = 0)
    private RegistryKey<World> onPlayerConnect(RegistryKey<World> world, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
            return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.worldSpawn.dimension));
        }
        return world;
    }
    *///?} else if >= 1.20.2 {
    /*@ModifyVariable(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
                at = @At("STORE"), ordinal = 0)
        private RegistryKey<World> onPlayerConnect(RegistryKey<World> world, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
            if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
                ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
                return RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension));
            }
            return world;
        }
    *///?} else {
    /*@ModifyVariable(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("STORE"), ordinal = 0)
    private RegistryKey<World> onPlayerConnect(RegistryKey<World> world, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
            return RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension));
        }
        return world;
    }
    *///?}

    //? if >= 1.21.9 {
    //?} else if >= 1.21.6 {
    /*@ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = StoneCutterUtils.getUsername(player);

            try (ErrorReporter.Logging logging = new ErrorReporter.Logging(player.getErrorReporterContext(), LOGGER)) {
                playerManager.loadPlayerData(player, logging).flatMap(view -> view.getOptionalReadView("RootVehicle")).ifPresent(rootVehicleView -> {
                    NbtCompound rootRootVehicle = new NbtCompound();
                    rootRootVehicle.put("RootVehicle", ((NbtReadView) rootVehicleView).nbt);
                    ReadView rootVehicle = NbtReadView.create(logging, player.getRegistryManager(), rootRootVehicle);
                    ((PlayerAuth) player).easyAuth$setRootVehicle(rootVehicle);

                    rootVehicleView.read("Attach", Uuids.INT_STREAM_CODEC).ifPresent(uUID -> {
                        ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                        LogDebug(String.format("Saving vehicle of player %s as %s", username, uUID));
                    });
                });
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.21.5 {
    /*@ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = StoneCutterUtils.getUsername(player);
            playerManager.loadPlayerData(player).flatMap(compound -> compound.getCompound("RootVehicle")).ifPresent(rootVehicle -> {
                NbtCompound rootRootVehicle = new NbtCompound();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                rootVehicle.get("Attach", Uuids.INT_STREAM_CODEC).ifPresent(uUID -> {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, uUID));
                });
            });
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.20.5 {
    /*@ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = StoneCutterUtils.getUsername(player);

            Optional<NbtCompound> nbtCompound = playerManager.loadPlayerData(player);
            if(nbtCompound.isPresent() && nbtCompound.get().contains("RootVehicle", 10)) {
                NbtCompound rootVehicle = nbtCompound.get().getCompound("RootVehicle");
                NbtCompound rootRootVehicle = new NbtCompound();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.containsUuid("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUuid("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUuid("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.20.2 {
    /*@ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = StoneCutterUtils.getUsername(player);


            NbtCompound nbtCompound = playerManager.loadPlayerData(player);
            if(nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                NbtCompound rootVehicle = nbtCompound.getCompound("RootVehicle");
                NbtCompound rootRootVehicle = new NbtCompound();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.containsUuid("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUuid("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUuid("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else {
        /*@ModifyArgs(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;requestTeleport(DDDFF)V"))
    private void onPlayerConnect(Args args, ClientConnection connection, ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = StoneCutterUtils.getUsername(player);

            NbtCompound nbtCompound = playerManager.loadPlayerData(player);
            if(nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                NbtCompound rootVehicle = nbtCompound.getCompound("RootVehicle");
                NbtCompound rootRootVehicle = new NbtCompound();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.containsUuid("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUuid("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUuid("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?}

    //? if >= 1.20.2 {
    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(player);
    }
    //?} else {
    /*@Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(ClientConnection clientConnection, ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(serverPlayerEntity);
    }
    *///?}

    //? if >=1.21 {
    @WrapOperation(method = "respawnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getRespawnTarget(ZLnet/minecraft/world/TeleportTarget$PostDimensionTransition;)Lnet/minecraft/world/TeleportTarget;"))
    private TeleportTarget replaceRespawnTarget(ServerPlayerEntity instance, boolean alive, TeleportTarget.PostDimensionTransition postDimensionTransition, Operation<TeleportTarget> original) {
        //? if >=1.21.2 {
        if (alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
        //?} else {
        /*if (!alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
        *///?}
            return new TeleportTarget(
                this.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.worldSpawn.dimension))),
                new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z),
                new Vec3d(0.0F, 0.0F, 0.0F), config.worldSpawn.yaw, config.worldSpawn.pitch, postDimensionTransition
            );
        }
        return original.call(instance, alive, postDimensionTransition);
    }
    //?} else {
    /*@WrapOperation(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;findRespawnPosition(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3d> respawnPlayer(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive, Operation<Optional<Vec3d>> original, ServerPlayerEntity player) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return Optional.of(new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z));
        }
        return original.call(world, pos, angle, forced, alive);
    }

    @WrapOperation(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getSpawnPointPosition()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos respawnPlayerBlockPos(ServerPlayerEntity instance, Operation<BlockPos> original, ServerPlayerEntity player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return new BlockPos((int) config.worldSpawn.x, (int) config.worldSpawn.y, (int) config.worldSpawn.z);
        }
        return original.call(instance);
    }

    @WrapOperation(method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getSpawnPointDimension()Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<World> respawnPlayerDimension(ServerPlayerEntity instance, Operation<RegistryKey<World>> original, ServerPlayerEntity player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension));
        }
        return original.call(instance);
    }
    *///?}

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerLeave(serverPlayerEntity);
    }

    //? if >= 1.21.9 {
    @Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lnet/minecraft/server/PlayerConfigEntry;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, PlayerConfigEntry profile, CallbackInfoReturnable<Text> cir) {
    //?} else {
    /*@Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
    *///?}
        // Getting the player that is trying to join the server
        Text returnText = AuthEventHandler.checkCanPlayerJoinServer(profile, playerManager, address);

        if (returnText != null) {
            // Canceling player joining with the returnText message
            cir.setReturnValue(returnText);
        }
    }

    //? if >= 1.21.11 {
    @ModifyReturnValue(method = "locateStatFilePath(Lcom/mojang/authlib/GameProfile;)Ljava/nio/file/Path;",
            at = @At("RETURN")
    )
    private Path migrateOfflineStats(Path original, @Local(ordinal = 0) Path parentPath, @Local(ordinal = 1) Path onlinePath, @Local(argsOnly = true) GameProfile profile) {
        if (!server.isOnlineMode() || extendedConfig.forcedOfflineUuid || Files.exists(onlinePath)) {
            return original;
        }

        PlayerEntity player = server.getPlayerManager().getPlayer(profile.id());
        if (player != null && ((PlayerAuth) player).easyAuth$isUsingMojangAccount()) {
            String playername = getName(profile);
            Path offlinePath = parentPath.resolve(Uuids.getOfflinePlayerUuid(playername) + ".json");
            if (!Files.exists(offlinePath)) {
                return original;
            }
            try {
                Files.move(offlinePath, onlinePath);
                LogDebug("Migrated offline stats (" + offlinePath.getFileName() + ") for player " + playername + " to online stats (" + onlinePath.getFileName() + ")");
            } catch (Exception e) {
                LogWarn("Failed to migrate offline stats (" + offlinePath.getFileName() + ") for player " + playername + " to online stats (" + onlinePath.getFileName() + "): " + e.getMessage());
                return original;
            }
            return onlinePath;
        }

        return original;
    }
    //?}


    //? if < 1.21.11 {
    /*@Inject(method = "createStatHandler(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/stat/ServerStatHandler;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private void migrateOfflineStats(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir, @Local UUID uUID, @Local ServerStatHandler serverStatHandler, @Local(ordinal = 0) File serverStatsDir) {
        File onlineFile = new File(serverStatsDir, uUID + ".json");
        if (server.isOnlineMode() && !extendedConfig.forcedOfflineUuid && ((PlayerAuth) player).easyAuth$isUsingMojangAccount() && !onlineFile.exists()) {
            String playername = getName(player.getGameProfile());
            File offlineFile = new File(onlineFile.getParent(), Uuids.getOfflinePlayerUuid(playername) + ".json");
            if (!offlineFile.exists()) {
                return;
            }
            if (!offlineFile.renameTo(onlineFile)) {
                LogWarn("Failed migrate offline stats (" + offlineFile.getName() + ") for player " + playername + " to online stats (" + onlineFile.getName() + ")");
                serverStatHandler.file = onlineFile;
            } else {
                LogDebug("Migrated offline stats (" + offlineFile.getName() + ") for player " + playername + " to online stats (" + onlineFile.getName() + ")");
            }
        }
    }
    *///?}

    //? if >= 1.21.9 {
    //?} else if >= 1.21.6 {
    
    /*@WrapOperation(method = "method_68176(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/storage/ReadView;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;readRootVehicle(Lnet/minecraft/storage/ReadView;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayerEntity instance, ReadView view, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, view);
    }
     
    *///?} else if >= 1.21.5 {
    /*@WrapOperation(method = "method_68176(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;readRootVehicle(Lnet/minecraft/nbt/NbtCompound;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayerEntity serverPlayerEntity, NbtCompound nbtCompound, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) serverPlayerEntity).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(serverPlayerEntity, nbtCompound);
    }
    *///?} else if >= 1.21.2 {
    /*@WrapOperation(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;readRootVehicle(Ljava/util/Optional;)V"))
    private void doNotMountPlayerToVehicle(ServerPlayerEntity instance, Optional<NbtCompound> nbt, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, nbt);
    }
    *///?} else {
    /*//? if >= 1.20.2 {
    @WrapOperation(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;loadEntityWithPassengers(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/World;Ljava/util/function/Function;)Lnet/minecraft/entity/Entity;"))
    //?} else {
    /^@WrapOperation(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;loadEntityWithPassengers(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/world/World;Ljava/util/function/Function;)Lnet/minecraft/entity/Entity;"))
    ^///?}
    private Entity onPlayerConnectStartRiding(NbtCompound nbt, World world, Function<Entity, Entity> entityProcessor, Operation<Entity> original, @Local(argsOnly = true) ServerPlayerEntity player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return null;
        }
        return original.call(nbt, world, entityProcessor);
    }
    *///?}

    @Unique
    private void onPlayerConnect(Args args, ServerPlayerEntity player, String username) {
        ((PlayerAuth) player).easyAuth$setSkipAuth();

        LogDebug(String.format("Teleporting player %s", username));
        LogDebug(String.format("Spawn position of player %s is %s", username, config.worldSpawn));

        args.set(0, config.worldSpawn.x);
        args.set(1, config.worldSpawn.y);
        args.set(2, config.worldSpawn.z);
        args.set(3, config.worldSpawn.yaw);
        args.set(4, config.worldSpawn.pitch);
    }
}
