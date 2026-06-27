package xyz.nikitacartes.easyauth.utils;

import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.hashing.HasherBCrypt;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

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
        }
        // Verify password
        if (!verifyPassword(password, storedPassword)) {
            return PasswordOptions.WRONG;
        }
        return PasswordOptions.CORRECT;
    }

    public static PasswordOptions checkPassword(String username, char[] password) {
        return checkPassword(DB.getUserData(username), password);
    }

    public static PasswordOptions checkPassword(PlayerAuth player, char[] password) {
        return checkPassword(player.easyAuth$getPlayerEntryV1(), password);
    }

    /**
     * Checks if player can force login
     *
     * @param player PlayerAuth object
     * @return PasswordOptions enum
     */
    public static PasswordOptions canForceLogin(PlayerAuth player) {
        PlayerEntryV1 entryV1 = player.easyAuth$getPlayerEntryV1();
        if (entryV1 == null || entryV1.password.isEmpty()) return PasswordOptions.NOT_REGISTERED;
        if (player.easyAuth$isAuthenticated()) return PasswordOptions.WRONG;
        return PasswordOptions.CORRECT;
    }

    public static boolean checkGlobalPassword(char[] password) {
        if (!verifyPassword(password, technicalConfig.globalPassword)) return false;

        return true;
    }

    public static String hashPassword(char[] password) {
        return HasherBCrypt.hash(password);
    }

    private static boolean verifyPassword(char[] pass, String hashed) {
        if (hashed.startsWith("$argon2")) {
            LogError("Password is using Argon2 hashing algorithm, which is not supported after 3.4.3");
            LogError("You need to change password using /auth update <username> <password>");
            return false;
        }
        return HasherBCrypt.verify(pass, hashed);
    }

    public enum PasswordOptions {
        CORRECT,
        WRONG,
        NOT_REGISTERED
    }
}
