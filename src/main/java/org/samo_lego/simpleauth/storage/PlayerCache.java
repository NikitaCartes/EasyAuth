package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.network.ServerPlayerEntity;

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.SimpleAuth.db;

public class PlayerCache {
    public boolean isRegistered;
    public boolean wasAuthenticated;
    public String password;
    public int loginTries;
    public String lastIp;
    public long validUntil;

    public int lastDimId;
    public double lastX;
    public double lastY;
    public double lastZ;

    private static final Gson gson = new Gson();


    public PlayerCache(String uuid, ServerPlayerEntity player) {
        if(db.isClosed())
            return;

        if(player != null) {
            this.lastIp = player.getIp();

            // Setting last coordinates
            this.lastDimId = player.dimension.getRawId();
            this.lastX = player.getX();
            this.lastY = player.getY();
            this.lastZ = player.getZ();
        }
        else {
            this.lastIp = "";

            // Setting last coordinates
            this.lastDimId = config.worldSpawn.dimensionId;
            this.lastX = config.worldSpawn.x;
            this.lastY = config.worldSpawn.y;
            this.lastZ = config.worldSpawn.z;
        }

        if(db.isUserRegistered(uuid)) {
            String data = db.getData(uuid);

            // Getting (hashed) password
            JsonObject json = gson.fromJson(data, JsonObject.class);
            this.password = json.get("password").getAsString();

            // If coordinates are same as the one from world spawn
            // we should check the DB for saved coords
            if(config.main.spawnOnJoin) {
                try {
                    JsonElement lastLoc = json.get("lastLocation");
                    if (
                            lastLoc != null &&
                            this.lastDimId == config.worldSpawn.dimensionId &&
                            this.lastX == config.worldSpawn.x &&
                            this.lastY == config.worldSpawn.y &&
                            this.lastZ == config.worldSpawn.z
                    ) {
                        // Getting DB coords
                        JsonObject lastLocation = gson.fromJson(lastLoc.getAsString(), JsonObject.class);
                        this.lastDimId = lastLocation.get("dimId").getAsInt();
                        this.lastX = lastLocation.get("x").getAsDouble();
                        this.lastY = lastLocation.get("y").getAsDouble();
                        this.lastZ = lastLocation.get("z").getAsDouble();

                        // Removing location data from DB
                        json.remove("lastLocation");
                        db.updateUserData(uuid, json.toString());
                    }
                } catch (JsonSyntaxException ignored) {
                    // Player didn't have any coords in db to tp to
                }
            }
            this.isRegistered = true;
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
        this.wasAuthenticated = false;
        this.loginTries = 0;
    }
}
