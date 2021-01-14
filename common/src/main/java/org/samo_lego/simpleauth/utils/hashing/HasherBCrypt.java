package org.samo_lego.simpleauth.utils.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class HasherBCrypt {

    /**
     * Verifies password
     *
     * @param password character array of password string
     * @param hashed hashed password
     * @return true if password was correct
     */
    public static boolean verify(char[] password, String hashed) {
        try {
            return BCrypt.verifyer().verify(password, hashed).verified;
        }
        catch (Error e) {
            logError("BCrypt password verification error: " + e);
            return false;
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
            return BCrypt.withDefaults().hashToString(12, password);
        } catch (Error e) {
            logError("BCrypt password hashing error: " + e);
        }
        return null;
    }
}
