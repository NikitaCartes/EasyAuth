package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKeys;
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
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.*;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PlayerAuth {
    @Unique
    private final ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
    @Final
    @Shadow
    public MinecraftServer server;
    // * 20 for 20 ticks in second
    @Unique
    private int kickTimer = config.main.kickTime * 20;

    /**
     * Teleports player to spawn or last location that is recorded.
     * Last location means the location before de-authentication.
     *
     * @param hide whether to teleport player to spawn (provided in config) or last recorded position
     */
    @Override
    public void easyAuth$hidePosition(boolean hide) {
        PlayerCache cache = playerCacheMap.get(this.easyAuth$getFakeUuid());
        LogDebug(String.format("Teleporting player %s to %s", player.getName().getContent(), hide ? "spawn." : "position."));
        if (hide) {
            // Saving position
            cache.lastLocation.dimension = player.getServerWorld();
            cache.lastLocation.position = player.getPos();
            cache.lastLocation.yaw = player.getYaw();
            cache.lastLocation.pitch = player.getPitch();
            cache.ridingEntityUUID = player.getVehicle() != null ? player.getVehicle().getUuid() : null;
            LogDebug(String.format("Saving position of player %s", cache.lastLocation));
            LogDebug(String.format("Saving vehicle of player %s", cache.ridingEntityUUID));

            // Teleports player to spawn
            player.teleport(
                    server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(config.worldSpawn.dimension))),
                    config.worldSpawn.x,
                    config.worldSpawn.y,
                    config.worldSpawn.z,
                    config.worldSpawn.yaw,
                    config.worldSpawn.pitch
            );
            return;
        }
        // Puts player to last cached position
        player.teleport(
                cache.lastLocation.dimension,
                cache.lastLocation.position.getX(),
                cache.lastLocation.position.getY(),
                cache.lastLocation.position.getZ(),
                cache.lastLocation.yaw,
                cache.lastLocation.pitch
        );
        LogDebug(String.format("Teleported player to %s", cache.lastLocation));
        // Mount player to vehicle if it exists
        if (cache.ridingEntityUUID != null) {
            LogDebug(String.format("Mounting player to vehicle %s", cache.ridingEntityUUID));
            ServerWorld world = server.getWorld(cache.lastLocation.dimension.getRegistryKey());
            if (world == null) return;
            Entity entity = world.getEntity(cache.ridingEntityUUID);
            if (entity != null) {
                player.startRiding(entity, true);
            } else {
                LogDebug("Could not find vehicle for player " + player.getName().getContent());
            }
        }

    }

    /**
     * Converts player uuid, to ensure player with "nAmE" and "NamE" get same uuid.
     * Both players are not allowed to play, since mod mimics Mojang behaviour.
     * of not allowing accounts with same names but different capitalization.
     *
     * @return converted UUID as string
     */
    @Override
    public String easyAuth$getFakeUuid() {
        // If server is in online mode online-mode UUIDs should be used
        assert server != null;
        if (server.isOnlineMode() && this.easyAuth$isUsingMojangAccount() && !config.experimental.forcedOfflineUuids)
            return player.getUuidAsString();
        /*
            Lower case is used for Player and PlAyEr to get same UUID (for password storing)
            Mimicking Mojang behaviour, where players cannot set their name to
            ExAmple if Example is already taken.
        */
        String playername = player.getGameProfile().getName().toLowerCase();
        return Uuids.getOfflinePlayerUuid(playername).toString();

    }

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     * @return Text with appropriate string (login or register)
     */
    @Override
    public Text easyAuth$getAuthMessage() {
        final PlayerCache cache = playerCacheMap.get(((PlayerAuth) player).easyAuth$getFakeUuid());
        if (!config.main.enableGlobalPassword && (cache == null || cache.password.isEmpty())) {
            if (config.experimental.enableServerSideTranslation) {
                return Text.translatable("text.easyauth.notAuthenticated").append("\n").append(Text.translatable("text.easyauth.registerRequired"));
            } else {
                return Text.of(config.lang.notAuthenticated + "\n" + config.lang.registerRequired);
            }
        } else {
            if (config.experimental.enableServerSideTranslation) {
                return Text.translatable("text.easyauth.notAuthenticated").append("\n").append(Text.translatable("text.easyauth.loginRequired"));
            } else {
                return Text.of(config.lang.notAuthenticated + "\n" + config.lang.loginRequired);
            }
        }


    }

    /**
     * Checks whether player can skip authentication process.
     *
     * @return true if player can skip authentication process, otherwise false
     */
    @Override
    public boolean easyAuth$canSkipAuth() {
        return (this.player.getClass() != ServerPlayerEntity.class) ||
                (config.main.floodgateAutologin && config.experimental.floodgateLoaded && FloodgateApiHelper.isFloodgatePlayer(this.player)) ||
                (easyAuth$isUsingMojangAccount() && config.main.premiumAutologin);
    }

    /**
     * Whether the player is using the mojang account.
     *
     * @return true if they are  using mojang account, otherwise false
     */
    @Override
    public boolean easyAuth$isUsingMojangAccount() {
        return server.isOnlineMode() && mojangAccountNamesCache.contains(player.getGameProfile().getName().toLowerCase());
    }

    /**
     * Checks whether player is authenticated.
     *
     * @return false if player is not authenticated, otherwise true.
     */
    @Override
    public boolean easyAuth$isAuthenticated() {
        String uuid = ((PlayerAuth) player).easyAuth$getFakeUuid();
        return this.easyAuth$canSkipAuth() || (playerCacheMap.containsKey(uuid) && playerCacheMap.get(uuid).isAuthenticated);
    }

    /**
     * Sets the authentication status of the player
     * and hides coordinates if needed.
     *
     * @param authenticated whether player should be authenticated
     */
    @Override
    public void easyAuth$setAuthenticated(boolean authenticated) {
        PlayerCache playerCache = playerCacheMap.get(this.easyAuth$getFakeUuid());
        playerCache.isAuthenticated = authenticated;

        player.setInvulnerable(!authenticated && config.experimental.playerInvulnerable);
        player.setInvisible(!authenticated && config.experimental.playerInvisible);

        // Teleporting player (hiding / restoring position)
        if (config.main.spawnOnJoin)
            this.easyAuth$hidePosition(!authenticated);

        if (authenticated) {
            kickTimer = config.main.kickTime * 20;
            // Updating blocks if needed (in case if portal rescue action happened)
            if (playerCache.wasInPortal) {
                World world = player.getEntityWorld();
                BlockPos pos = player.getBlockPos();

                // Sending updates to portal blocks
                // This is technically not needed, but it cleans the "messed portal" on the client
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                world.updateListeners(pos.up(), world.getBlockState(pos.up()), world.getBlockState(pos.up()), 3);
            }
        }
    }

    @Inject(method = "playerTick()V", at = @At("HEAD"), cancellable = true)
    private void playerTick(CallbackInfo ci) {
        if (!this.easyAuth$isAuthenticated()) {
            // Checking player timer
            if (kickTimer <= 0 && player.networkHandler.isConnectionOpen()) {
                player.networkHandler.disconnect(TranslationHelper.getTimeExpired());
            } else if (!playerCacheMap.containsKey(((PlayerAuth) player).easyAuth$getFakeUuid())) {
                player.networkHandler.disconnect(TranslationHelper.getAccountDeleted());
            } else {
                // Sending authentication prompt every 10 seconds
                if (kickTimer % 200 == 0)
                    player.sendMessage(this.easyAuth$getAuthMessage(), false);
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
}