package xyz.nikitacartes.easyauth.utils.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public class HasherBCrypt {
    private static final BCrypt.Version VERSION = BCrypt.Version.VERSION_2A;
    // Truncate: passwords <=72 bytes hash exactly as before (existing hashes stay valid), longer ones are
    // cut to 72 instead of throwing.
    private static final BCrypt.Hasher HASHER =
            BCrypt.with(LongPasswordStrategies.truncate(VERSION));
    private static final BCrypt.Verifyer VERIFYER =
            BCrypt.verifyer(VERSION, LongPasswordStrategies.truncate(VERSION));

    /**
     * Verifies password
     *
     * @param password character array of password string
     * @param hashed   hashed password
     * @return true if password was correct
     */
    public static boolean verify(char[] password, String hashed) {
        try {
            return VERIFYER.verify(password, hashed).verified;
        } catch (Exception e) {
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
            return HASHER.hashToString(12, password);
        } catch (Exception e) {
            LogError("password hashing error", e);
        }
        return null;
    }
}
