package org.samo_lego.simpleauth.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.SimpleAuth.db;

public class PlayerCache {
    public boolean isRegistered;
    public boolean wasAuthenticated;
    public String password;
    public int loginTries;
    public String lastIp;
    public long validUntil;

    public String lastDim;
    public double lastX;
    public double lastY;
    public double lastZ;

    private static final Gson gson = new Gson();
    public boolean wasInPortal;


    public PlayerCache(String uuid, ServerPlayerEntity player) {
        if(db.isClosed())
            return;

        if(player != null) {
            this.lastIp = player.getIp();

            // Getting dimension registry
            //Registry<DimensionType> registry = Objects.requireNonNull(player.getServer()).method_29435().getRegistry();

            // Setting position cache
            this.lastDim = String.valueOf(new Identifier(String.valueOf(player.getEntityWorld().getDimension())));
            this.lastX = player.getX();
            this.lastY = player.getY();
            this.lastZ = player.getZ();
        }
        else {
            this.lastIp = "";
        }

        if(db.isUserRegistered(uuid)) {
            String data = db.getData(uuid);

            // Getting (hashed) password
            JsonObject json = gson.fromJson(data, JsonObject.class);
            this.password = json.get("password").getAsString();
            this.isRegistered = true;

            // We should check the DB for saved coords
            if(config.main.spawnOnJoin) {
                try {
                    JsonElement lastLoc = json.get("lastLocation");
                    if (lastLoc != null) {
                        // Getting DB coords
                        JsonObject lastLocation = gson.fromJson(lastLoc.getAsString(), JsonObject.class);
                        //String dim = Objects.requireNonNull(Objects.requireNonNull(player).getServer()).method_29174().getRegistry().get(new Identifier(dim));
                        // Extra long line to get dimension from string
                        this.lastDim = lastLocation.get("dim").getAsString();
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
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
        this.wasAuthenticated = false;
        this.loginTries = 0;
        this.wasInPortal = false;
    }
}
