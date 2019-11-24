package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.util.ActionResult;

public interface OnChatCallback {
    Event<OnChatCallback> EVENT = EventFactory.createArrayBacked(OnChatCallback.class, listeners -> (player, chatMessageC2SPacket) -> {
        for (OnChatCallback event : listeners) {
            ActionResult result = event.onPlayerChat(player, chatMessageC2SPacket);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onPlayerChat(PlayerEntity player, ChatMessageC2SPacket chatMessageC2SPacket);
}