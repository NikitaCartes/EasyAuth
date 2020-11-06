package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCache {
    /**
     * Whether player is registered.
     */
    public boolean isRegistered;
    /**
     * Whether player is authenticated.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) session validation}.
     */
    public boolean isAuthenticated;
    /**
     * Hashed password of player.
     */
    public String password;
    /**
     * Stores how many times player has tried to login.
     */
    public int loginTries;
    /**
     * Last recorded IP of player.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
     */
    public String lastIp;
    /**
     * Time until session is valid.
     */
    public long validUntil;

    /**
     * Player stats before de-authentication.
     */
    public boolean wasInPortal;

    /**
     * Last recorded position before de-authentication.
     */
    public static class LastLocation {
        public ServerWorld dimension;
        public Vec3d position;
        public float yaw;
        public float pitch;
    }

    public final PlayerCache.LastLocation lastLocation = new PlayerCache.LastLocation();


    private static final Gson gson = new Gson();

    /**
     * Creates an empty cache for player (when player doesn't exist in DB).
     *
     * @param player player to create cache for
     */
    public PlayerCache(ServerPlayerEntity player) {
        if(config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getName());
        this.isAuthenticated = false;
        this.loginTries = 0;

        if(player != null) {
            this.lastIp = player.getIp();

            // Setting position cache
            this.lastLocation.dimension = player.getServerWorld();
            this.lastLocation.position = player.getPos();
            this.lastLocation.yaw = player.yaw;
            this.lastLocation.pitch = player.pitch;

            this.wasInPortal = player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL);
        }
        else {
            this.wasInPortal = false;
        }

        this.isRegistered = false;
        this.password = "";

        if(config.experimental.debugMode)
            logInfo("New player cache created.");
    }

    public static PlayerCache fromJson(ServerPlayerEntity player, String json) {
        if(config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getName());

        // Parsing data from DB
        PlayerCache playerCache = gson.fromJson(json, PlayerCache.class);

        playerCache.loginTries = 0;
        playerCache.isAuthenticated = false;

        if(playerCache.password != null && !playerCache.password.isEmpty()) {
            playerCache.isRegistered = true;
        }
        else {
            // Not registered
            playerCache.isRegistered = false;
            playerCache.password = "";
        }
        if(player != null) {
            playerCache.lastIp = player.getIp();

            playerCache.wasInPortal = player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL);

            // Setting position cache
            playerCache.lastLocation.dimension = player.getServerWorld();
            playerCache.lastLocation.position = player.getPos();
            playerCache.lastLocation.yaw = player.yaw;
            playerCache.lastLocation.pitch = player.pitch;
        }
        else {
            playerCache.wasInPortal = false;
        }

        return playerCache;
    }

    public JsonObject toJson() {
        JsonObject cacheJson = new JsonObject();
        cacheJson.addProperty("password", this.password);

        return cacheJson;
    }
}
