package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
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

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Unique
    private final PlayerManager playerManager = (PlayerManager) (Object) this;

    @Final
    @Shadow
    private MinecraftServer server;

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

    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V", at = @At("RETURN"))
    private void onPlayerConnectReturn(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        AuthEventHandler.onPlayerJoin(player);
    }

    @WrapOperation(method = "respawnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getRespawnTarget(ZLnet/minecraft/world/TeleportTarget$PostDimensionTransition;)Lnet/minecraft/world/TeleportTarget;"))
    private TeleportTarget replaceRespawnTarget(ServerPlayerEntity instance, boolean alive, TeleportTarget.PostDimensionTransition postDimensionTransition, Operation<TeleportTarget> original) {
        if (alive && config.hidePlayerCoords && !((PlayerAuth) instance).easyAuth$isAuthenticated()) {
            return new TeleportTarget(
                this.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.worldSpawn.dimension))),
                new Vec3d(config.worldSpawn.x, config.worldSpawn.y, config.worldSpawn.z),
                new Vec3d(0.0F, 0.0F, 0.0F), config.worldSpawn.yaw, config.worldSpawn.pitch, postDimensionTransition
            );
        }
        return original.call(instance, alive, postDimensionTransition);
    }

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        AuthEventHandler.onPlayerLeave(serverPlayerEntity);
    }

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("RETURN"))
    private void onPlayerLeaveUnVanish(ServerPlayerEntity player, CallbackInfo ci) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        if (playerAuth.easyAuth$canSkipAuth() || playerAuth.easyAuth$isAuthenticated()) {
            return;
        }
        if (config.vanishUntilAuth) {
            VanishIntegration.setVanished(player, playerAuth.easyAuth$wasVanished());
        }
    }

    @Inject(method = "checkCanJoin(Ljava/net/SocketAddress;Lnet/minecraft/server/PlayerConfigEntry;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, PlayerConfigEntry profile, CallbackInfoReturnable<Text> cir) {
        // Getting the player that is trying to join the server
        Text returnText = AuthEventHandler.checkCanPlayerJoinServer(profile, playerManager, address);

        if (returnText != null) {
            // Canceling player joining with the returnText message
            cir.setReturnValue(returnText);
        }
    }

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

}
