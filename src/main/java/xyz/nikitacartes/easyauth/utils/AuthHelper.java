package xyz.nikitacartes.easyauth.utils;

import xyz.nikitacartes.easyauth.utils.hashing.HasherArgon2;
import xyz.nikitacartes.easyauth.utils.hashing.HasherBCrypt;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.playerCacheMap;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

public class AuthHelper {
    /**
     * Checks password of user
     *
     * @param uuid     uuid of player, stored in database
     * @param password password that needs to be checked
     * @return 1 for pass, 0 if password is false, -1 if user is not yet registered
     */
    public static PasswordOptions checkPassword(String uuid, char[] password) {
        String hashed = playerCacheMap.get(uuid).password;
        if (config.experimental.debugMode) {
            LogDebug("Checking password for " + uuid);
            LogDebug("Stored password's hash: " + hashed);
            LogDebug("Hashed password: " + hashPassword(password));
        }
        if (config.main.enableGlobalPassword) {
            // We have global password enabled
            // Player must know global password or password set by auth register
            char [] passwordCopy = password.clone();
            return (verifyPassword(password, config.main.globalPassword) || (!hashed.isEmpty() && verifyPassword(passwordCopy, hashed))) ? PasswordOptions.CORRECT : PasswordOptions.WRONG;
        } else {
            if (hashed.isEmpty())
                return PasswordOptions.NOT_REGISTERED;

            // Verify password
            return verifyPassword(password, hashed) ? PasswordOptions.CORRECT : PasswordOptions.WRONG;
        }
    }

    /**
     * Hashes password with algorithm, depending on config
     *
     * @param password character array of password string
     * @return hashed password as string
     */
    public static String hashPassword(char[] password) {
        if (config.experimental.useBCryptLibrary)
            return HasherBCrypt.hash(password);
        else
            return HasherArgon2.hash(password);
    }

    private static boolean verifyPassword(char[] pass, String hashed) {
        if (config.experimental.useBCryptLibrary)
            return HasherBCrypt.verify(pass, hashed);
        else
            return HasherArgon2.verify(pass, hashed);
    }

    public enum PasswordOptions {
        CORRECT,
        WRONG,
        NOT_REGISTERED
    }
}
