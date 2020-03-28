package org.samo_lego.simpleauth.event.item;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface TakeItemCallback {
    Event<TakeItemCallback> EVENT = EventFactory.createArrayBacked(TakeItemCallback.class, listeners -> (player) -> {
        for (TakeItemCallback event : listeners) {
            ActionResult result = event.onTakeItem(player);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onTakeItem(PlayerEntity player);
}
