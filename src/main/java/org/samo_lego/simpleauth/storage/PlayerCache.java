package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static org.samo_lego.simpleauth.SimpleAuth.DB;
import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCache {
    /**
     * Whether player is authenticated.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) session validation}.
     */
    @Expose
    public boolean isAuthenticated = false;
    /**
     * Hashed password of player.
     */
    @Expose
    public String password = "";
    /**
     * Stores how many times player has tried to login.
     */
    public int loginTries = 0;
    /**
     * Last recorded IP of player.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
     */
    @Expose
    public String lastIp;
    /**
     * Time until session is valid.
     */
    @Expose
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

    public final PlayerCache.LastLocation lastLocation = new PlayerCache.LastLocation();


    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /**
     * Creates an empty cache for player (when player doesn't exist in DB).
     *
     * @param player player to create cache for
     */
    public PlayerCache(ServerPlayerEntity player) {
        if(player != null) {
            this.lastIp = player.getIp();

            // Setting position cache
            this.lastLocation.dimension = player.getServerWorld();
            this.lastLocation.position = player.getPos();
            this.lastLocation.yaw = player.yaw;
            this.lastLocation.pitch = player.pitch;

            this.wasInPortal = player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL);
        }
    }

    public static PlayerCache fromJson(ServerPlayerEntity player, String fakeUuid) {
        if(config.experimental.debugMode)
            logInfo("Creating cache for " + Objects.requireNonNull(player).getGameProfile().getName());

        PlayerCache playerCache = new PlayerCache(player);;

        String json = DB.getUserData(fakeUuid);
        if(!json.isEmpty()) {
            // Parsing data from DB
            playerCache = gson.fromJson(json, PlayerCache.class);
        }

        return playerCache;
    }

    public String toJson() {
        return gson.toJson(this);
    }
}
