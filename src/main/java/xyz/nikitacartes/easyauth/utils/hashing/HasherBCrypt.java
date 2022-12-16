package xyz.nikitacartes.easyauth.utils.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public class HasherBCrypt {
    /**
     * Verifies password
     *
     * @param password character array of password string
     * @param hashed   hashed password
     * @return true if password was correct
     */
    public static boolean verify(char[] password, String hashed) {
        try {
            return BCrypt.verifyer().verify(password, hashed).verified;
        } catch (Error e) {
            LogError("password verification error", e);
        }
        return false;
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
            LogError("password hashing error", e);
        }
        return null;
    }
}
