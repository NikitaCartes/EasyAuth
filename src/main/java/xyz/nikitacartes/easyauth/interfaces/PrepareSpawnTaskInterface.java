package xyz.nikitacartes.easyauth.interfaces;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import xyz.nikitacartes.easyauth.utils.LastLocation;

public interface PrepareSpawnTaskInterface {
    // Temporary store spawn data until ServerPlayerEntity is created
    void easyAuth$setSpawnData(LastLocation spawnData);

    LastLocation easyAuth$getSpawnData();

    // Is that player authenticated?
    void easyAuth$setAuthenticated(boolean authenticated);

    boolean easyAuth$getAuthenticated();

    NameAndId easyAuth$getPlayer();

    MinecraftServer easyAuth$getServer();
}
