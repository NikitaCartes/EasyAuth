package xyz.nikitacartes.easyauth.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Unique;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.util.UUID;

/**
 * PLayer authentication extension.
 */
public interface PlayerAuth {
    void easyAuth$saveTrueLocation();

    void easyAuth$saveTrueDimension(RegistryKey<World> registryKey);

    void easyAuth$restoreTrueLocation();

    /**
     * Sets the authentication status of the player.
     *
     * @param authenticated whether player should be authenticated
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void easyAuth$setAuthenticated(boolean authenticated);

    /**
     * Checks whether player is authenticated.
     *
     * @return false if player is not authenticated, otherwise true.
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean easyAuth$isAuthenticated();

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     * @return Text with appropriate string (login or register)
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void easyAuth$sendAuthMessage();

    /**
     * Checks whether player is a fake player (from CarpetMod).
     *
     * @return true if player is fake (can skip authentication process), otherwise false
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean easyAuth$canSkipAuth();

    void easyAuth$setSkipAuth();

    /**
     * Whether the player is using the mojang account
     *
     * @return true if paid, false if cracked
     */
    boolean easyAuth$isUsingMojangAccount();

    void easyAuth$setUsingMojangAccount();
    /**
     * Gets the player's IP address on connection step.
     *
     * @return player's IP address as string
     */
    String easyAuth$getIpAddress();

    /**
     * Sets the player's IP address on connection step.
     *
     * @param ClientConnection connection
     */
    void easyAuth$setIpAddress(ClientConnection connection);

    PlayerEntryV1 easyAuth$getPlayerEntryV1();
    void easyAuth$setPlayerEntryV1(PlayerEntryV1 playerEntryV1);
    void easyAuth$canSkipAuth(boolean cantSkipAuth);
    long easyAuth$getKickTimer();
    void easyAuth$setKickTimer(long kickTimer);
    void easyAuth$setIpAddress(String ipAddress);
    LastLocation easyAuth$getLastLocation();
    void easyAuth$setLastLocation(LastLocation lastLocation);
    NbtCompound easyAuth$getRootVehicle();
    void easyAuth$setRootVehicle(NbtCompound rootVehicle);
    UUID easyAuth$getRidingEntityUUID();
    void easyAuth$setRidingEntityUUID(UUID ridingEntityUUID);
    boolean easyAuth$wasDead();
    void easyAuth$wasDead(boolean wasDead);
    boolean easyAuth$wasVanished();
    void easyAuth$wasVanished(boolean wasVanished);
}
