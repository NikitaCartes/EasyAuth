package xyz.nikitacartes.easyauth.utils.hashing;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HasherArgon2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(HasherArgon2.class);

    // Creating the instance
    private static final Argon2 HASHER = Argon2Factory.create();

    /**
     * Verifies password
     *
     * @param password character array of password string
     * @param hashed   hashed password
     * @return true if password was correct
     */
    public static boolean verify(char[] password, String hashed) {
        try {
            return HASHER.verify(hashed, password);
        } catch (Error e) {
            LOGGER.error("password verification error", e);
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
            LOGGER.error("password hashing error", e);
        }
        return null;
    }
}
