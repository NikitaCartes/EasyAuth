package xyz.nikitacartes.easyauth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import xyz.nikitacartes.easyauth.commands.*;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;
import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class EasyAuthFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        gameDirectory = FabricLoader.getInstance().getGameDir();
        LogInfo("EasyAuth mod by NikitaCartes");

        migrateConfigs();

        loadConfigs();
        loadDatabase();

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
            langConfig.configurationReloaded.send(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(EasyAuth::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(EasyAuth::onStopServer);

        Identifier earlyPhase = Identifier.of("easyauth", "early");
        ServerLoginConnectionEvents.QUERY_START.addPhaseOrdering(earlyPhase, Event.DEFAULT_PHASE);
        ServerLoginConnectionEvents.QUERY_START.register(earlyPhase, (netHandler, server, packetSender, sync) -> AuthEventHandler.onPreLogin(netHandler));
    }
}
