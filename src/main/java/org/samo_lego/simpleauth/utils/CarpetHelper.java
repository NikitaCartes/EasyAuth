package org.samo_lego.simpleauth.utils;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;

public class CarpetHelper {
    // Checking if player is actually a fake one
    // This is in its own class since we need carpet classes
    public static boolean isPlayerCarpetFake(PlayerEntity player) {
        return player instanceof EntityPlayerMPFake;
    }
}
