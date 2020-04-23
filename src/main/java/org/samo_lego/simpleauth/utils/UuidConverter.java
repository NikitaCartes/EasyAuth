package org.samo_lego.simpleauth.utils;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Converts player uuid, to ensure player with "nAmE" and "NamE" get same uuid
 * Both players are not allowed to play, since mod mimics Mojang behaviour
 * of not allowing accounts with same names but different capitalization
 */
public class UuidConverter {

    /** Converts player UUID to offline mode style.
     *
     * @param playername name of the player to get UUID for
     * @return converted UUID as string
     */
    public static String convertUuid(String playername) {
        return PlayerEntity.getOfflinePlayerUuid(playername).toString();
    }

    /** Converts player UUID to offline mode style.
     *
     * @param player player to get UUID for
     * @return converted UUID as string
     */
    public static String convertUuid(PlayerEntity player) {
        return convertUuid(player.getName().asString().toLowerCase());
    }
}
