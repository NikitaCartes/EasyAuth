package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;

import static org.samo_lego.simpleauth.SimpleAuth.DB;
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

    public PlayerCache.LastLocation lastLocation = new PlayerCache.LastLocation();


    private static final Gson gson = new Gson();

    public PlayerCache(String uuid, ServerPlayerEntity player) {
        if(DB.isClosed())
            return;

        if(player != null) {
            if(config.experimental.debugMode)
                logInfo("Creating cache for " + player.getName().asString());
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

        String data = DB.getData(uuid);
        if(!data.isEmpty()) {
            // Getting (hashed) password
            JsonObject json = gson.fromJson(data, JsonObject.class);
            JsonElement passwordElement = json.get("password");
            if(passwordElement instanceof JsonNull) {
                if(player != null) {
                    player.sendMessage(new LiteralText(config.lang.corruptedPlayerData), false);
                }

                if(config.experimental.debugMode)
                    logInfo("Password for " + uuid + " is null! Marking as not registered.");
                this.password = "";
                this.isRegistered = false;
            }
            else {
                this.password = passwordElement.getAsString();
                this.isRegistered = !this.password.isEmpty();
            }
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
        this.isAuthenticated = false;
        this.loginTries = 0;

        if(config.experimental.debugMode)
            logInfo("Cache created. Registered: " + this.isRegistered + ", hashed password: " + this.password + ".");
    }
}
