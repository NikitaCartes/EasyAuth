package xyz.nikitacartes.easyauth.mixin;

import com.google.common.net.InetAddresses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
//? if < 1.21.6 {
/*import net.minecraft.nbt.CompoundTag;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >= 1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
//?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if < 1.21.11 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.*;
import static xyz.nikitacartes.easyauth.utils.Utils.getIp;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends EntityMixin implements PlayerAuth {
    @Unique
    private final ServerPlayer player = (ServerPlayer) (Object) this;

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
    //? if >= 1.21.6 {
    private ValueInput rootVehicle = null;
    //?} else {
    /*private CompoundTag rootVehicle = null;
    *///?}

    @Unique
    private boolean wasDead = false;

    @Unique
    PlayerEntryV1 playerEntryV1 = new PlayerEntryV1(getUsername(player));

    @Unique
    private boolean canSkipAuth = this.player.getClass() != ServerPlayer.class;

    @Unique
    private volatile boolean isAuthenticated = this.player.getClass() != ServerPlayer.class;

    @Unique
    private boolean isUsingMojangAccount = false;

    @Unique
    private boolean wasVanished = false;

    @Unique
    private boolean dialogShown = false;

    @Override
    public void easyAuth$saveTrueLocation() {
        if (lastLocation == null) {
            lastLocation = new LastLocation();
        }
        lastLocation.position = getPosition(player);
        lastLocation.yaw = player.getYRot();
        lastLocation.pitch = player.getXRot();

        ridingEntityUUID = player.getVehicle() != null ? player.getVehicle().getUUID() : null;
        wasDead = player.isDeadOrDying();
        String username = getUsername(player);
        LogDebug(String.format("Saving position of player %s as %s", username, lastLocation));
        if (ridingEntityUUID != null) {
            LogDebug(String.format("Saving vehicle of player %s as %s", username, ridingEntityUUID));
        }
    }

    @Override
    public void easyAuth$saveTrueDimension(ResourceKey<Level> registryKey) {
        if (lastLocation == null) {
            lastLocation = new LastLocation();
        }
        lastLocation.dimension = registryKey;
    }

    @Override
    public void easyAuth$restoreTrueLocation() {
        if (lastLocation == null) {
            return;
        }
        if (wasDead) {
            StoneCutterUtils.killPlayer(player);
            return;
        }
        // Puts player to last saved position
        teleport(player, lastLocation, server.getLevel(Level.OVERWORLD));
        String username = getUsername(player);
        LogDebug(String.format("Teleported player %s to %s", username, lastLocation));

        if (rootVehicle != null) {
            LogDebug(String.format("Mounting player to vehicle %s", rootVehicle));
            readRootVehicle(player, rootVehicle);
        }

        if (player.getVehicle() == null && ridingEntityUUID != null) {
            LogDebug(String.format("Mounting player to vehicle %s", ridingEntityUUID));
            if (lastLocation.dimension == null) return;
            ServerLevel world = server.getLevel(lastLocation.dimension);
            if (world == null) return;
            Entity entity = world.getEntity(ridingEntityUUID);
            if (entity != null) {
                startRiding(player, entity);
            } else {
                LogDebug("Could not find vehicle for player " + username);
            }
        }
    }

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     */
    @Override
    public void easyAuth$sendAuthMessage() {
        //? if >= 1.21.6 {
        if (dialogConfig.enabled) {
            // Open the window once; reopening on the prompt timer would wipe what the player typed.
            if (!dialogShown && xyz.nikitacartes.easyauth.dialog.AuthDialogs.openAuthPrompt(player)) {
                dialogShown = true;
            }
            if (dialogShown) {
                return;
            }
        }
        //?}
        if (playerEntryV1 != null && !playerEntryV1.password.isEmpty()) {
            langConfig.session.loginRequired.send(player);
            return;
        }
        if (!config.enableGlobalPassword) {
            langConfig.registration.required.send(player);
            return;
        }
        if (config.singleUseGlobalPassword) {
            langConfig.registration.requiredWithGlobalPassword.send(player);
            return;
        }
        langConfig.session.loginRequired.send(player);
    }

    /**
     * Checks whether player can skip an authentication process (Online Player or Fake one).
     *
     * @return true if a player can skip an authentication process, otherwise false
     */
    @Override
    public boolean easyAuth$canSkipAuth() {
        return canSkipAuth;
    }

    @Override
    public void easyAuth$setSkipAuth() {
        easyAuth$setUsingMojangAccount();
        canSkipAuth = (this.player.getClass() != ServerPlayer.class) ||
                (config.floodgateAutoLogin && FloodgateApiHelper.isFloodgatePlayer(this.player)) ||
                (config.premiumAutoLogin && easyAuth$isUsingMojangAccount());
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
        isUsingMojangAccount = server.usesAuthentication() && playerEntryV1.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE;
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
            ServerLevel world = StoneCutterUtils.getServerWorld(player);
            BlockPos pos = player.blockPosition();

            // Sending updates to portal blocks
            // This is technically not needed, but it cleans the "messed portal" on the client
            if (!world.isOutsideBuildHeight(pos)) {
                world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
            if (!world.isOutsideBuildHeight(pos.above())) {
                world.sendBlockUpdated(pos.above(), world.getBlockState(pos.above()), world.getBlockState(pos.above()), 3);
            }

            player.containerMenu.broadcastFullState();

            VanishIntegration.setVanished(player, wasVanished);
        } else {
            // Re-prompt with a fresh window next time (e.g. after /logout or an admin removing the account).
            dialogShown = false;
            if (config.vanishUntilAuth) {
                wasVanished = VanishIntegration.isVanished(player);
                VanishIntegration.setVanished(player, true);
            }
        }
    }

    @Inject(method = "doTick()V", at = @At("HEAD"), cancellable = true)
    private void playerTick(CallbackInfo ci) {
        if (!this.easyAuth$isAuthenticated()) {
            // Checking player timer
            if (kickTimer <= 0 && player.connection.isAcceptingMessages()) {
                player.connection.disconnect(langConfig.session.timeExpired.get());
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
    //? if >= 1.21.11 {
    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean entireStack, CallbackInfo ci) {
        InteractionResult result = AuthEventHandler.onDropItem(player);

        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        InteractionResult result = AuthEventHandler.onDropItem(player);

        if (result == InteractionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }
    *///?}

    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("RETURN"))
    private void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
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

    //? if >= 1.21.6 {
    public ValueInput easyAuth$getRootVehicle() {
        return rootVehicle;
    }

    public void easyAuth$setRootVehicle(ValueInput rootVehicle) {
        this.rootVehicle = rootVehicle;
    }
    //?} else {
    /*public CompoundTag easyAuth$getRootVehicle() {
        return rootVehicle;
    }

    public void easyAuth$setRootVehicle(CompoundTag rootVehicle) {
        this.rootVehicle = rootVehicle;
    }
    *///?}

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

    public void easyAuth$setIpAddress(Connection connection) {
        ipAddress = getIp(connection.getRemoteAddress());
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

