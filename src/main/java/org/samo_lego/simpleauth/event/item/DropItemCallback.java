package org.samo_lego.simpleauth.event.item;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface DropItemCallback {
    Event<DropItemCallback> EVENT = EventFactory.createArrayBacked(DropItemCallback.class, listeners -> (player) -> {
        for (DropItemCallback event : listeners) {
            ActionResult result = event.onDropItem(player);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onDropItem(PlayerEntity player);
}