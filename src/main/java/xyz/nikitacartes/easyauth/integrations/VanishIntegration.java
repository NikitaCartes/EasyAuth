package xyz.nikitacartes.easyauth.integrations;

import me.drex.vanish.api.VanishAPI;
import me.drex.vanish.util.VanishedEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class VanishIntegration {
    public static boolean isVanished(ServerPlayerEntity player) {
        return VanishAPI.isVanished(player);
    }

    public static void setVanished(ServerPlayerEntity player, boolean vanished) {
        VanishAPI.setVanish(player, vanished);
        ((VanishedEntity) player).vanish$setDirty();
    }
}
