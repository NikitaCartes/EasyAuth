package org.samo_lego.simpleauth;

import com.google.gson.JsonObject;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.samo_lego.simpleauth.utils.CarpetHelper.isPlayerCarpetFake;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

public class SimpleAuth implements DedicatedServerModInitializer {

    public static SimpleAuthDatabase DB = new SimpleAuthDatabase();

	public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

	private static final Timer TIMER = new Timer();

	/**
	 * HashMap of players that have joined the server.
	 * It's cleared on server stop in order to save some interactions with database during runtime.
	 * Stores their data as {@link org.samo_lego.simpleauth.storage.PlayerCache PlayerCache} object
	 */
	public static HashMap<String, PlayerCache> playerCacheMap = new HashMap<>();

	/**
	 * Checks whether player is authenticated.
	 * Fake players always count as authenticated.
	 *
	 * @param player player that needs to be checked
	 * @return false if player is not authenticated, otherwise true
	 */
	public static boolean isAuthenticated(ServerPlayerEntity player) {
		String uuid = convertUuid(player);
		return playerCacheMap.containsKey(uuid) && playerCacheMap.get(uuid).isAuthenticated;
	}

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
		AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> AuthEventHandler.onAttackBlock(playerEntity));
		UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> AuthEventHandler.onUseBlock(player));
		UseItemCallback.EVENT.register((player, world, hand) -> AuthEventHandler.onUseItem(player));
		AttackEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onAttackEntity(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, entityHitResult) -> AuthEventHandler.onUseEntity(player));
		ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, serverResourceManager) -> AuthCommand.reloadConfig(null));
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

			JsonObject lastLocation = new JsonObject();
			lastLocation.addProperty("dim", playerCache.lastDim);
			lastLocation.addProperty("x", playerCache.lastX);
			lastLocation.addProperty("y", playerCache.lastY);
			lastLocation.addProperty("z", playerCache.lastZ);

			data.addProperty("lastLocation", lastLocation.toString());

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
			TIMER.cancel();
			TIMER.purge();
            if (!THREADPOOL.awaitTermination(100, TimeUnit.MICROSECONDS)) {
				Thread.currentThread().interrupt();
            }
        } catch (InterruptedException e) {
		    logError(e.getMessage());
			THREADPOOL.shutdownNow();
        }

        // Closing DB connection
		DB.close();
	}

	/**
	 * Gets the text which tells the player
	 * to login or register, depending on account status.
	 *
	 * @param player player who will get the message
	 * @return LiteralText with appropriate string (login or register)
	 */
	public static LiteralText notAuthenticated(PlayerEntity player) {
        final PlayerCache cache = playerCacheMap.get(convertUuid(player));
        if(SimpleAuth.config.main.enableGlobalPassword || cache.isRegistered)
			return new LiteralText(
			        SimpleAuth.config.lang.notAuthenticated + "\n" + SimpleAuth.config.lang.loginRequired
            );
		return new LiteralText(
		        SimpleAuth.config.lang.notAuthenticated+ "\n" + SimpleAuth.config.lang.registerRequired
        );
	}

	/**
	 * Authenticates player and sends the success message.
	 *
	 * @param player player that needs to be authenticated
	 * @param msg message to be send to the player
	 */
	public static void authenticatePlayer(ServerPlayerEntity player, Text msg) {
		PlayerCache playerCache = playerCacheMap.get(convertUuid(player));
		// Teleporting player back
		if(config.main.spawnOnJoin)
			teleportPlayer(player, false);

		// Updating blocks if needed (if portal rescue action happened)
		if(playerCache.wasInPortal) {
			World world = player.getEntityWorld();
			BlockPos pos = player.getBlockPos();

			// Sending updates to portal blocks
			// This is technically not needed, but it cleans the "messed portal" on the client
			world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
			world.updateListeners(pos.up(), world.getBlockState(pos.up()), world.getBlockState(pos.up()), 3);
		}

		// Setting last air to player
		if(player.isSubmergedInWater())
			player.setAir(playerCache.lastAir);

		// In case player is in lava during authentication proccess
		if(!playerCache.wasOnFire)
			player.setFireTicks(0);

		playerCache.isAuthenticated = true;

		// Player no longer needs to be invisible and invulnerable
		player.setInvulnerable(false);
		player.setInvisible(false);
		player.sendMessage(msg, false);
	}

	/**
	 * De-authenticates the player.
	 *
	 * @param player player that needs to be de-authenticated
	 */
	public static void deauthenticatePlayer(ServerPlayerEntity player) {
		if(DB.isClosed())
			return;

		// Marking player as not authenticated
		String uuid = convertUuid(player);
		playerCacheMap.put(uuid, new PlayerCache(uuid, player));
		playerCacheMap.get(uuid).isAuthenticated = false;

		// Teleporting player to spawn to hide its position
		if(config.main.spawnOnJoin)
			teleportPlayer(player, true);

		// Player is now not authenticated
		player.sendMessage(notAuthenticated(player), false);

		// Setting the player to be invisible to mobs and also invulnerable
		player.setInvulnerable(SimpleAuth.config.experimental.playerInvulnerable);
		player.setInvisible(SimpleAuth.config.experimental.playerInvisible);

		// Kicks player after a certain amount of time if not authenticated
		TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				// Kicking player if not authenticated
				if(!isAuthenticated(player) && player.networkHandler.getConnection().isOpen())
					player.networkHandler.disconnect(new LiteralText(SimpleAuth.config.lang.timeExpired));
			}
		}, SimpleAuth.config.main.kickTime * 1000);
	}

	/**
	 * Checks whether player is a fake player (from CarpetMod).
	 *
	 * @param player player that needs to be checked
	 * @return true if player is fake, otherwise false
	 */
	public static boolean isPlayerFake(PlayerEntity player) {
		// We ask CarpetHelper class since it has the imports needed
		return FabricLoader.getInstance().isModLoaded("carpet") && isPlayerCarpetFake(player);
	}

	/**
	 * Teleports player to spawn or last location that is recorded.
	 * Last location means the location before de-authentication.
	 *
	 * @param player player that needs to be teleported
	 * @param toSpawn whether to teleport player to spawn (provided in config) or last recorded position
	 */
	public static void teleportPlayer(ServerPlayerEntity player, boolean toSpawn) {
		MinecraftServer server = player.getServer();
		if(server == null || config.worldSpawn.dimension == null)
			return;

		if (toSpawn) {
			// Teleports player to spawn
			player.teleport(
					server.getWorld(RegistryKey.of(Registry.DIMENSION, new Identifier(config.worldSpawn.dimension))),
					config.worldSpawn.x,
					config.worldSpawn.y,
					config.worldSpawn.z,
					90,
					90
			);
			return;
		}
		PlayerCache cache = playerCacheMap.get(convertUuid(player));
		// Puts player to last cached position
		try {
			player.teleport(
					server.getWorld(RegistryKey.of(Registry.DIMENSION, new Identifier(cache.lastDim))),
					cache.lastX,
					cache.lastY,
					cache.lastZ,
					0,
					0
			);
		} catch (Error e) {
			player.sendMessage(new LiteralText(config.lang.corruptedPlayerData), false);
			logError("Couldn't teleport player " + player.getName().asString());
			logError(
				String.format("Last recorded position is X: %s, Y: %s, Z: %s in dimension %s",
				cache.lastX,
				cache.lastY,
				cache.lastZ,
				cache.lastDim
			));
		}
	}
}
