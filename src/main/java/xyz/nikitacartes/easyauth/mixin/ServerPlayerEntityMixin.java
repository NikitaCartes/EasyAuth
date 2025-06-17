package xyz.nikitacartes.easyauth.mixin;

import com.google.common.net.InetAddresses;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.EnumSet;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends EntityMixin implements PlayerAuth {
    @Unique
    private final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

    @Final
    @Shadow
    public MinecraftServer server;

    @Unique
    private long kickTimer = config.kickTimeout * 20;

    @Unique
    private String ipAddress = null;

    @Unique
    private LastLocation lastLocation = null;

    @Unique
    private UUID ridingEntityUUID = null;

    @Unique
    private NbtCompound rootVehicle = null;

    @Unique
    private boolean wasDead = false;

    @Unique
    PlayerEntryV1 playerEntryV1 = new PlayerEntryV1(player.getNameForScoreboard());

    @Unique
    private boolean canSkipAuth = this.player.getClass() != ServerPlayerEntity.class;

    @Unique
    private boolean isAuthenticated = this.player.getClass() != ServerPlayerEntity.class;

    @Unique
    private boolean isUsingMojangAccount = false;

    @Unique
    private boolean wasVanished = false;

    @Override
    public void easyAuth$saveTrueLocation() {
        if (lastLocation == null) {
            lastLocation = new LastLocation();
        }
        lastLocation.position = player.getPos();
        lastLocation.yaw = player.getYaw();
        lastLocation.pitch = player.getPitch();

        ridingEntityUUID = player.getVehicle() != null ? player.getVehicle().getUuid() : null;
        wasDead = player.isDead();
        LogDebug(String.format("Saving position of player %s as %s", player.getNameForScoreboard(), lastLocation));
        if (ridingEntityUUID != null) {
            LogDebug(String.format("Saving vehicle of player %s as %s", player.getNameForScoreboard(), ridingEntityUUID));
        }
    }

    @Override
    public void easyAuth$saveTrueDimension(RegistryKey<World> registryKey) {
        if (lastLocation == null) {
            lastLocation = new LastLocation();
        }
        lastLocation.dimension = this.server.getWorld(registryKey);
    }

    @Override
    public void easyAuth$restoreTrueLocation() {
        if (lastLocation == null) {
            return;
        }
        if (wasDead) {
            player.kill(player.getServerWorld());
            player.getScoreboard().forEachScore(ScoreboardCriterion.DEATH_COUNT, player, (score) -> score.setScore(score.getScore() - 1));
            return;
        }
        // Puts player to last saved position
        player.teleport(
                lastLocation.dimension == null ? server.getWorld(World.OVERWORLD) : lastLocation.dimension,
                lastLocation.position.getX(),
                lastLocation.position.getY(),
                lastLocation.position.getZ(),
                EnumSet.noneOf(PositionFlag.class),
                lastLocation.yaw,
                lastLocation.pitch,
                true);
        LogDebug(String.format("Teleported player %s to %s", player.getNameForScoreboard(), lastLocation));

        if (rootVehicle != null) {
            LogDebug(String.format("Mounting player to vehicle %s", rootVehicle));
            player.readRootVehicle(rootVehicle);
        }

        if (player.getVehicle() == null && ridingEntityUUID != null) {
            LogDebug(String.format("Mounting player to vehicle %s", ridingEntityUUID));
            if (lastLocation.dimension == null) return;
            ServerWorld world = server.getWorld(lastLocation.dimension.getRegistryKey());
            if (world == null) return;
            Entity entity = world.getEntity(ridingEntityUUID);
            if (entity != null) {
                player.startRiding(entity, true);
            } else {
                LogDebug("Could not find vehicle for player " + player.getNameForScoreboard());
            }
        }
    }

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     * @return Text with appropriate string (login or register)
     */
    @Override
    public void easyAuth$sendAuthMessage() {
        if ((!config.enableGlobalPassword || config.singleUseGlobalPassword) && (playerEntryV1 == null || playerEntryV1.password.isEmpty())) {
            if (config.singleUseGlobalPassword) {
                langConfig.registerRequiredWithGlobalPassword.send(player);
            } else {
                langConfig.registerRequired.send(player);
            }
        } else {
            langConfig.loginRequired.send(player);
        }
    }

    /**
     * Checks whether player can skip authentication process.
     *
     * @return true if player can skip authentication process, otherwise false
     */
    @Override
    public boolean easyAuth$canSkipAuth() {
        return canSkipAuth;
    }

    @Override
    public void easyAuth$setSkipAuth() {
        easyAuth$setUsingMojangAccount();
        canSkipAuth = (this.player.getClass() != ServerPlayerEntity.class) ||
                (config.floodgateAutoLogin && technicalConfig.floodgateLoaded && FloodgateApiHelper.isFloodgatePlayer(this.player)) ||
                (easyAuth$isUsingMojangAccount() && config.premiumAutoLogin);
    }

    /**
     * Whether the player is using the mojang account.
     *
     * @return true if they are  using mojang account, otherwise false
     */
    @Override
    public boolean easyAuth$isUsingMojangAccount() {
        return isUsingMojangAccount;
    }

    @Override
    public void easyAuth$setUsingMojangAccount() {
        isUsingMojangAccount = server.isOnlineMode() && playerEntryV1.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE;
    }

    /**
     * Checks whether player is authenticated.
     *
     * @return false if player is not authenticated, otherwise true.
     */
    @Override
    public boolean easyAuth$isAuthenticated() {
        return isAuthenticated;
    }

    /**
     * Sets the authentication status of the player
     *
     * @param authenticated whether player should be authenticated
     */
    @Override
    public void easyAuth$setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;

        if (authenticated) {
            kickTimer = config.kickTimeout * 20;
            // Updating blocks if needed (in case if portal rescue action happened)
            World world = player.getEntityWorld();
            BlockPos pos = player.getBlockPos();

            // Sending updates to portal blocks
            // This is technically not needed, but it cleans the "messed portal" on the client
            if (world.isInBuildLimit(pos)) {
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
            if (world.isInBuildLimit(pos.up())) {
                world.updateListeners(pos.up(), world.getBlockState(pos.up()), world.getBlockState(pos.up()), 3);
            }

            player.currentScreenHandler.syncState();

            if (!wasVanished && technicalConfig.vanishLoaded) {
                VanishIntegration.setVanished(player, false);
            }
        } else {
            if (config.vanishUntilAuth && technicalConfig.vanishLoaded) {
                wasVanished = VanishIntegration.isVanished(player);
                VanishIntegration.setVanished(player, true);
            }
        }
    }

    @Inject(method = "playerTick()V", at = @At("HEAD"), cancellable = true)
    private void playerTick(CallbackInfo ci) {
        if (!this.easyAuth$isAuthenticated()) {
            // Checking player timer
            if (kickTimer <= 0 && player.networkHandler.isConnectionOpen()) {
                player.networkHandler.disconnect(langConfig.timeExpired.get());
            } else {
                // Sending authentication prompt every 10 seconds
                if (kickTimer % (extendedConfig.authenticationPromptInterval * 20) == 0) {
                    this.easyAuth$sendAuthMessage();
                }
                --kickTimer;
            }
            ci.cancel();
        }
    }

    // Player item dropping
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        ActionResult result = AuthEventHandler.onDropItem(player);

        if (result == ActionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V", at = @At("RETURN"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerAuth oldPlayerAuth = (PlayerAuth) oldPlayer;
        PlayerAuth newPlayerAuth = (PlayerAuth) player;
        newPlayerAuth.easyAuth$setKickTimer(oldPlayerAuth.easyAuth$getKickTimer());
        newPlayerAuth.easyAuth$setIpAddress(oldPlayerAuth.easyAuth$getIpAddress());
        newPlayerAuth.easyAuth$setLastLocation(oldPlayerAuth.easyAuth$getLastLocation());
        newPlayerAuth.easyAuth$setRidingEntityUUID(oldPlayerAuth.easyAuth$getRidingEntityUUID());
        newPlayerAuth.easyAuth$setRootVehicle(oldPlayerAuth.easyAuth$getRootVehicle());
        newPlayerAuth.easyAuth$wasDead(oldPlayerAuth.easyAuth$wasDead());
        newPlayerAuth.easyAuth$canSkipAuth(oldPlayerAuth.easyAuth$canSkipAuth());
        newPlayerAuth.easyAuth$setAuthenticated(oldPlayerAuth.easyAuth$isAuthenticated());

        newPlayerAuth.easyAuth$setPlayerEntryV1(oldPlayerAuth.easyAuth$getPlayerEntryV1());
    }

    @Override
    public boolean easyAuth$isInvisible(boolean original) {
        return original || (!isAuthenticated && extendedConfig.playerIgnored);
    }

    @Override
    public boolean easyAuth$isInvulnerable(boolean original) {
        return original || (!isAuthenticated && extendedConfig.playerInvulnerable);
    }

    public long easyAuth$getKickTimer() {
        return kickTimer;
    }

    public void easyAuth$setKickTimer(long kickTimer) {
        this.kickTimer = kickTimer;
    }

    public void easyAuth$setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LastLocation easyAuth$getLastLocation() {
        return lastLocation;
    }

    public void easyAuth$setLastLocation(LastLocation lastLocation) {
        this.lastLocation = lastLocation;
    }

    public UUID easyAuth$getRidingEntityUUID() {
        return ridingEntityUUID;
    }

    public void easyAuth$setRidingEntityUUID(UUID ridingEntityUUID) {
        this.ridingEntityUUID = ridingEntityUUID;
    }

    public NbtCompound easyAuth$getRootVehicle() {
        return rootVehicle;
    }

    public void easyAuth$setRootVehicle(NbtCompound rootVehicle) {
        this.rootVehicle = rootVehicle;
    }

    public boolean easyAuth$wasDead() {
        return wasDead;
    }

    public void easyAuth$wasDead(boolean wasDead) {
        this.wasDead = wasDead;
    }

    public void easyAuth$canSkipAuth(boolean cantSkipAuth) {
        this.canSkipAuth = cantSkipAuth;
    }

    public String easyAuth$getIpAddress() {
        return ipAddress;
    }

    public void easyAuth$setIpAddress(ClientConnection connection) {
        SocketAddress socketAddress = connection.getAddress();
        ipAddress = socketAddress instanceof InetSocketAddress inetSocketAddress ? InetAddresses.toAddrString(inetSocketAddress.getAddress()) : "<unknown>";
    }

    public PlayerEntryV1 easyAuth$getPlayerEntryV1() {
        return playerEntryV1;
    }

    public void easyAuth$setPlayerEntryV1(PlayerEntryV1 playerEntryV1) {
        this.playerEntryV1 = playerEntryV1;
    }

    public boolean easyAuth$wasVanished() {
        return wasVanished;
    }

    public void easyAuth$wasVanished(boolean wasVanished) {
        this.wasVanished = wasVanished;
    }

}

