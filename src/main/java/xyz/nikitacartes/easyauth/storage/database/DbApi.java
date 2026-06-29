package xyz.nikitacartes.easyauth.storage.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.util.HashMap;
import java.util.List;

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

    /**
     * Returns the username that owns the given UUID, or null if none.
     * Used to detect forced-UUID collisions.
     *
     * @param uuid the UUID (canonical string form) to look up
     * @return the owning username, or null if the UUID is unused
     */
    @Nullable
    String getUsernameByUuid(String uuid);

    /**
     * Migrates IP addresses from JSON to column.
     */
    void migrateFromV4();

    /**
     * Backfills the online_account mirror column from the JSON blob for existing rows.
     */
    void migrateFromV9();
}
