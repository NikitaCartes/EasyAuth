package org.samo_lego.simpleauth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.samo_lego.simpleauth.commands.*;
import org.samo_lego.simpleauth.event.AuthEventHandler;

public class SimpleAuthFabric implements ModInitializer {

	@Override
	public void onInitialize() {
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
		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> AuthCommand.reloadConfig(null));
		ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopServer);
	}

	private void onStopServer(MinecraftServer server) {
		SimpleAuth.stop();
	}
}
