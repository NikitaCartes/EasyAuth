package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public interface InteractBlockCallback {
    Event<InteractBlockCallback> EVENT = EventFactory.createArrayBacked(InteractBlockCallback.class,
            (listeners) -> (player) -> {
                for (InteractBlockCallback event : listeners) {
                    ActionResult result = event.onInteractBlock(player);
                    if(result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });
    ActionResult onInteractBlock(ServerPlayerEntity player);
}
