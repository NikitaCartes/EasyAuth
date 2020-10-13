package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;

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
    public int lastAir;
    public boolean wasOnFire;
    public boolean wasInPortal;

    /**
     * Last recorded position before de-authentication.
     */
    public String lastDim;
    public double lastX;
    public double lastY;
    public double lastZ;

    private static final Gson gson = new Gson();

    public PlayerCache(ServerPlayerEntity player) {
        if(config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getName());
        this.isAuthenticated = false;
        this.loginTries = 0;

        if(player != null) {
            this.lastIp = player.getIp();

            this.wasOnFire = player.isOnFire();
            this.wasInPortal = player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL);
            this.lastAir = player.getAir();

            // Setting position cache
            this.lastDim = String.valueOf(player.getEntityWorld().getRegistryKey().getValue());
            this.lastX = player.getX();
            this.lastY = player.getY();
            this.lastZ = player.getZ();
        }
        else {
            this.wasOnFire = false;
            this.wasInPortal = false;
            this.lastAir = 300;
        }

        this.isRegistered = false;
        this.password = "";

        if(config.experimental.debugMode)
            logInfo("New player cache created.");
    }

    public static PlayerCache fromJson(ServerPlayerEntity player, String json) {
        if(json.isEmpty()) {
            // Player doesn't have data yet
            return new PlayerCache(player);
        }
        if(config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getName());

        // Parsing data from DB
        PlayerCache playerCache = gson.fromJson(json, PlayerCache.class);

        playerCache.loginTries = 0;
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
            playerCache.lastAir = player.getAir();
            playerCache.wasOnFire = player.isOnFire();

            // Setting position cache
            playerCache.lastDim = String.valueOf(player.getEntityWorld().getRegistryKey().getValue());
            playerCache.lastX = player.getX();
            playerCache.lastY = player.getY();
            playerCache.lastZ = player.getZ();
        }
        else {
            playerCache.wasInPortal = false;
            playerCache.lastAir = 300;
            playerCache.wasOnFire = false;
        }

        return playerCache;
    }

    public JsonObject toJson() {
        JsonObject cacheJson = new JsonObject();
        cacheJson.addProperty("password", this.password);

        JsonObject lastLocation = new JsonObject();
        lastLocation.addProperty("dim", this.lastDim);
        lastLocation.addProperty("x", this.lastX);
        lastLocation.addProperty("y", this.lastY);
        lastLocation.addProperty("z", this.lastZ);

        cacheJson.addProperty("lastLocation", lastLocation.toString());

        return cacheJson;
    }
}
