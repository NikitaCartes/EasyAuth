package xyz.nikitacartes.easyauth.utils;

import dan200.computercraft.api.turtle.FakePlayer;
import net.minecraft.entity.player.PlayerEntity;

public class ComputerCraftHelper {
    /**
     * Checks if player is actually a fake one.
     *
     * @param player player to check
     * @return true if it's fake, otherwise false
     */
    public static boolean isPlayerFake(PlayerEntity player) {
        return player instanceof FakePlayer;
    }
}
