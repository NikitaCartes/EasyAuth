package org.samo_lego.simpleauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.samo_lego.simpleauth.commands.*;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.*;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.samo_lego.simpleauth.event.item.TakeItemCallback;
import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.DBHelper;
import org.samo_lego.simpleauth.storage.PlayerCache;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.samo_lego.simpleauth.commands.AuthCommand.reloadConfig;
import static org.samo_lego.simpleauth.event.AuthEventHandler.*;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class SimpleAuth implements DedicatedServerModInitializer {

    public static DBHelper DB = new DBHelper();

	public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

	/**
	 * HashMap of players that have joined the server.
	 * It's cleared on server stop in order to save some interactions with database during runtime.
	 * Stores their data as {@link org.samo_lego.simpleauth.storage.PlayerCache PlayerCache} object.
	 */
	public static final HashMap<String, PlayerCache> playerCacheMap = new HashMap<>();

	/**
	 * HashSet of player names that have Mojang accounts.
	 * If player is saved in here, they will be treated as online-mode ones.
	 */
	public static final HashSet<String> mojangAccountNamesCache = new HashSet<>();

	// Getting game directory
	public static final Path gameDirectory = FabricLoader.getInstance().getGameDir();

	// Server properties
	public static final Properties serverProp = new Properties();

	/**
	 * Config of the SimpleAuth mod.
	 */
	public static AuthConfig config;

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		logInfo("SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		logInfo("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		try {
			serverProp.load(new FileReader(gameDirectory + "/server.properties"));
		} catch (IOException e) {
			logError("Error while reading server properties: " + e.getMessage());
		}

		// Creating data directory (database and config files are stored there)
		File file = new File(gameDirectory + "/mods/SimpleAuth/leveldbStore");
		if (!file.exists() && !file.mkdirs())
		    throw new RuntimeException("[SimpleAuth] Error creating directory!");
		// Loading config
		config = AuthConfig.load(new File(gameDirectory + "/mods/SimpleAuth/config.json"));
		// Connecting to db
		DB.openConnection();


		// Registering the commands
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			RegisterCommand.registerCommand(dispatcher);
			LoginCommand.registerCommand(dispatcher);
			LogoutCommand.registerCommand(dispatcher);
			AuthCommand.registerCommand(dispatcher);
			AccountCommand.registerCommand(dispatcher);
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
		PlayerBlockBreakEvents.BEFORE.register((world, player, blockPos, blockState, blockEntity) -> onBreakBlock(player));
		UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> onUseBlock(player));
		UseItemCallback.EVENT.register((player, world, hand) -> onUseItem(player));
		AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> onUseEntity(player));
		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> reloadConfig(null));
		ServerLifecycleEvents.SERVER_STOPPED.register(this::onStopServer);
	}

	/**
	 * Called on server stop.
	 */
	private void onStopServer(MinecraftServer server) {
		logInfo("Shutting down SimpleAuth.");
		DB.saveAll(playerCacheMap);

		// Closing threads
		try {
            THREADPOOL.shutdownNow();
            if (!THREADPOOL.awaitTermination(500, TimeUnit.MILLISECONDS)) {
				Thread.currentThread().interrupt();
            }
        } catch (InterruptedException e) {
		    logError(e.getMessage());
			THREADPOOL.shutdownNow();
        }

        // Closing DB connection
		DB.close();
	}
}
