package org.samo_lego.simpleauth.storage;

import org.samo_lego.simpleauth.SimpleAuth;

public class PlayerCache {
    public boolean isRegistered;
    public boolean isAuthenticated;
    public String password;
    public int loginTries;

    public PlayerCache(String uuid) {
        SimpleAuthDatabase db = SimpleAuth.db;

        this.isAuthenticated = false;
        this.loginTries = 0;
        if(db.isUserRegistered(uuid)) {
            this.isRegistered = true;
            this.password = db.getPassword(uuid);
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
    }
}
