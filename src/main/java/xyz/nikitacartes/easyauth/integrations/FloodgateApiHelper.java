package xyz.nikitacartes.easyauth.integrations;

import net.minecraft.world.entity.player.Player;
//? if fabric {
import org.geysermc.floodgate.api.FloodgateApi;
//?}

import java.util.UUID;

//? if fabric {
import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;
//?}

public class FloodgateApiHelper{
    /**
     * Checks if player is a floodgate one.
     *
     * @param player player to check
     * @return true if it's fake, otherwise false
     */

    public static boolean isFloodgatePlayer(Player player) {
        return isFloodgatePlayer(player.getUUID());
    }

    /**
     * Checks if player is a floodgate one.
     *
     * @param uuid player's uuid to check
     * @return true if it's fake, otherwise false
     */

    public static boolean isFloodgatePlayer(UUID uuid) {
        //? if neoforge {
        /*// Floodgate has no NeoForge port; always false
        return false;
        *///?} else {
        if (!technicalConfig.floodgateLoaded) return false;
        FloodgateApi floodgateApi = FloodgateApi.getInstance();
        return floodgateApi.isFloodgatePlayer(uuid);
        //?}
    }
}
