package xyz.nikitacartes.easyauth.client;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
//? if >=1.21.9 {
import net.minecraft.client.gui.screens.ManageServerScreen;
//?} else {
/*import net.minecraft.client.gui.screens.EditServerScreen;
*///?}
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.rules.Vault;
import xyz.nikitacartes.easyauth.client.screen.ConfigScreen;
import xyz.nikitacartes.easyauth.client.screen.ServerEditScreen;
import xyz.nikitacartes.easyauth.client.screen.UnlockScreen;

public class EasyAuthClientFabric implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    @Override
    public void onInitializeClient() {
        RuleEngine.init(FabricLoader.getInstance().getConfigDir());
        EasyAuthPackets.init();

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> RuleEngine.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> RuleEngine.onDisconnect());
        // hasChatRules() gate: skip flattening messages on sessions with no chat rules.
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
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // Once-per-launch master-password prompt, before any join so auto-login can use the unlocked secrets.
            if (screen instanceof JoinMultiplayerScreen && Vault.shouldPromptUnlock()) {
                Vault.markPrompted();
                ConfigScreen.open(new UnlockScreen(screen));
            }
            // "…" shortcut next to the address box of the vanilla edit-server screen.
            //? if >=1.21.9 {
            if (screen instanceof ManageServerScreen) {
            //?} else {
            /*if (screen instanceof EditServerScreen) {
            *///?}
                //? if >=26.1 {
                var widgets = Screens.getWidgets(screen);
                //?} else {
                /*var widgets = Screens.getButtons(screen);*/
                //?}
                Button shortcut = ServerEditScreen.vanillaShortcut(screen, widgets);
                if (shortcut != null) {
                    widgets.add(shortcut);
                }
            }
            // Wrap the disconnect button so "leave" rules fire while the connection is open (see QuitHook).
            if (screen instanceof PauseScreen && RuleEngine.hasLeaveRules()) {
                //? if >=26.1 {
                var buttons = Screens.getWidgets(screen);
                //?} else {
                /*var buttons = Screens.getButtons(screen);*/
                //?}
                for (int i = 0; i < buttons.size(); i++) {
                    if (buttons.get(i) instanceof Button button && QuitHook.isDisconnect(button)) {
                        buttons.set(i, QuitHook.wrap(button));
                    }
                }
            }
        });

        LOGGER.info("EasyAuth Client loaded");
    }
}
//?}
