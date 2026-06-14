package xyz.nikitacartes.easyauth.integrations;

//? if fabric {
import me.drex.vanish.api.VanishAPI;
//? if != 1.20.2 && != 1.19.4 {
/*import me.drex.vanish.util.VanishedEntity;
*///?}
//?}
import net.minecraft.server.level.ServerPlayer;

//? if fabric {
import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;
//?}

public class VanishIntegration {
    public static boolean isVanished(ServerPlayer player) {
        //? if neoforge {
        /*// Vanish has no NeoForge port; always false
        return false;
        *///?} else {
        if (!technicalConfig.vanishLoaded) return false;
        return VanishAPI.isVanished(player);
        //?}
    }

    public static void setVanished(ServerPlayer player, boolean vanished) {
        //? if neoforge {
        /*// no-op on NeoForge
        *///?} else {
        if (!technicalConfig.vanishLoaded) return;
        VanishAPI.setVanish(player, vanished);
        //? if != 1.20.2 && != 1.19.4 {
        /*((VanishedEntity) player).vanish$setDirty();
        *///?}
        //?}
    }
}
