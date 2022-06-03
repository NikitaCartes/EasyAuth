package xyz.nikitacartes.easyauth.utils;

import dev.cafeteria.fakeplayerapi.server.FakeServerPlayer;
import net.minecraft.entity.player.PlayerEntity;

public class FakePlayerApiHelper {
    /**
     * Checks if player is actually a fake one.
     *
     * @param player player to check
     * @return true if it's fake, otherwise false
     */
    public static boolean isPlayerFake(PlayerEntity player) {
        return player instanceof FakeServerPlayer;
    }
}
