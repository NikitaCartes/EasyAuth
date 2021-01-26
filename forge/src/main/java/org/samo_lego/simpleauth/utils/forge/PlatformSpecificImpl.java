package org.samo_lego.simpleauth.utils.forge;

import net.minecraft.entity.player.PlayerEntity;

public class PlatformSpecificImpl {
    public static boolean isPlayerFake(PlayerEntity player) {
        // Are there any forge mods with NPCs that don't work with SimpleAuth?
        // Let me know!
        return false;
    }
}
