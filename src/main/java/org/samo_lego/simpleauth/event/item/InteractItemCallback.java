package org.samo_lego.simpleauth.event.item;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public interface InteractItemCallback {
    Event<InteractItemCallback> EVENT = EventFactory.createArrayBacked(InteractItemCallback.class,
            (listeners) -> (player) -> {
                for (InteractItemCallback event : listeners) {
                    ActionResult result = event.onInteractItem(player);
                    if(result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });
    ActionResult onInteractItem(ServerPlayerEntity player);
}
