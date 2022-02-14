package xyz.nikitacartes.easyauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.util.Objects;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.logInfo;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCache {
    /**
     * Whether player is authenticated.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayerEntity) session validation}.
     */
    @Expose
    @SerializedName("is_authenticated")
    public boolean isAuthenticated = false;
    /**
     * Hashed password of player.
     */
    @Expose
    public String password = "";
    /**
     * Stores how many times player has tried to log in.
     * Cleared on restart.
     */
    private int loginTries = 0;
    /**
     * Stores the last time a player was kicked for too many logins.
     * Persists across restarts.
     */
    @Expose
    @SerializedName("last_kicked")
    public long lastKicked;
    /**
     * Last recorded IP of player.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
     */
    @Expose
    @SerializedName("last_ip")
    public String lastIp;
    /**
     * Time until session is valid.
     */
    @Expose
    @SerializedName("valid_until")
    public long validUntil;

    /**
     * Player stats before de-authentication.
     */
    public boolean wasInPortal = false;

    /**
     * Last recorded position before de-authentication.
     */
    public static class LastLocation {
        public ServerWorld dimension;
        public Vec3d position;
        public float yaw;
        public float pitch;
    }

    public final LastLocation lastLocation = new LastLocation();


    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /**
     * Creates an empty cache for player (when player doesn't exist in DB).
     *
     * @param player player to create cache for
     */


    public static PlayerCache fromJson(ServerPlayerEntity player, String fakeUuid) {
        if (config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getGameProfile().getName());

        String json = DB.getUserData(fakeUuid);
        PlayerCache playerCache;
        if (!json.isEmpty()) {
            // Parsing data from DB
            playerCache = gson.fromJson(json, PlayerCache.class);
        } else
            playerCache = new PlayerCache();
        if (player != null) {
            // Setting position cache
            playerCache.lastLocation.dimension = player.getWorld();
            playerCache.lastLocation.position = player.getPos();
            playerCache.lastLocation.yaw = player.getYaw();
            playerCache.lastLocation.pitch = player.getPitch();

            // playerCache.wasInPortal = player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL);
            playerCache.wasInPortal = false;
        }

        return playerCache;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static boolean isAuthenticated(String uuid) {
        PlayerCache playerCache = playerCacheMap.get(uuid);
        return (playerCache != null && playerCache.isAuthenticated);
    }

    // Hide the actual login tries modifications behind synchronized functions for thread safety.
    public synchronized void incrementLoginTries() {
        loginTries++;
    }

    public synchronized void resetLoginTries() {
        loginTries = 0;
    }

    public synchronized int getLoginTries() {
        return loginTries;
    }
}
