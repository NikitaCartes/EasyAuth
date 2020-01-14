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
import org.samo_lego.simpleauth.event.entity.player.ChatCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerMoveCallback;
import org.samo_lego.simpleauth.event.entity.player.PlayerJoinServerCallback;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.samo_lego.simpleauth.utils.AuthConfig;

import java.io.File;
import java.util.HashSet;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();
    public static SimpleAuthDatabase db = new SimpleAuthDatabase();
    // HashSet of players that are not authenticated
	// Rather than storing all the authenticated players, we just store ones that are not authenticated
	public static HashSet<PlayerEntity> deauthenticatedUsers = new HashSet<>();
	// Boolean for easier checking if player is authenticated
	public static boolean isAuthenticated(ServerPlayerEntity player) { return !deauthenticatedUsers.contains(player); }
	// Mod config
	public static AuthConfig config;


	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("[SimpleAuth] SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		LOGGER.info("[SimpleAuth] This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		// Creating data directory (database and config files are stored there)
		File file = new File("./mods/SimpleAuth");
		if (!file.exists() && !file.mkdir())
		    LOGGER.error("[SimpleAuth] Error creating directory!");
		// Loading config
		config = AuthConfig.load(new File("./mods/SimpleAuth/config.json"));
		// Connecting to db
		db.openConnection();
		// Making a table in the database
		db.makeTable();

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
		DropItemCallback.EVENT.register(AuthEventHandler::onDropItem);
		ChatCallback.EVENT.register(AuthEventHandler::onPlayerChat);
		PlayerMoveCallback.EVENT.register(AuthEventHandler::onPlayerMove);
		// From Fabric API
		AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> AuthEventHandler.onAttackBlock(playerEntity));
        UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
        UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
		ServerStopCallback.EVENT.register(minecraftServer -> SimpleAuth.onStopServer());
	}
	private static void onStopServer() {
		LOGGER.info("[SimpleAuth] Shutting down SimpleAuth.");
		db.close();
	}
}