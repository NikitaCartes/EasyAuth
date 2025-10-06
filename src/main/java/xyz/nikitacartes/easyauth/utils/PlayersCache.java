package xyz.nikitacartes.easyauth.utils;

import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.util.HashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.DB;
import static xyz.nikitacartes.easyauth.EasyAuth.config;

// PlayersCache for player data
public class PlayersCache {
    private static final HashMap<String, PlayerEntryV1> playerDataCache = new HashMap<>();

    public static void put(String username, PlayerEntryV1 data) {
        playerDataCache.put(username, data);
    }

    public static PlayerEntryV1 get(String username) {
        return playerDataCache.get(username);
    }

    public static PlayerEntryV1 getOrLoadOrRegister(String username) {
        PlayerEntryV1 playerEntryV1 = playerDataCache.get(username);
        // Cache should contain the player's data, but Floodgate players are not cached for some reason
        if (playerEntryV1 == null) {
            playerEntryV1 = loadOrRegister(username);
        }
        return playerEntryV1;
    }

    public static PlayerEntryV1 loadOrRegister(String username) {
        PlayerEntryV1 playerEntryV1 = DB.getUserData(username);
        if (playerEntryV1 == null) {
            playerEntryV1 = new PlayerEntryV1(username);
            if (config.offlineByDefault) {
                playerEntryV1.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
            }
            DB.registerUser(playerEntryV1);
        }
        playerDataCache.put(username, playerEntryV1);
        return playerEntryV1;
    }

    public static PlayerEntryV1 getOrCreate(String username) {
        PlayerEntryV1 data = playerDataCache.get(username);

        if (data == null) {
            data = new PlayerEntryV1(username);
            playerDataCache.put(username, data);
        }
        return data;
    }

}
