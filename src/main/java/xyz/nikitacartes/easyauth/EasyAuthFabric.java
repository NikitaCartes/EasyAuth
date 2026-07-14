//~ resource_location
package xyz.nikitacartes.easyauth;

//? if fabric {
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.ClientModBridge;
import xyz.nikitacartes.easyauth.proxy.ProxyBridge;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;
import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class EasyAuthFabric implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        gameDirectory = FabricLoader.getInstance().getGameDir();
        LogInfo("EasyAuth mod by NikitaCartes");

        createConfigFolder();

        loadConfigs();
        loadDatabase();

        ProxyBridge.init();
        ClientModBridge.init();

        registerCommands();
        registerEvents();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            RegisterCommand.registerCommand(dispatcher);
            LoginCommand.registerCommand(dispatcher);
            LogoutCommand.registerCommand(dispatcher);
            AuthCommand.registerCommand(dispatcher);
            AccountCommand.registerCommand(dispatcher);
            if (proxyConfig != null && proxyConfig.enabled) {
                PremiumCommand.registerCommand(dispatcher);
            }
        });
    }

    private void registerEvents() {
        // From Fabric API
        PlayerBlockBreakEvents.BEFORE.register((world, player, blockPos, blockState, blockEntity) -> AuthEventHandler.onBreakBlock(player));
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
        UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
        UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> {
            reloadConfigs(server);
            langConfig.admin.configReloaded.send(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(EasyAuth::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(EasyAuth::onStopServer);

        //? if >= 1.21 {
        Identifier earlyPhase = Identifier.fromNamespaceAndPath("easyauth", "early");
        //?} else {
        /*Identifier earlyPhase = new Identifier("easyauth", "early");
        *///?}
        ServerLoginConnectionEvents.QUERY_START.addPhaseOrdering(earlyPhase, Event.DEFAULT_PHASE);
        ServerLoginConnectionEvents.QUERY_START.register(earlyPhase, (netHandler, server, packetSender, sync) -> AuthEventHandler.onPreLogin(netHandler));
    }
}
//?}
