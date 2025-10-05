package xyz.nikitacartes.easyauth.integrations;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

public class MojangApi {
    public static boolean isValidUsername(String username) throws IOException {
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

            return true;
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            // Player doesn't have a Mojang account
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " doesn't have a Mojang account");

            return false;
        }

        LogDebug("Unexpected response code " + response + " for player " + username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }

    public static UUID getUuid(String username) throws IOException {
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
            return UUID.fromString(uuidString.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
        } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
            httpsURLConnection.disconnect();
            LogDebug("Player " + username + " not found");
            return null;
        }
        LogDebug("Unexpected response code " + response + " for player " + username);
        throw new IOException("Unexpected response code " + response + " for player " + username);
    }
}
