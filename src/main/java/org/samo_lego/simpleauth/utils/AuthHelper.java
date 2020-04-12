package org.samo_lego.simpleauth.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.SimpleAuth;

public class AuthHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    // Creating the instance
    private static Argon2 argon2 = Argon2Factory.create();

    // Returns 1 if password is correct, 0 if not
    // and -1 if user is not registered yet
    public static int checkPass(String uuid, char[] pass) {
        if(SimpleAuth.config.main.enableGlobalPassword) {
            // We have global password enabled
            try {
                return argon2.verify(SimpleAuth.config.main.globalPassword, pass) ? 1 : 0;
            }
            catch (Error e) {
                LOGGER.error("[SimpleAuth] Argon2 error: " + e);
                return 0;
            } finally {
                // Wipe confidential data
                argon2.wipeArray(pass);
            }
        }
        else {
            try {
                // Hashed password from DB
                String hashed = SimpleAuth.db.getPassword(uuid);
                if(hashed.equals(""))
                    return -1;  // User is not yet registered
                // Verify password
                return argon2.verify(hashed, pass) ? 1 : 0;
            } catch (Error e) {
                LOGGER.error("[SimpleAuth] error: " + e);
                return 0;
            } finally {
                // Wipe confidential data
                argon2.wipeArray(pass);
            }
        }
    }
    // Hashing the password with the Argon2 power
    public static String hashPass(char[] pass) {
        try {
            return argon2.hash(10, 65536, 1, pass);
        } catch (Error e) {
            LOGGER.error("[SimpleAuth] " + e);
        }
        return null;
    }
}
