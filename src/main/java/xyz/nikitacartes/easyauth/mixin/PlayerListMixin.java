//~ resource_location
package xyz.nikitacartes.easyauth.mixin;

//? if = 1.21.6 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
*///?}
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
//? if < 1.21 {
/*import net.minecraft.core.BlockPos;
*///?}
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
//? if >= 1.21.9 {
import net.minecraft.server.players.NameAndId;
//?}
import net.minecraft.server.players.PlayerList;
//? if >= 1.20.2 {
import net.minecraft.server.network.CommonListenerCookie;
//?}
//? if >= 1.20.2 && < 1.21.5 {
/*import java.util.Optional;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >= 1.21.6 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
//?}
//? if < 1.21.2 {
/*import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import java.util.function.Function;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if >= 1.21.2 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} else if >= 1.21 {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}
import net.minecraft.world.phys.Vec3;
//? if < 1.21.11 {
/*import net.minecraft.stats.ServerStatsCounter;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.PlayerDataMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if < 1.21.11 {
/*import java.io.File;
*///?}
import java.net.SocketAddress;
//? if < 1.21.11 {
/*import java.util.Optional;
import java.util.UUID;
*///?}
//? if < 1.21.2 {
/*import java.util.function.Function;
*///?}

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getName;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getId;
//? if < 1.21.9 {
/*import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getUsername;
*///?}

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Unique
    private final PlayerList playerManager = (PlayerList) (Object) this;

    @Final
    @Shadow
    private MinecraftServer server;

    //? if >= 1.21.6 {
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);

        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            //? if >= 1.21.9 {
            try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {

                playerManager.loadPlayerData(new NameAndId(player.getGameProfile())).flatMap(compound -> compound.getCompound("RootVehicle")).ifPresent(rootVehicle -> {
                    CompoundTag rootRootVehicle = new CompoundTag();
                    rootRootVehicle.put("RootVehicle", rootVehicle);
                    ValueInput readView = TagValueInput.create(logging, player.registryAccess(), rootRootVehicle);
                    ((PlayerAuth) player).easyAuth$setRootVehicle(readView);

                    rootVehicle.read("Attach", UUIDUtil.CODEC).ifPresent(uUID -> {
                        ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                        LogDebug(String.format("Saving vehicle of player %s as %s", player.getScoreboardName(), uUID));
                    });
                });
            }
            //?} else {
            /*try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
                playerManager.load(player, logging).ifPresent(valueInput -> valueInput.child("RootVehicle").ifPresent(rootVehicle -> {
                    ((PlayerAuth) player).easyAuth$setRootVehicle(valueInput);

                    rootVehicle.read("Attach", UUIDUtil.CODEC).ifPresent(uUID -> {
                        ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                        LogDebug(String.format("Saving vehicle of player %s as %s", player.getScoreboardName(), uUID));
                    });
                }));
            }
            *///?}
        }

        ((PlayerAuth) player).easyAuth$setSkipAuth();
    }
    //?} else if >= 1.20.2 {
    /*@Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);
    }
    *///?} else {
    /*@Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(Connection connection, ServerPlayer player, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);
    }
    *///?}

    //? if >= 1.21.9 {
    //?} else if = 1.21.6 {
    /*@ModifyExpressionValue(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;flatMap(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<ResourceKey<Level>> onPlayerConnect(Optional<ResourceKey<Level>> original, @Local(argsOnly = true) ServerPlayer player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(original.orElse(Level.OVERWORLD));
            return Optional.of(ResourceKey.create(Registries.DIMENSION, Identifier.parse(config.worldSpawn.dimension)));
        }
        return original;
    }
    *///?} else if >= 1.21 {
    /*@ModifyVariable(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("STORE"), ordinal = 0)
    private ResourceKey<Level> onPlayerConnect(ResourceKey<Level> world, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(config.worldSpawn.dimension));
        }
        return world;
    }
    *///?} else if >= 1.20.2 {
    /*@ModifyVariable(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
                at = @At("STORE"), ordinal = 0)
        private ResourceKey<Level> onPlayerConnect(ResourceKey<Level> world, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
            if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
                ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
                return ResourceKey.create(Registries.DIMENSION, new Identifier(config.worldSpawn.dimension));
            }
            return world;
        }
    *///?} else {
    /*@ModifyVariable(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("STORE"), ordinal = 0)
    private ResourceKey<Level> onPlayerConnect(ResourceKey<Level> world, Connection connection, ServerPlayer player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueDimension(world);
            return ResourceKey.create(Registries.DIMENSION, new Identifier(config.worldSpawn.dimension));
        }
        return world;
    }
    *///?}

    //? if >= 1.21.9 {
    //?} else if >= 1.21.6 {
    /*@ModifyArgs(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void onPlayerConnect(Args args, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = player.getScoreboardName();
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.21.5 {
    /*@ModifyArgs(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void onPlayerConnect(Args args, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = getUsername(player);
            playerManager.load(player).flatMap(compound -> compound.getCompound("RootVehicle")).ifPresent(rootVehicle -> {
                CompoundTag rootRootVehicle = new CompoundTag();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                rootVehicle.read("Attach", UUIDUtil.CODEC).ifPresent(uUID -> {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(uUID);
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, uUID));
                });
            });
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.20.5 {
    /*@ModifyArgs(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void onPlayerConnect(Args args, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = getUsername(player);

            Optional<CompoundTag> nbtCompound = playerManager.load(player);
            if(nbtCompound.isPresent() && nbtCompound.get().contains("RootVehicle", 10)) {
                CompoundTag rootVehicle = nbtCompound.get().getCompound("RootVehicle");
                CompoundTag rootRootVehicle = new CompoundTag();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.hasUUID("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUUID("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUUID("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else if >= 1.20.2 {
    /*@ModifyArgs(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void onPlayerConnect(Args args, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = getUsername(player);


            CompoundTag nbtCompound = playerManager.load(player);
            if(nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                CompoundTag rootVehicle = nbtCompound.getCompound("RootVehicle");
                CompoundTag rootRootVehicle = new CompoundTag();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.hasUUID("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUUID("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUUID("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?} else {
        /*@ModifyArgs(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void onPlayerConnect(Args args, Connection connection, ServerPlayer player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            ((PlayerAuth) player).easyAuth$saveTrueLocation();
            String username = getUsername(player);

            CompoundTag nbtCompound = playerManager.load(player);
            if(nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                CompoundTag rootVehicle = nbtCompound.getCompound("RootVehicle");
                CompoundTag rootRootVehicle = new CompoundTag();
                rootRootVehicle.put("RootVehicle", rootVehicle);
                ((PlayerAuth) player).easyAuth$setRootVehicle(rootRootVehicle);

                if (rootVehicle.hasUUID("Attach")) {
                    ((PlayerAuth) player).easyAuth$setRidingEntityUUID(rootVehicle.getUUID("Attach"));
                    LogDebug(String.format("Saving vehicle of player %s as %s", username, rootVehicle.getUUID("Attach")));
                }
            }
			onPlayerConnect(args, player, username);
        }
    }
    *///?}

    //? if >= 1.20.2 {
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(player);
    }
    //?} else {
    /*@Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(Connection connection, ServerPlayer serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(serverPlayerEntity);
    }
    *///?}

    //? if >=1.21.2 {
    @WrapOperation(method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private TeleportTransition replaceRespawnTarget(ServerPlayer instance, boolean alive, TeleportTransition.PostTeleportTransition postDimensionTransition, Operation<TeleportTransition> original) {
        if (alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            ResourceKey<Level> worldKey = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.parse(config.worldSpawn.dimension)
            );
            ServerLevel serverLevel = this.server.getLevel(worldKey);
            return new TeleportTransition(
                serverLevel != null ? serverLevel : this.server.overworld(),
                new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z),
                Vec3.ZERO, config.worldSpawn.yaw, config.worldSpawn.pitch, postDimensionTransition
            );
        }
        return original.call(instance, alive, postDimensionTransition);
    }
    //?} else if >=1.21 {
    /*@WrapOperation(method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition replaceRespawnTarget(ServerPlayer instance, boolean alive, DimensionTransition.PostDimensionTransition postDimensionTransition, Operation<DimensionTransition> original) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            ResourceKey<Level> worldKey = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.parse(config.worldSpawn.dimension)
            );
            ServerLevel serverLevel = this.server.getLevel(worldKey);
            return new DimensionTransition(
                serverLevel != null ? serverLevel : this.server.overworld(),
                new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z),
                Vec3.ZERO, config.worldSpawn.yaw, config.worldSpawn.pitch, postDimensionTransition
            );
        }
        return original.call(instance, alive, postDimensionTransition);
    }
    *///?} else {
    /*@WrapOperation(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;findRespawnPositionAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3> respawnPlayer(ServerLevel world, BlockPos pos, float angle, boolean forced, boolean alive, Operation<Optional<Vec3>> original, ServerPlayer player) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return Optional.of(new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z));
        }
        return original.call(world, pos, angle, forced, alive);
    }

    @WrapOperation(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getRespawnPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos respawnPlayerBlockPos(ServerPlayer instance, Operation<BlockPos> original, ServerPlayer player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return new BlockPos((int) config.worldSpawn.x, (int) config.worldSpawn.y, (int) config.worldSpawn.z);
        }
        return original.call(instance);
    }

    @WrapOperation(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getRespawnDimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> respawnPlayerDimension(ServerPlayer instance, Operation<ResourceKey<Level>> original, ServerPlayer player, boolean alive) {
        if (!alive && config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return ResourceKey.create(Registries.DIMENSION, new Identifier(config.worldSpawn.dimension));
        }
        return original.call(instance);
    }
    *///?}

    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayer serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerLeave(serverPlayerEntity);
    }

    //? if >= 1.21.9 {
    @Inject(method = "canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
    //?} else {
    /*@Inject(method = "canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
    *///?}
        // Before the ServerPlayer is built (its constructor loads stats/advancements), move any
        // offline-UUID world data onto the UUID this player is actually joining with.
        PlayerDataMigration.migrateOnJoin(server, getName(profile), getId(profile));

        // Getting the player that is trying to join the server
        Component returnText = AuthEventHandler.checkCanPlayerJoinServer(profile, playerManager, address);

        if (returnText != null) {
            // Canceling player joining with the returnText message
            cir.setReturnValue(returnText);
        }
    }

    //? if >= 1.21.9 {
    //?} else if >= 1.21.6 {
    /*@WrapOperation(method = "method_68176(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;loadAndSpawnParentVehicle(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayer instance, ValueInput view, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, view);
    }
    *///?} else if >= 1.21.5 {
    /*@WrapOperation(method = "method_68176(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;loadAndSpawnParentVehicle(Lnet/minecraft/nbt/CompoundTag;)V"))
    private static void doNotMountPlayerToVehicle(ServerPlayer serverPlayer, CompoundTag compoundTag, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) serverPlayer).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(serverPlayer, compoundTag);
    }
    *///?} else if >= 1.21.2 {
    /*@WrapOperation(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;loadAndSpawnParentVehicle(Ljava/util/Optional;)V"))
    private void doNotMountPlayerToVehicle(ServerPlayer instance, Optional<CompoundTag> nbt, Operation<Void> original) {
        if (config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return;
        }
        original.call(instance, nbt);
    }
    *///?} else {
    /*//? if >= 1.20.2 {
    @WrapOperation(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Ljava/util/function/Function;)Lnet/minecraft/world/entity/Entity;"))
    //?} else {
    /^@WrapOperation(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Ljava/util/function/Function;)Lnet/minecraft/world/entity/Entity;"))
    ^///?}
    private Entity onPlayerConnectStartRiding(CompoundTag nbt, Level world, Function<Entity, Entity> entityProcessor, Operation<Entity> original, @Local(argsOnly = true) ServerPlayer player) {
        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return null;
        }
        return original.call(nbt, world, entityProcessor);
    }
    *///?}

    @Unique
    private void onPlayerConnect(Args args, ServerPlayer player, String username) {
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
