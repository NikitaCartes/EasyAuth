package org.samo_lego.simpleauth.event.item;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface DropItemCallback {
    Event<DropItemCallback> EVENT = EventFactory.createArrayBacked(DropItemCallback.class, listeners -> (playerEntity) -> {
        for(DropItemCallback callback : listeners) {
            if(callback.onDropItem(playerEntity)) {
                return true;
            }
        }
        return false;
    });

    boolean onDropItem(ServerPlayerEntity playerEntity);
}
