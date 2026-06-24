package xyz.nikitacartes.easyauth.integrations;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

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
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create(extendedConfig.mojangApiSettings.url + username).toURL().openConnection();
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setConnectTimeout(extendedConfig.mojangApiSettings.connectionTimeout);
        httpsURLConnection.setReadTimeout(extendedConfig.mojangApiSettings.readTimeout);

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
        String key = username.toLowerCase(Locale.ENGLISH);
        UUID cached = UUID_CACHE.get(key);
        if (cached != null) {
            LogDebug("Player " + username + " has UUID (cached): " + cached);
            return cached;
        }

        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create(extendedConfig.mojangApiSettings.url + username).toURL().openConnection();
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setConnectTimeout(extendedConfig.mojangApiSettings.connectionTimeout);
        httpsURLConnection.setReadTimeout(extendedConfig.mojangApiSettings.readTimeout);

        int response = httpsURLConnection.getResponseCode();
        if (response == HttpURLConnection.HTTP_OK) {
            String responseBody = new String(httpsURLConnection.getInputStream().readAllBytes());
            httpsURLConnection.disconnect();

            // Extract UUID from the response body
            String uuidString = responseBody.split("\"id\" : \"")[1].split("\"")[0];
            LogDebug("Player " + username + " has UUID: " + uuidString);
            UUID uuid = UUID.fromString(uuidString.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
            UUID_CACHE.put(key, uuid);
            return uuid;
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " not found");
            return null;
        }
        LogDebug("Unexpected response code " + response + " for player " + username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }
}
