package xyz.nikitacartes.easyauth.integrations;

import net.minecraft.server.level.ServerPlayer;

public class VanishIntegration {

    public static boolean isVanished(ServerPlayer player) {
        return false;
    }

    public static void setVanished(ServerPlayer player, boolean vanished) {
        // no-op on NeoForge
    }
}
