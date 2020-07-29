package org.samo_lego.simpleauth.utils.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

public class HasherBCrypt {

    public static boolean verify(char[] pass, String hashed) {
        try {
            return BCrypt.verifyer().verify(pass, hashed).verified;
        }
        catch (Error e) {
            logError("BCrypt password verification error: " + e);
            return false;
        }
    }

    // Hashing the password with the Argon2 power
    public static String hash(char[] pass) {
        try {
            return BCrypt.withDefaults().hashToString(12, pass);
        } catch (Error e) {
            logError("BCrypt password hashing error: " + e);
        }
        return null;
    }
}
