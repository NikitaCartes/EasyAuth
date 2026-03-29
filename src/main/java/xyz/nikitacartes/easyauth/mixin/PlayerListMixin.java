package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.resources.Identifier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import org.spongepowered.asm.mixin.Mixin;

import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getName;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Unique
    private final PlayerList playerManager = (PlayerList) (Object) this;

    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"))
    private void onPlayerConnectHead(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        AuthEventHandler.loadPlayerData(player, connection);

        if (config.hidePlayerCoords && !((PlayerAuth) player).easyAuth$isAuthenticated()) {
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
        }

        ((PlayerAuth) player).easyAuth$setSkipAuth();
    }

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(player);
    }

    @WrapOperation(method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private TeleportTransition replaceRespawnTarget(ServerPlayer instance, boolean alive, TeleportTransition.PostTeleportTransition postDimensionTransition, Operation<TeleportTransition> original) {
        if (alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return new TeleportTransition(
                this.server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(config.worldSpawn.dimension))),
                new Vec3(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z),
                new Vec3(0.0F, 0.0F, 0.0F), config.worldSpawn.yaw, config.worldSpawn.pitch, postDimensionTransition
            );
        }
        return original.call(instance, alive, postDimensionTransition);
    }

    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayer serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerLeave(serverPlayerEntity);
    }

    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("RETURN"))
    private void onPlayerLeaveUnVanish(ServerPlayer player, CallbackInfo ci) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        if (playerAuth.easyAuth$canSkipAuth() || playerAuth.easyAuth$isAuthenticated()) {
            return;
        }
        if (config.vanishUntilAuth) {
            VanishIntegration.setVanished(player, playerAuth.easyAuth$wasVanished());
        }
    }

    @Inject(method = "canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
        // Getting the player that is trying to join the server
        Component returnText = AuthEventHandler.checkCanPlayerJoinServer(profile, playerManager, address);

        if (returnText != null) {
            // Canceling player joining with the returnText message
            cir.setReturnValue(returnText);
        }
    }

    @ModifyReturnValue(method = "locateStatsFile(Lcom/mojang/authlib/GameProfile;)Ljava/nio/file/Path;",
            at = @At("RETURN")
    )
    private Path migrateOfflineStats(Path original, @Local(ordinal = 0) Path parentPath, @Local(ordinal = 1) Path onlinePath, @Local(argsOnly = true) GameProfile profile) {
        if (!server.usesAuthentication() || extendedConfig.forcedOfflineUuid || Files.exists(onlinePath)) {
            return original;
        }

        Player player = server.getPlayerList().getPlayer(profile.id());
        if (player != null && ((PlayerAuth) player).easyAuth$isUsingMojangAccount()) {
            String playername = getName(profile);
            Path offlinePath = parentPath.resolve(UUIDUtil.createOfflinePlayerUUID(playername) + ".json");
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

}
