package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.util.ActionResult;

public interface OnPlayerMoveCallback {
    Event<OnPlayerMoveCallback> EVENT = EventFactory.createArrayBacked(OnPlayerMoveCallback.class, listeners -> (player) -> {
        for (OnPlayerMoveCallback event : listeners) {
            ActionResult result = event.onPlayerMove(player);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onPlayerMove(PlayerEntity player);
}