package xyz.nikitacartes.easyauth.integrations;

import me.drex.vanish.api.VanishAPI;
import me.drex.vanish.util.VanishedEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

public class VanishIntegration {
    public static boolean isVanished(ServerPlayerEntity player) {
        if (!technicalConfig.vanishLoaded) return false;
        return VanishAPI.isVanished(player);
    }

    public static void setVanished(ServerPlayerEntity player, boolean vanished) {
        if (!technicalConfig.vanishLoaded) return;
        VanishAPI.setVanish(player, vanished);
        ((VanishedEntity) player).vanish$setDirty();
    }
}
