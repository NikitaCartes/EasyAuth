package org.samo_lego.simpleauth.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.hashing.HasherArgon2;
import org.samo_lego.simpleauth.utils.hashing.HasherBCrypt;

import static org.samo_lego.simpleauth.SimpleAuth.config;

public class AuthHelper {
    // Json parser
    private static final JsonParser parser = new JsonParser();

    // Returns 1 if password is correct, 0 if not
    // and -1 if user is not registered yet
    public static int checkPass(String uuid, char[] pass) {
        if(config.main.enableGlobalPassword) {
            // We have global password enabled
            return verifyPassword(pass, config.main.globalPassword) ? 1 : 0;
        }
        else {
            String hashed;
            // Password from cache
            if(SimpleAuth.deauthenticatedUsers.containsKey(uuid))
                hashed = SimpleAuth.deauthenticatedUsers.get(uuid).password;

            // Hashed password from DB
            else {
                JsonObject json = parser.parse(SimpleAuth.DB.getData(uuid)).getAsJsonObject();
                hashed = json.get("password").getAsString();
            }

            if(hashed.equals(""))
                return -1;  // User is not yet registered

            // Verify password
            return verifyPassword(pass, hashed) ? 1 : 0;
        }
    }

    public static String hashPassword(char[] pass) {
        if(config.experimental.useBCryptLibrary)
            return HasherBCrypt.hash(pass);
        else
            return HasherArgon2.hash(pass);
    }


    private static boolean verifyPassword(char[] pass, String hashed) {
        if(config.experimental.useBCryptLibrary)
            return HasherBCrypt.verify(pass, hashed);
        else
            return HasherArgon2.verify(pass, hashed);
    }
}
