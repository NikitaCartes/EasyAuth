package xyz.nikitacartes.easyauth.integrations;

import me.drex.vanish.api.VanishAPI;
//? if != 1.20.2 || < 1.20 {
import me.drex.vanish.util.VanishedEntity;
//?}
import net.minecraft.server.level.ServerPlayer;

import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

public class VanishIntegration {
    public static boolean isVanished(ServerPlayer player) {
        if (!technicalConfig.vanishLoaded) return false;
        return VanishAPI.isVanished(player);
    }

    public static void setVanished(ServerPlayer player, boolean vanished) {
        if (!technicalConfig.vanishLoaded) return;
        VanishAPI.setVanish(player, vanished);
        //? if != 1.20.2 || < 1.20 {
        ((VanishedEntity) player).vanish$setDirty();
        //?}
    }
}
