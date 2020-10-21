package org.samo_lego.simpleauth.utils;

import net.minecraft.text.Text;

/**
 * PLayer authentication extension.
 */
public interface PlayerAuth {
    /**
     * Teleports player to spawn or last location that is recorded.
     * Last location means the location before de-authentication.
     *
     * @param hide whether to teleport player to spawn (provided in config) or last recorded position
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void hidePosition(boolean hide);

    /**
     * Converts player uuid, to ensure player with "nAmE" and "NamE" get same uuid.
     * Both players are not allowed to play, since mod mimics Mojang behaviour.
     * of not allowing accounts with same names but different capitalization.
     *
     * @return converted UUID as string
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    String getFakeUuid();


    /**
     * Sets the authentication status of the player.
     *
     * @param authenticated whether player should be authenticated
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    void setAuthenticated(boolean authenticated);

    /**
     * Checks whether player is authenticated.
     *
     * @return false if player is not authenticated, otherwise true.
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean isAuthenticated();

    /**
     * Gets the text which tells the player
     * to login or register, depending on account status.
     *
     * @return LiteralText with appropriate string (login or register)
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    Text getAuthMessage();

    /**
     * Checks whether player is a fake player (from CarpetMod).
     *
     * @return true if player is fake (can skip authentication process), otherwise false
     * @see <a href="https://samolego.github.io/SimpleAuth/org/samo_lego/simpleauth/mixin/MixinPlayerEntity.html">See implementation</a>
     */
    boolean canSkipAuth();

    /**
     * Whether the player is using the mojang account
     * @return true if paid, false if cracked
     */
    boolean isUsingMojangAccount();
}
