package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.AuthCommand;
import org.samo_lego.simpleauth.commands.ChangepwCommand;
import org.samo_lego.simpleauth.commands.LoginCommand;
import org.samo_lego.simpleauth.commands.RegisterCommand;
import org.samo_lego.simpleauth.database.SimpleAuthDatabase;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.PlayerJoinServerCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerLeaveServerCallback;
import org.samo_lego.simpleauth.event.item.DropItemCallback;

import java.io.File;
import java.util.HashSet;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();
    public static SimpleAuthDatabase db = new SimpleAuthDatabase();

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
			ChangepwCommand.registerCommand(dispatcher);
			AuthCommand.registerCommand(dispatcher);
		});

		// Registering the events
		PlayerJoinServerCallback.EVENT.register(AuthEventHandler::onPlayerJoin);
		PlayerLeaveServerCallback.EVENT.register(AuthEventHandler::onPlayerLeave);
		DropItemCallback.EVENT.register(player -> AuthEventHandler.onDropItem(player));
		// From Fabric API
		AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> AuthEventHandler.onAttackBlock(playerEntity));
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
        UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));

		// Making a table in database
        db.makeTable();
    }
    public static HashSet<PlayerEntity> authenticatedUsers = new HashSet<>();

    public static boolean isAuthenticated(ServerPlayerEntity player) { return authenticatedUsers.contains(player); }

}
