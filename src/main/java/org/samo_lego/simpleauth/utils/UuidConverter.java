package org.samo_lego.simpleauth.utils;

import net.minecraft.entity.player.PlayerEntity;

import static org.samo_lego.simpleauth.SimpleAuth.serverProp;

/**
 * Converts player uuid, to ensure player with "nAmE" and "NamE" get same uuid
 * Both players are not allowed to play, since mod mimics Mojang behaviour
 * of not allowing accounts with same names but different capitalization
 */
public class UuidConverter {

    private static final boolean isOnline = Boolean.parseBoolean(serverProp.getProperty("online-mode"));

    /** Gets player UUID.
     *
     * @param player player to get UUID for
     * @return converted UUID as string
     */
    public static String convertUuid(PlayerEntity player) {
        // If server is in online mode online-mode UUIDs should be used
        if(isOnline)
            return player.getUuidAsString();
        /*
            Lower case is used for Player and PlAyEr to get same UUID (for password storing)
            Mimicking Mojang behaviour, where players cannot set their name to
            ExAmple if Example is already taken.
        */
        String playername = player.getName().asString().toLowerCase();
        return PlayerEntity.getOfflinePlayerUuid(playername).toString();
    }
}
