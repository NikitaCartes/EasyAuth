package xyz.nikitacartes.easyauth.integrations;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.gson.JsonParser.parseString;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public class MojangApi {
    private record PremiumResult(boolean isPremium, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    // Premium status can change (name freed/claimed), so cache it for a day. UUID for a name never changes, so cache it forever.
    private static final Map<String, PremiumResult> PREMIUM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, UUID> UUID_CACHE = new ConcurrentHashMap<>();
    private static final long PREMIUM_TTL_MS = 24 * 60 * 60 * 1000L;

    public static boolean isValidUsername(String username) throws IOException {
        String key = username.toLowerCase(Locale.ENGLISH);
        PremiumResult cached = PREMIUM_CACHE.get(key);
        if (cached != null && !cached.expired()) {
            LogDebug("Player " + username + " premium status (cached): " + cached.isPremium());
            return cached.isPremium();
        }

        LogDebug("Checking player " + username + " for premium status");
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create(extendedConfig.profileLookup.url + username).toURL().openConnection();
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setConnectTimeout(extendedConfig.profileLookup.connectionTimeout);
        httpsURLConnection.setReadTimeout(extendedConfig.profileLookup.readTimeout);

        int response = httpsURLConnection.getResponseCode();
        if (response == HttpURLConnection.HTTP_OK) {
            // Player has a Mojang account
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " has a Mojang account");

            PREMIUM_CACHE.put(key, new PremiumResult(true, System.currentTimeMillis() + PREMIUM_TTL_MS));
            return true;
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            // Player doesn't have a Mojang account
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " doesn't have a Mojang account");

            PREMIUM_CACHE.put(key, new PremiumResult(false, System.currentTimeMillis() + PREMIUM_TTL_MS));
            return false;
        }

        LogDebug("Unexpected response code " + response + " for player " + username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }

    public static UUID getUuid(String username) throws IOException {
        return getUuidFrom(extendedConfig.profileLookup.url, username);
    }

    /**
     * Resolves a username against the configured alternative-auth profile-lookup URLs (e.g. ely.by).
     * Returns the first UUID a provider knows for this name, or {@code null} if none recognize it.
     * Used only to decide whether to let the vanilla online handshake proceed (so a companion mod like
     * Alternative Authentication can validate the session) instead of forcing the player offline.
     */
    public static UUID resolveAlternativeUuid(String username) {
        for (String url : extendedConfig.profileLookup.alternativeUrls) {
            try {
                UUID uuid = getUuidFrom(url, username);
                if (uuid != null) {
                    return uuid;
                }
            } catch (IOException e) {
                LogDebug("Alternative auth provider " + url + " failed for " + username + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static UUID getUuidFrom(String baseUrl, String username) throws IOException {
        String key = baseUrl + "|" + username.toLowerCase(Locale.ENGLISH);
        UUID cached = UUID_CACHE.get(key);
        if (cached != null) {
            LogDebug("Player " + username + " has UUID (cached): " + cached);
            return cached;
        }

        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create(baseUrl + username).toURL().openConnection();
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setConnectTimeout(extendedConfig.profileLookup.connectionTimeout);
        httpsURLConnection.setReadTimeout(extendedConfig.profileLookup.readTimeout);

        int response = httpsURLConnection.getResponseCode();
        if (response == HttpURLConnection.HTTP_OK) {
            String responseBody = new String(httpsURLConnection.getInputStream().readAllBytes());
            httpsURLConnection.disconnect();

            // Extract UUID from the response body.
            try {
                String uuidString = parseString(responseBody)
                        .getAsJsonObject().get("id").getAsString();
                LogDebug("Player " + username + " has UUID: " + uuidString);
                UUID uuid = UUID.fromString(uuidString.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
                UUID_CACHE.put(key, uuid);
                return uuid;
            } catch (RuntimeException e) {
                LogError("Failed to parse Mojang response for " + username);
                throw new IOException("Failed to parse Mojang response for " + username, e);
            }
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " not found");
            return null;
        }
        LogDebug("Unexpected response code " + response + " for player " + username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }
}
