package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.ActionResult;

public interface ChatCallback {
    Event<ChatCallback> EVENT = EventFactory.createArrayBacked(ChatCallback.class, listeners -> (player, chatMessageC2SPacket) -> {
        for (ChatCallback event : listeners) {
            ActionResult result = event.onPlayerChat(player, chatMessageC2SPacket);

            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    });

    ActionResult onPlayerChat(PlayerEntity player, ChatMessageC2SPacket chatMessageC2SPacket);
}