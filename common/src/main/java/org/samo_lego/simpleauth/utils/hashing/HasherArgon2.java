package org.samo_lego.simpleauth.utils.hashing;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class HasherArgon2 {

    // Creating the instance
    private static final Argon2 HASHER = Argon2Factory.create();

    /**
     * Verifies password
     *
     * @param password character array of password string
     * @param hashed hashed password
     * @return true if password was correct
     */
    public static boolean verify(char[] password, String hashed) {
        try {
            return HASHER.verify(hashed, password);
        }
        catch (Error e) {
            logError("Argon2 password verification error: " + e);
            return false;
        } finally {
            // Wipe confidential data
            HASHER.wipeArray(password);
        }
    }

    /**
     * Hashes the password
     *
     * @param password character array of password string that needs to be hashed
     * @return string
     */
    public static String hash(char[] password) {
        try {
            return HASHER.hash(10, 65536, 1, password);
        } catch (Error e) {
            logError("Argon2 password hashing error: " + e);
        }
        return null;
    }
}
