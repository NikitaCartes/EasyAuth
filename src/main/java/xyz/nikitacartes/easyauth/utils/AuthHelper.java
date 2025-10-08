package xyz.nikitacartes.easyauth.utils;

import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.hashing.HasherArgon2;
import xyz.nikitacartes.easyauth.utils.hashing.HasherBCrypt;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

public class AuthHelper {
    /**
     * Check password using PlayerEntryV1 object
     *
     * @param playerEntry PlayerEntryV1 object
     * @param password    password that needs to be checked
     * @return PasswordOptions enum
     */
    public static PasswordOptions checkPassword(PlayerEntryV1 playerEntry, char[] password) {
        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            // We have global password enabled
            // Player must know global password if not registered
            if (checkGlobalPassword(password)) {
                return PasswordOptions.CORRECT;
            } else {
                if (playerEntry == null || playerEntry.password.isEmpty()) {
                    return PasswordOptions.WRONG;
                }
            }
        }
        if (playerEntry == null || playerEntry.password.isEmpty()) {
            return PasswordOptions.NOT_REGISTERED;
        }
        String storedPassword = playerEntry.password;
        if (config.debug) {
            LogDebug("Checking password for " + playerEntry.username);
            LogDebug("Stored password's hash: " + storedPassword);
        }
        // Verify password
        if (!verifyPassword(password, storedPassword)) {
            return PasswordOptions.WRONG;
        }
        // Rehash password if it's using Argon2
        if (storedPassword.startsWith("$argon2")) {
            playerEntry.password = HasherBCrypt.hash(password);
            playerEntry.update();
        }
        return PasswordOptions.CORRECT;
    }

    public static PasswordOptions checkPassword(String username, char[] password) {
        return checkPassword(DB.getUserData(username), password);
    }

    public static PasswordOptions checkPassword(PlayerAuth player, char[] password) {
        return checkPassword(player.easyAuth$getPlayerEntryV1(), password);
    }

    public static boolean checkGlobalPassword(char[] password) {
        if (!verifyPassword(password, technicalConfig.globalPassword)) return false;

        // Rehash password if it's using Argon2
        if (technicalConfig.globalPassword.startsWith("$argon2")) {
            technicalConfig.globalPassword = HasherBCrypt.hash(password);
            technicalConfig.save();
        }
        return true;
    }

    public static String hashPassword(char[] password) {
        return HasherBCrypt.hash(password);
    }

    private static boolean verifyPassword(char[] pass, String hashed) {
        if (hashed.startsWith("$argon2")) {
            if (config.debug) LogDebug("Hashed password (Argon2): " + HasherArgon2.hash(pass));
            return HasherArgon2.verify(pass, hashed);
        }
        if (config.debug) LogDebug("Hashed password (BCrypt): " + HasherBCrypt.hash(pass));
        return HasherBCrypt.verify(pass, hashed);
    }

    public enum PasswordOptions {
        CORRECT,
        WRONG,
        NOT_REGISTERED
    }
}
