package org.samo_lego.simpleauth.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.utils.hashing.HasherArgon2;
import org.samo_lego.simpleauth.utils.hashing.HasherBCrypt;

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.SimpleAuth.playerCacheMap;

public class AuthHelper {
    // Json parser
    private static final JsonParser parser = new JsonParser();

    /**
     * Checks password of user
     *
     * @param uuid uuid of player, stored in database
     * @param password password that needs to be checked
     * @return 1 for pass, 0 if password is false, -1 if user is not yet registered
     */
    public static int checkPass(String uuid, char[] password) {
        if(config.main.enableGlobalPassword) {
            // We have global password enabled
            return verifyPassword(password, config.main.globalPassword) ? 1 : 0;
        }
        else {
            String hashed;
            // Password from cache
            if(playerCacheMap.containsKey(uuid))
                hashed = playerCacheMap.get(uuid).password;

            // Hashed password from DB
            else {
                JsonObject json = parser.parse(SimpleAuth.DB.getData(uuid)).getAsJsonObject();
                JsonElement passwordElement = json.get("password");
                if(passwordElement instanceof JsonNull) {
                    // This shouldn't have happened, data seems to be corrupted
                    return -1;
                }
                else {
                    hashed = passwordElement.getAsString();
                }
            }

            if(hashed == null)
                return -1;  // User is not yet registered

            // Verify password
            return verifyPassword(password, hashed) ? 1 : 0;
        }
    }

    /**
     * Hashes password with algorithm, depending on config
     *
     * @param password character array of password string
     * @return hashed password as string
     */
    public static String hashPassword(char[] password) {
        if(config.experimental.useBCryptLibrary)
            return HasherBCrypt.hash(password);
        else
            return HasherArgon2.hash(password);
    }

    private static boolean verifyPassword(char[] pass, String hashed) {
        if(config.experimental.useBCryptLibrary)
            return HasherBCrypt.verify(pass, hashed);
        else
            return HasherArgon2.verify(pass, hashed);
    }
}
