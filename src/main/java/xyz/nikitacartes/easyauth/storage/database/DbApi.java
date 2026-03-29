package xyz.nikitacartes.easyauth.storage.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.storage.deprecated.PlayerCacheV0;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static xyz.nikitacartes.easyauth.EasyAuth.getUnixZero;
import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

public interface DbApi {
    /**
     * Opens database connection.
     */
    void connect() throws DBApiException;

    /**
     * Closes database connection.
     */
    void close();

    /**
     * Tells whether DbApi connection is closed.
     *
     * @return false if connection is open, otherwise true
     */
    boolean isClosed();

    /**
     * Inserts the data for the player.
     *
     * @param data data to put inside database
     */
    void registerUser(PlayerEntryV1 data);

    /**
     * Gets data for the provided username.
     *
     * @param username username of the player to get data for
     * @return data if player is registered, otherwise null
     */
    @Nullable
    PlayerEntryV1 getUserData(String username);

    /**
     * Gets data for the provided username or creates a new entry if it doesn't exist.
     *
     * @param username username of the player to get data for
     * @return data if player is registered, otherwise empty PlayerEntryV1
     */
    @NotNull
    PlayerEntryV1 getUserDataOrCreate(String username);

    /**
     * Deletes data for the provided username.
     *
     * @param username username of player to delete data for
     * @return true if player data was deleted, otherwise false
     */
    boolean deleteUserData(String username);

    /**
     * Updates player's data.
     *
     * @param data data to put inside database
     * @return true if player data was updated, otherwise false
     */
    boolean updateUserData(PlayerEntryV1 data);

    /**
     * Get all data from DbApi.
     * @return HashMap with all data.
     */
    HashMap<String, PlayerEntryV1> getAllData();

    /**
     * Migrate data from v1 to v2.
     * @param userCache HashMap of usernames and UUIDs.
     */
    void migrateFromV1(HashMap<String, String> userCache);

    /**
     * Counts the number of registered accounts associated with the given IP address.
     *
     * @param ipAddress the IP address to check
     * @return the number of accounts registered with this IP
     */
    int countAccountsByIp(String ipAddress);

    /**
     * Gets all usernames associated with the given IP address.
     *
     * @param ipAddress the IP address to check
     * @return list of usernames registered with this IP
     */
    List<String> getUsernamesByIp(String ipAddress);

    default PlayerEntryV1 migrateFromV1(String data, String username) {
        String lowerCaseUsername = username.toLowerCase(Locale.ENGLISH);

        PlayerCacheV0 playerCache = PlayerCacheV0.fromJson(data);
        PlayerEntryV1 playerEntry = new PlayerEntryV1(username, lowerCaseUsername, null, data);

        ZoneOffset localOffset = ZonedDateTime.now().getOffset();
        playerEntry.lastAuthenticatedDate = LocalDateTime.ofEpochSecond(playerCache.validUntil/1000 - EasyAuth.config.sessionTimeout, 0, localOffset).atZone(localOffset);
        playerEntry.registrationDate = getUnixZero();
        if (technicalConfig.forcedOfflinePlayers.contains(lowerCaseUsername) || technicalConfig.forcedOfflinePlayers.contains(username)) {
            playerEntry.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
        } else if (technicalConfig.confirmedOnlinePlayers.contains(lowerCaseUsername) || technicalConfig.confirmedOnlinePlayers.contains(username)) {
            playerEntry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
        }

        return playerEntry;
    }

    /**
     * Migrates IP addresses from JSON to column.
     */
    void migrateFromV4();
}
