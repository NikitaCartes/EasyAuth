package xyz.nikitacartes.easyauth.integrations;

import net.minecraft.entity.player.PlayerEntity;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

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
        if (!technicalConfig.floodgateLoaded) return false;
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        return floodgateApi.isFloodgatePlayer(uuid);
    }
}