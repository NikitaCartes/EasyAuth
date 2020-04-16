package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.commands.*;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.*;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.samo_lego.simpleauth.event.item.TakeItemCallback;
import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.PlayerCache;
import org.samo_lego.simpleauth.storage.SimpleAuthDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class SimpleAuth implements DedicatedServerModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();

    public static SimpleAuthDatabase db = new SimpleAuthDatabase();

    // HashMap of players that are not authenticated
	// Rather than storing all the authenticated players, we just store ones that are not authenticated
	// It stores some data as well, e.g. login tries and user password
	public static HashMap<String, PlayerCache> deauthenticatedUsers = new HashMap<>();

	// Boolean for easier checking if player is authenticated
	public static boolean isAuthenticated(ServerPlayerEntity player) {
		return !deauthenticatedUsers.containsKey(player.getUuidAsString());
	}

	// Getting game directory
	public static final File gameDirectory = FabricLoader.getInstance().getGameDirectory();
	// Mod config
	public static AuthConfig config;

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		LOGGER.info("[SimpleAuth] SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		LOGGER.info("[SimpleAuth] This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		// Creating data directory (database and config files are stored there)
		File file = new File(gameDirectory + "/mods/SimpleAuth/levelDBStore");
		if (!file.exists() && !file.mkdir())
		    LOGGER.error("[SimpleAuth] Error creating directory!");
		// Loading config
		config = AuthConfig.load(new File(gameDirectory + "/mods/SimpleAuth/config.json"));
		// Connecting to db
		db.openConnection();


		// Registering the commands
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			RegisterCommand.registerCommand(dispatcher);
			LoginCommand.registerCommand(dispatcher);
			LogoutCommand.registerCommand(dispatcher);
			ChangepwCommand.registerCommand(dispatcher);
			UnregisterCommand.registerCommand(dispatcher);
			AuthCommand.registerCommand(dispatcher);
		});

		// Registering the events
		PrePlayerJoinCallback.EVENT.register(AuthEventHandler::checkCanPlayerJoinServer);
		PlayerJoinServerCallback.EVENT.register(AuthEventHandler::onPlayerJoin);
		PlayerLeaveServerCallback.EVENT.register(AuthEventHandler::onPlayerLeave);
		DropItemCallback.EVENT.register(AuthEventHandler::onDropItem);
		TakeItemCallback.EVENT.register(AuthEventHandler::onTakeItem);
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

	// Getting some config options
	private static Text notAuthenticated() {
		if(SimpleAuth.config.main.enableGlobalPassword) {
			return new LiteralText(SimpleAuth.config.lang.loginRequired);
		}
		return new LiteralText(SimpleAuth.config.lang.notAuthenticated);
	}

	// Authenticates player and sends the message
	public static void authenticatePlayer(ServerPlayerEntity player, Text msg) {
		deauthenticatedUsers.remove(player.getUuidAsString());
		// Player no longer needs to be invisible and invulnerable
		player.setInvulnerable(false);
		player.setInvisible(false);
		player.sendMessage(msg, false);
	}

	// De-authenticates player
	public static void deauthenticatePlayer(ServerPlayerEntity player) {
		if(db.isClosed())
			return;
		// Marking player as not authenticated, (re)setting login tries to zero
		String uuid = player.getUuidAsString();
		SimpleAuth.deauthenticatedUsers.put(uuid, new PlayerCache(uuid, player.getIp()));

		// Player is now not authenticated
		player.sendMessage(notAuthenticated(), false);
		// Setting the player to be invisible to mobs and also invulnerable
		player.setInvulnerable(SimpleAuth.config.experimental.playerInvulnerable);
		player.setInvisible(SimpleAuth.config.experimental.playerInvisible);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(!SimpleAuth.isAuthenticated(player)) // Kicking player if not authenticated
					player.networkHandler.disconnect(new LiteralText(SimpleAuth.config.lang.timeExpired));
			}
		}, SimpleAuth.config.main.delay * 1000);
	}
}