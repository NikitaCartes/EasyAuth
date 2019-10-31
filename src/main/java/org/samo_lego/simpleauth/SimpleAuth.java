package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.LoginCommand;
import org.samo_lego.simpleauth.commands.RegisterCommand;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.BreakBlockCallback;
import org.samo_lego.simpleauth.event.entity.player.InteractBlockCallback;
import org.samo_lego.simpleauth.event.entity.player.InteractItemCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerJoinWorldCallback;

import java.util.HashSet;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("SimpleAuth mod by samo_lego.");
		LOGGER.info("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		// Registering the commands
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			RegisterCommand.register(dispatcher);
			LoginCommand.register(dispatcher);
		});

		// Registering the events
		InteractBlockCallback.EVENT.register(AuthEventHandler::onInteractBlock);
		InteractItemCallback.EVENT.register(AuthEventHandler::onInteractItem);
		PlayerJoinWorldCallback.EVENT.register((world, player) -> AuthEventHandler.onPlayerJoin(player));
		BreakBlockCallback.EVENT.register((world, pos, state, player) -> AuthEventHandler.onBlockBroken(player));
	}
    public static HashSet<ServerPlayerEntity> authenticatedUsers = new HashSet<>();

    public static boolean isAuthenticated(ServerPlayerEntity player) { return authenticatedUsers.contains(player); }

}
