package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public interface PlayerJoinWorldCallback {

    Event<PlayerJoinWorldCallback> EVENT = EventFactory.createArrayBacked(PlayerJoinWorldCallback.class, listeners -> (world, player) -> {
        for (PlayerJoinWorldCallback callback : listeners) {
            callback.onPlayerJoin(world, player);
        }
    });

    /**
     * Fired when a player joins a world
     */
    void onPlayerJoin(ServerWorld world, ServerPlayerEntity player);
}
