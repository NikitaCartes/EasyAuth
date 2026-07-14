package xyz.nikitacartes.easyauth.client;

//? if neoforge {
/*import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.rules.Vault;
import xyz.nikitacartes.easyauth.client.screen.ConfigScreen;
import xyz.nikitacartes.easyauth.client.screen.UnlockScreen;

import java.util.List;

@Mod(value = EasyAuthClientNeoForge.MOD_ID, dist = Dist.CLIENT)
public class EasyAuthClientNeoForge {
    public static final String MOD_ID = "easyauth";
    public static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    public EasyAuthClientNeoForge(IEventBus modBus, ModContainer container) {
        RuleEngine.init(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));

        // Companion-mod packet channels (all NeoForge targets are >=1.21, which has the payload API).
        modBus.addListener(EasyAuthPackets::onRegisterPayloads);

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> RuleEngine.onJoin());
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> RuleEngine.onDisconnect());
        // hasChatRules() gate: skip flattening messages on sessions with no chat rules.
        gameBus.addListener((ClientChatReceivedEvent.System event) -> {
            if (!event.isOverlay() && RuleEngine.hasChatRules()) {
                RuleEngine.onChat(event.getMessage().getString(), true, null);
            }
        });
        gameBus.addListener((ClientChatReceivedEvent.Player event) -> {
            if (RuleEngine.hasChatRules()) {
                RuleEngine.onChat(event.getMessage().getString(), false, event.getSender());
            }
        });
        gameBus.addListener((ClientTickEvent.Post event) -> RuleEngine.onTick());
        gameBus.addListener((ScreenEvent.Init.Post event) -> {
            // Once-per-launch master-password prompt, before any join so auto-login can use the unlocked secrets.
            if (event.getScreen() instanceof JoinMultiplayerScreen && Vault.shouldPromptUnlock()) {
                Vault.markPrompted();
                ConfigScreen.open(new UnlockScreen(event.getScreen()));
            }
            // Wrap the disconnect button so "leave" rules fire while the connection is open (see QuitHook).
            if (event.getScreen() instanceof PauseScreen && RuleEngine.hasLeaveRules()) {
                for (var listener : List.copyOf(event.getListenersList())) {
                    if (listener instanceof Button button && QuitHook.isDisconnect(button)) {
                        event.removeListener(button);
                        event.addListener(QuitHook.wrap(button));
                    }
                }
            }
        });

        LOGGER.info("EasyAuth Client loaded");
    }
}
*///?}
