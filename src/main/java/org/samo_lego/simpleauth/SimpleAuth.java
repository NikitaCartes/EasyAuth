package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.*;
import org.samo_lego.simpleauth.database.SimpleAuthDatabase;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.OnChatCallback;
import org.samo_lego.simpleauth.event.entity.player.OnPlayerMoveCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerJoinServerCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerLeaveServerCallback;
import org.samo_lego.simpleauth.event.item.DropItemCallback;

import java.io.File;
import java.util.HashSet;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();
    public static SimpleAuthDatabase db = new SimpleAuthDatabase();
	public static HashSet<PlayerEntity> authenticatedUsers = new HashSet<>();
	public static boolean isAuthenticated(ServerPlayerEntity player) { return authenticatedUsers.contains(player); }

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("[SimpleAuth] SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		LOGGER.info("[SimpleAuth] This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");
		// Connecting to db
		db.openConnection();

		// Creating data directory (database is stored there)
		File file = new File("./mods/SimpleAuth");
		if (!file.exists() && !file.mkdir())
		    LOGGER.error("[SimpleAuth] Error creating directory!");


		// Registering the commands
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			RegisterCommand.registerCommand(dispatcher);
			LoginCommand.registerCommand(dispatcher);
			ChangepwCommand.registerCommand(dispatcher);
			UnregisterCommand.registerCommand(dispatcher);
			AuthCommand.registerCommand(dispatcher);
		});

		// Registering the events
		PlayerJoinServerCallback.EVENT.register(AuthEventHandler::onPlayerJoin);
		PlayerLeaveServerCallback.EVENT.register(AuthEventHandler::onPlayerLeave);
		DropItemCallback.EVENT.register(AuthEventHandler::onDropItem);
		//todo
		OnChatCallback.EVENT.register(AuthEventHandler::onPlayerChat);
		OnPlayerMoveCallback.EVENT.register(AuthEventHandler::onPlayerMove);
		// From Fabric API
		AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> AuthEventHandler.onAttackBlock(playerEntity));
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
        UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
		ServerStopCallback.EVENT.register(minecraftServer -> SimpleAuth.onStopServer());
		// Making a table in the database
        db.makeTable();
    }

	private static void onStopServer() {
		LOGGER.info("[SimpleAuth] Shutting down SimpleAuth.");
		db.close();
	}
}