package xyz.nikitacartes.easyauth.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

/**
 * Manages IP-based account limits to prevent abuse.
 * Limits the number of accounts that can be registered/logged in from the same IP address.
 */
public class IpLimitManager {

    // Cache for IP usernames with timestamps to reduce database queries
    private record IpCacheEntry(List<String> usernames, long timestamp) {}
    private static final ConcurrentHashMap<String, IpCacheEntry> ipCache = new ConcurrentHashMap<>();

    /**
     * Checks if the given IP address has exceeded the account limit.
     *
     * @param ipAddress the IP address to check
     * @param currentUsername the username of the player attempting to register (excluded from count)
     * @return true if the IP has exceeded the limit, false otherwise
     */
    public static boolean isIpLimitExceeded(String ipAddress, String currentUsername) {
        return isIpLimitExceeded(ipAddress, currentUsername, null);
    }

    /**
     * Checks if the given IP address has exceeded the account limit.
     *
     * @param ipAddress the IP address to check
     * @param currentUsername the username of the player attempting to register (excluded from count)
     * @param playerData pre-fetched player data, or null to query from DB
     * @return true if the IP has exceeded the limit, false otherwise
     */
    public static boolean isIpLimitExceeded(String ipAddress, String currentUsername, PlayerEntryV1 playerData) {
        if (!extendedConfig.ipLimit.enabled || extendedConfig.ipLimit.maxAccountsPerIp <= 0) {
            return false;
        }

        // Check if IP is exempt
        if (isIpExempt(ipAddress)) {
            LogDebug("IP " + ipAddress + " is exempt from IP limit");
            return false;
        }

        // Short-circuit for already registered players — they should always be allowed to log in regardless of IP limit
        if (playerData == null) {
            playerData = DB.getUserData(currentUsername);
        }
        if (playerData != null && !playerData.password.isEmpty()) {
            LogDebug("User " + currentUsername + " is already registered, allowing login");
            return false;
        }

        List<String> existingUsernames = getUsernamesForIp(ipAddress);

        // Check if current user is already registered with this IP
        boolean isExistingUser = existingUsernames.stream()
                .anyMatch(name -> name.equalsIgnoreCase(currentUsername));

        if (isExistingUser) {
            // User is already registered with this IP, don't count against limit
            LogDebug("User " + currentUsername + " is already registered with IP " + ipAddress);
            return false;
        }

        boolean exceeded = existingUsernames.size() >= extendedConfig.ipLimit.maxAccountsPerIp;
        LogDebug("IP " + ipAddress + " has " + existingUsernames.size() + " accounts, limit is " +
                extendedConfig.ipLimit.maxAccountsPerIp + ", exceeded: " + exceeded);

        return exceeded;
    }

    /**
     * Gets the cached list of usernames registered with the given IP address.
     * Uses caching to reduce database queries.
     *
     * @param ipAddress the IP address to check
     * @return unmodifiable list of usernames
     */
    public static List<String> getUsernamesForIp(String ipAddress) {
        IpCacheEntry entry = ipCache.get(ipAddress);
        if (entry != null && System.currentTimeMillis() - entry.timestamp() < extendedConfig.ipLimit.cacheExpirySeconds * 1000L) {
            return entry.usernames();
        }

        List<String> usernames = Collections.unmodifiableList(DB.getUsernamesByIp(ipAddress));
        ipCache.put(ipAddress, new IpCacheEntry(usernames, System.currentTimeMillis()));
        return usernames;
    }

    /**
     * Gets the number of accounts registered with the given IP address.
     * Uses caching to reduce database queries.
     *
     * @param ipAddress the IP address to check
     * @return the number of accounts
     */
    public static int getAccountCountForIp(String ipAddress) {
        return getUsernamesForIp(ipAddress).size();
    }

    /**
     * Checks if the given IP address is exempt from the limit.
     *
     * @param ipAddress the IP address to check
     * @return true if exempt, false otherwise
     */
    public static boolean isIpExempt(String ipAddress) {
        if (ipAddress == null) {
            return true;
        }

        // Check loopback addresses
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost") ||
            ipAddress.startsWith("127.") || ipAddress.equals("::1")) {
            return true;
        }

        // Check configured exempt IPs
        return extendedConfig.ipLimit.exemptIps.contains(ipAddress);
    }

    /**
     * Notifies all online admins about an IP limit violation.
     *
     * @param server the Minecraft server
     * @param ipAddress the IP address that exceeded the limit
     * @param username the username attempting to login
     */
    public static void notifyAdmins(MinecraftServer server, String ipAddress, String username) {
        if (!extendedConfig.ipLimit.notifyAdmins || server == null) {
            return;
        }

        List<String> existingAccounts = getUsernamesForIp(ipAddress);
        String accountList = String.join(", ", existingAccounts);

        Text message = langConfig.ipLimitAdminNotify.get(username, ipAddress,
                extendedConfig.ipLimit.maxAccountsPerIp, accountList);

        LogInfo("IP limit exceeded: " + username + " from IP " + ipAddress +
                " (existing accounts: " + accountList + ")");

        // Notify all players with admin permission (op level 3+)
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (StoneCutterUtils.isOperator(server.getPlayerManager(), player)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Invalidates the cache for a specific IP address.
     * Should be called when a new account is registered or an account's IP changes.
     *
     * @param ipAddress the IP address to invalidate
     */
    public static void invalidateCache(String ipAddress) {
        ipCache.remove(ipAddress);
    }

    /**
     * Clears the entire IP account count cache.
     */
    public static void clearCache() {
        ipCache.clear();
    }

    /**
     * Counts the number of online players from the same IP address.
     *
     * @param server the Minecraft server
     * @param ipAddress the IP address to check
     * @return the number of concurrent sessions from this IP
     */
    public static int countConcurrentSessions(MinecraftServer server, String ipAddress) {
        int count = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerAuth playerAuth = (PlayerAuth) player;
            if (ipAddress.equals(playerAuth.easyAuth$getIpAddress())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if joining should be blocked because the concurrent session limit for this IP is exceeded.
     *
     * @param server the Minecraft server
     * @param ipAddress the IP address to check
     * @param isOnlinePlayer whether the joining player is a premium (online) player
     * @return true if the session limit is exceeded
     */
    public static boolean isConcurrentSessionLimitExceeded(MinecraftServer server, String ipAddress, boolean isOnlinePlayer) {
        if (!extendedConfig.ipLimit.enabled || extendedConfig.ipLimit.maxConcurrentSessionsPerIp <= 0) {
            return false;
        }

        if (isOnlinePlayer && extendedConfig.ipLimit.exemptOnlinePlayers) {
            LogDebug("Online player is exempt from concurrent session limit");
            return false;
        }

        if (isIpExempt(ipAddress)) {
            LogDebug("IP " + ipAddress + " is exempt from concurrent session limit");
            return false;
        }

        int sessions = countConcurrentSessions(server, ipAddress);
        boolean exceeded = sessions >= extendedConfig.ipLimit.maxConcurrentSessionsPerIp;
        LogDebug("IP " + ipAddress + " has " + sessions + " concurrent sessions, limit is " +
                extendedConfig.ipLimit.maxConcurrentSessionsPerIp + ", exceeded: " + exceeded);
        return exceeded;
    }

    /**
     * Checks if registration should be blocked based on IP limit settings.
     *
     * @return true if excess registrations should be blocked
     */
    public static boolean shouldBlockExcessRegistration() {
        return extendedConfig.ipLimit.enabled && extendedConfig.ipLimit.blockExcessRegistration;
    }
}
