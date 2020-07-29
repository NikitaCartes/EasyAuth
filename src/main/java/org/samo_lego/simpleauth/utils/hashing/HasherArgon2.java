package org.samo_lego.simpleauth.utils.hashing;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class HasherArgon2 {

    // Creating the instance
    private static final Argon2 HASHER = Argon2Factory.create();

    public static boolean verify(char[] pass, String hashed) {
        try {
            return HASHER.verify(hashed, pass);
        }
        catch (Error e) {
            logError("Argon2 password verification error: " + e);
            return false;
        } finally {
            // Wipe confidential data
            HASHER.wipeArray(pass);
        }
    }

    // Hashing the password with the Argon2 power
    public static String hash(char[] pass) {
        try {
            return HASHER.hash(10, 65536, 1, pass);
        } catch (Error e) {
            logError("Argon2 password hashing error: " + e);
        }
        return null;
    }
}
