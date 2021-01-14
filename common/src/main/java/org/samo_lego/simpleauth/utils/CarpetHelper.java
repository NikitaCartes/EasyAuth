package org.samo_lego.simpleauth.utils;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;

public class CarpetHelper {
    /**
     * Checks if player is actually a fake one.
     * Fake players are counted as ones, summoned with Carpet mod.
     *
     * @param player player to check
     * @return true if it's fake, otherwise false
     */
    public static boolean isPlayerCarpetFake(PlayerEntity player) {
        return player instanceof EntityPlayerMPFake;
    }
}
