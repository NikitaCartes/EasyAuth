package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Per-server credentials from {@code config/easyauth-client/credentials.json}.
 * Stored as plain text for now — encryption is the last plan phase (§7);
 * the load path warns about it instead.
 */
public final class Credentials {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");
    private static final Gson GSON = new Gson();

    public String password;
    public String totpSecret;    // Base32; presence = explicit opt-in to auto-{otp}
    public boolean autoLogin = true;     // built-in auto-/login rules
    public boolean autoRegister = false; // opt-in: registering sets the account password

    /** Returns the entry for {@code address} or null; creates a skeleton file if missing. */
    static Credentials load(Path file, String address) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "{\n  \"servers\": {}\n}\n");
                return null;
            }
            CredentialsFile parsed = GSON.fromJson(Files.readString(file), CredentialsFile.class);
            if (parsed == null || parsed.servers == null) {
                return null;
            }
            Credentials entry = parsed.servers.get(address);
            if (entry != null && (entry.password == null || entry.password.isEmpty())) {
                entry.password = null;
            }
            if (entry != null && entry.password != null) {
                LOGGER.warn("Using credentials for {} from {} — this file is stored as plain text", address, file);
            }
            return entry;
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
            return null;
        }
    }

    private static final class CredentialsFile {
        Map<String, Credentials> servers;
    }
}
