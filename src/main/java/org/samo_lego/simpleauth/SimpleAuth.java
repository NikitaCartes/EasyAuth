package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.LoginCommand;
import org.samo_lego.simpleauth.commands.RegisterCommand;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("SimpleAuth mod by samo_lego.");

		// Registering the commands
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			RegisterCommand.register(dispatcher);
			LoginCommand.register(dispatcher);
		});
	}
}
