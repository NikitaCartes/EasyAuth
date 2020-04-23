package org.samo_lego.simpleauth.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private static final JsonParser parser = new JsonParser();


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

        this.wasAuthenticated = false;
        this.loginTries = 0;


        if(db.isUserRegistered(uuid)) {
            this.isRegistered = true;
            JsonObject json = parser.parse(db.getData(uuid)).getAsJsonObject();
            this.password = json.get("password").getAsString();
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
    }
}
