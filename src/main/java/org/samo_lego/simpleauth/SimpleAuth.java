package org.samo_lego.simpleauth;

import com.google.gson.JsonObject;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.iq80.leveldb.WriteBatch;
import org.samo_lego.simpleauth.commands.*;
import org.samo_lego.simpleauth.event.AuthEventHandler;
import org.samo_lego.simpleauth.event.entity.player.*;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.samo_lego.simpleauth.event.item.TakeItemCallback;
import org.samo_lego.simpleauth.storage.AuthConfig;
import org.samo_lego.simpleauth.storage.PlayerCache;
import org.samo_lego.simpleauth.storage.SimpleAuthDatabase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.samo_lego.simpleauth.commands.AuthCommand.reloadConfig;
import static org.samo_lego.simpleauth.event.AuthEventHandler.*;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class SimpleAuth implements DedicatedServerModInitializer {

    public static SimpleAuthDatabase DB = new SimpleAuthDatabase();

	public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

	/**
	 * HashMap of players that have joined the server.
	 * It's cleared on server stop in order to save some interactions with database during runtime.
	 * Stores their data as {@link org.samo_lego.simpleauth.storage.PlayerCache PlayerCache} object.
	 */
	public static HashMap<String, PlayerCache> playerCacheMap = new HashMap<>();

	// Getting game directory
	public static final Path gameDirectory = FabricLoader.getInstance().getGameDir();

	// Server properties
	public static Properties serverProp = new Properties();

	// Mod config
	public static AuthConfig config;

	@Override
	public void onInitializeServer() {
		// Info I guess :D
		logInfo("SimpleAuth mod by samo_lego.");
		// The support on discord was great! I really appreciate your help.
		logInfo("This mod wouldn't exist without the awesome Fabric Community. TYSM guys!");

		// Creating data directory (database and config files are stored there)
		File file = new File(gameDirectory + "/mods/SimpleAuth/leveldbStore");
		if (!file.exists() && !file.mkdirs())
		    throw new RuntimeException("[SimpleAuth] Error creating directory!");
		// Loading config
		config = AuthConfig.load(new File(gameDirectory + "/mods/SimpleAuth/config.json"));
		// Connecting to db
		DB.openConnection();

		try {
			serverProp.load(new FileReader(gameDirectory + "/server.properties"));
		} catch (IOException e) {
			logError("Error while reading server properties: " + e.getMessage());
		}


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
		PlayerBlockBreakEvents.BEFORE.register((world, playerEntity, blockPos, blockState, blockEntity) -> onBreakBlock(playerEntity));
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

		WriteBatch batch = DB.getLevelDBStore().createWriteBatch();
		// Updating player data.
		playerCacheMap.forEach((uuid, playerCache) -> {
			JsonObject data = new JsonObject();
			data.addProperty("password", playerCache.password);

			batch.put(bytes("UUID:" + uuid), bytes("data:" + data.toString()));
		});
		try {
			// Writing and closing batch
			DB.getLevelDBStore().write(batch);
			batch.close();
		} catch (IOException e) {
			logError("Error saving player data! " + e.getMessage());
		}

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
