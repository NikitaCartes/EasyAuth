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

    public static boolean checkPass(String uuid, char[] pass) {
        try {
            // Hashed password from DB
            String hashed = SimpleAuth.db.getPassword(uuid);
            // Verify password
            return argon2.verify(hashed, pass);
        } catch(Error e) {
            LOGGER.error("SimpleAut error: " + e);
        } finally {
            // Wipe confidential data
            argon2.wipeArray(pass);
        }
        return false;
    }
    // Hashing the password with the Argon2 power
    public static String hashPass(char[] pass) {
        try {
            return argon2.hash(10, 65536, 1, pass);
        } catch (Error e) {
            LOGGER.error(e);
        }
        return null;
    }
}
