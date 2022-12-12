package xyz.nikitacartes.easyauth.utils.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HasherBCrypt {
    private static final Logger LOGGER = LoggerFactory.getLogger(HasherBCrypt.class);

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
            LOGGER.error("password verification error", e);
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
            LOGGER.error("password hashing error", e);
        }
        return null;
    }
}
