package xyz.nikitacartes.easyauth.utils;

import net.minecraft.entity.player.PlayerEntity;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateApiHelper{
    /**
     * Checks if player is a floodgate one.
     *
     * @param player player to check
     * @return true if it's fake, otherwise false
     */

    public static boolean isFloodgatePlayer(PlayerEntity player) {
        return isFloodgatePlayer(player.getUuid());
    }

    /**
     * Checks if player is a floodgate one.
     *
     * @param uuid player's uuid to check
     * @return true if it's fake, otherwise false
     */

    public static boolean isFloodgatePlayer(UUID uuid) {
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        return floodgateApi.isFloodgatePlayer(uuid);
    }
}