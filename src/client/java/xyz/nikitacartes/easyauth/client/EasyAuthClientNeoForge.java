package xyz.nikitacartes.easyauth.client;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.rules.RuleEngine;
import xyz.nikitacartes.easyauth.client.screen.ConfigScreen;

@Mod(value = EasyAuthClientNeoForge.MOD_ID, dist = Dist.CLIENT)
public class EasyAuthClientNeoForge {
    public static final String MOD_ID = "easyauth";
    public static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    public EasyAuthClientNeoForge(IEventBus modBus, ModContainer container) {
        RuleEngine.init(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));

        // Companion-mod packet channels (all NeoForge targets are >=1.21, which has the payload API).
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                EasyAuthPackets.onRegisterPayloads(event));

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> RuleEngine.onJoin());
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> RuleEngine.onDisconnect());
        // hasChatRules() gate: don't flatten every chat/system message into a String (the
        // typical session has no chat rules at all).
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
        // "leave" rules must go out while the connection is still open (LoggingOut above fires
        // after it closed), so the pause-menu disconnect button gets wrapped — see QuitHook.
        gameBus.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) -> {
            if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen && RuleEngine.hasLeaveRules()) {
                for (var listener : java.util.List.copyOf(event.getListenersList())) {
                    if (listener instanceof net.minecraft.client.gui.components.Button button
                            && QuitHook.isDisconnect(button)) {
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
