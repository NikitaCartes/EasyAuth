package xyz.nikitacartes.easyauth.client;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;

public class EasyAuthClientFabric implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    @Override
    public void onInitializeClient() {
        RuleEngine.init(FabricLoader.getInstance().getConfigDir());
        EasyAuthPackets.init();

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> RuleEngine.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> RuleEngine.onDisconnect());
        // hasChatRules() gate: don't flatten every chat/system message into a String (the
        // typical session has no chat rules at all).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && RuleEngine.hasChatRules()) {
                RuleEngine.onChat(message.getString(), true, null);
            }
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, boundChatType, timeStamp) -> {
            if (RuleEngine.hasChatRules()) {
                //? if >=1.21.9 {
                RuleEngine.onChat(message.getString(), false, sender != null ? sender.id() : null);
                //?} else {
                /*RuleEngine.onChat(message.getString(), false, sender != null ? sender.getId() : null);*/
                //?}
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> RuleEngine.onTick());
        // "leave" rules must go out while the connection is still open (DISCONNECT above fires
        // after it closed), so the pause-menu disconnect button gets wrapped — see QuitHook.
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screens.PauseScreen && RuleEngine.hasLeaveRules()) {
                //? if >=26.1 {
                var buttons = net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen);
                //?} else {
                /*var buttons = net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen);*/
                //?}
                for (int i = 0; i < buttons.size(); i++) {
                    if (buttons.get(i) instanceof net.minecraft.client.gui.components.Button button
                            && QuitHook.isDisconnect(button)) {
                        buttons.set(i, QuitHook.wrap(button));
                    }
                }
            }
        });

        LOGGER.info("EasyAuth Client loaded");
    }
}
//?}
