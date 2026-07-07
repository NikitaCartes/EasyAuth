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
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                RuleEngine.onChat(message.getString(), true, null);
            }
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, boundChatType, timeStamp) ->
                //? if >=1.21.9 {
                RuleEngine.onChat(message.getString(), false, sender != null ? sender.id() : null));
                //?} else {
                /*RuleEngine.onChat(message.getString(), false, sender != null ? sender.getId() : null));*/
                //?}
        ClientTickEvents.END_CLIENT_TICK.register(client -> RuleEngine.onTick());

        LOGGER.info("EasyAuth Client loaded");
    }
}
//?}
