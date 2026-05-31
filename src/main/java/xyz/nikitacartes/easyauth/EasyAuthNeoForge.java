package xyz.nikitacartes.easyauth;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import xyz.nikitacartes.easyauth.commands.AccountCommand;
import xyz.nikitacartes.easyauth.commands.AuthCommand;
import xyz.nikitacartes.easyauth.commands.LoginCommand;
import xyz.nikitacartes.easyauth.commands.LogoutCommand;
import xyz.nikitacartes.easyauth.commands.RegisterCommand;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;
import static xyz.nikitacartes.easyauth.EasyAuth.loadConfigs;
import static xyz.nikitacartes.easyauth.EasyAuth.loadDatabase;
import static xyz.nikitacartes.easyauth.EasyAuth.migrateConfigs;
import static xyz.nikitacartes.easyauth.EasyAuth.reloadConfigs;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * NeoForge entrypoint for EasyAuth.
 *
 * Replaces the previous Fabric entrypoint (EasyAuthFabric). Registers
 * commands and event listeners on NeoForge's game event bus.
 */
@Mod(EasyAuthNeoForge.MOD_ID)
public class EasyAuthNeoForge {
    public static final String MOD_ID = "easyauth";

    public EasyAuthNeoForge(IEventBus modBus, ModContainer container) {
        EasyAuth.gameDirectory = FMLPaths.GAMEDIR.get();
        LogInfo("EasyAuth mod by NikitaCartes (NeoForge port)");

        migrateConfigs();
        loadConfigs();
        loadDatabase();

        // Mod-bus events (setup, registry)
        modBus.addListener(this::onCommonSetup);

        // Game-bus events (commands, world, players)
        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(this::onRegisterCommands);
        gameBus.addListener(this::onServerStarted);
        gameBus.addListener(this::onServerStopped);
        gameBus.addListener(this::onAddServerReloadListeners);

        gameBus.addListener(this::onBreakBlock);
        gameBus.addListener(this::onUseBlock);
        gameBus.addListener(this::onUseItem);
        gameBus.addListener(this::onAttackEntity);
        gameBus.addListener(this::onUseEntity);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Reserved for future setup work.
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        RegisterCommand.registerCommand(dispatcher);
        LoginCommand.registerCommand(dispatcher);
        LogoutCommand.registerCommand(dispatcher);
        AuthCommand.registerCommand(dispatcher);
        AccountCommand.registerCommand(dispatcher);
    }

    private void onServerStarted(ServerStartedEvent event) {
        runningServer = event.getServer();
        EasyAuth.onStartServer(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        try {
            EasyAuth.onStopServer(event.getServer());
        } finally {
            runningServer = null;
        }
    }

    private void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "config_reload"),
                new EasyAuthReloadListener());
    }

    /**
     * Holds the running server so the reload listener can call back into
     * {@link EasyAuth#reloadConfigs(MinecraftServer)} on the main thread.
     */
    private static volatile MinecraftServer runningServer;

    private static final class EasyAuthReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(SharedState currentReload,
                                              Executor taskExecutor,
                                              PreparationBarrier preparationBarrier,
                                              Executor reloadExecutor) {
            return CompletableFuture.completedFuture(null)
                    .thenCompose(preparationBarrier::wait)
                    .thenRunAsync(() -> {
                        MinecraftServer server = runningServer;
                        if (server == null) {
                            return;
                        }
                        reloadConfigs(server);
                        langConfig.configurationReloaded.send(server);
                    }, reloadExecutor);
        }
    }

    private void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!AuthEventHandler.onBreakBlock(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (AuthEventHandler.onUseBlock(event.getEntity()) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }

    private void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (AuthEventHandler.onUseItem(event.getEntity()) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }

    private void onAttackEntity(AttackEntityEvent event) {
        if (AuthEventHandler.onAttackEntity(event.getEntity()) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }

    private void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (AuthEventHandler.onUseEntity(event.getEntity()) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }
}
