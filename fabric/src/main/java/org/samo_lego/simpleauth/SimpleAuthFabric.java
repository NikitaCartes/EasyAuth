package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class SimpleAuthFabric implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		SimpleAuth.init(FabricLoader.getInstance().getGameDir());


		// Registering the commands
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			RegisterCommand.registerCommand(dispatcher);
			LoginCommand.registerCommand(dispatcher);
			LogoutCommand.registerCommand(dispatcher);
			AuthCommand.registerCommand(dispatcher);
			AccountCommand.registerCommand(dispatcher);
		});

		// From Fabric API
		PlayerBlockBreakEvents.BEFORE.register((world, player, blockPos, blockState, blockEntity) -> AuthEventHandler.onBreakBlock(player));
		UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
		UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
		AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> AuthEventHandler.reloadConfig(null));
		ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopServer);
	}

	private void onStopServer(MinecraftServer server) {
		SimpleAuth.stop();
	}
}
