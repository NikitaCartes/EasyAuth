package org.samo_lego.simpleauth.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.samo_lego.simpleauth.SimpleAuth;

public class PlayerCache {
    public boolean isRegistered;
    public boolean wasAuthenticated;
    public String password;
    public int loginTries;
    public String lastIp;
    private static final JsonParser parser = new JsonParser();


    public PlayerCache(String uuid, String ip) {
        SimpleAuthDatabase db = SimpleAuth.db;

        this.wasAuthenticated = false;
        this.loginTries = 0;
        this.lastIp = ip;

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
