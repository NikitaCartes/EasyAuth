package org.samo_lego.simpleauth.event.entity.player;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.LiteralText;

import java.net.SocketAddress;

public interface PrePlayerJoinCallback {
    Event<PrePlayerJoinCallback> EVENT = EventFactory.createArrayBacked(
            PrePlayerJoinCallback.class, listeners -> (
                    socketAddress, profile, manager
            ) -> {
        for (PrePlayerJoinCallback event : listeners) {

            LiteralText returnText = event.checkCanPlayerJoinServer(socketAddress, profile, manager);

            if (returnText != null) {
                return returnText;
            }
        }
        return null;
    });

    LiteralText checkCanPlayerJoinServer(SocketAddress socketAddress, GameProfile profile, PlayerManager manager);
}