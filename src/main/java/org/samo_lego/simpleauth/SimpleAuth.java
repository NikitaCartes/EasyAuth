package org.samo_lego.simpleauth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.LoginCommand;
import org.samo_lego.simpleauth.commands.RegisterCommand;
import org.samo_lego.simpleauth.database.SimpleAuthDatabase;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.block.BreakBlockCallback;
import org.samo_lego.simpleauth.event.block.InteractBlockCallback;
import org.samo_lego.simpleauth.event.entity.player.*;

import java.io.File;
import java.util.HashSet;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		LOGGER.info("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		// Creating data directory (database is stored there)
		File file = new File("./mods/SimpleAuth");
		if (!file.exists() && !file.mkdir())
		    LOGGER.error("Error creating directory");


		// Registering the commands
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			RegisterCommand.registerCommand(dispatcher);
			LoginCommand.registerCommand(dispatcher);
		});

		// Registering the events
		InteractBlockCallback.EVENT.register(AuthEventHandler::onInteractBlock);
		AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> AuthEventHandler.interact(playerEntity));
		InteractItemCallback.EVENT.register(AuthEventHandler::onInteractItem);
		PlayerJoinServerCallback.EVENT.register(AuthEventHandler::onPlayerJoin);
		PlayerLeaveServerCallback.EVENT.register(AuthEventHandler::onPlayerLeave);
		BreakBlockCallback.EVENT.register((world, pos, state, player) -> AuthEventHandler.onBlockBroken(player));

		// Connection to database
		SimpleAuthDatabase.main();
    }
    public static HashSet<ServerPlayerEntity> authenticatedUsers = new HashSet<>();

    public static boolean isAuthenticated(ServerPlayerEntity player) { return authenticatedUsers.contains(player); }

}
