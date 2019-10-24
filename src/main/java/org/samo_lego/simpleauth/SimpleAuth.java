package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

public class SimpleAuth implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.

		System.out.println("SimpleAuth mod by samo_lego."); // Info I guess :D

		// Registering the commands
		CommandRegistry.INSTANCE.register(true, AuthCommands::registerCommands);
	}
}
