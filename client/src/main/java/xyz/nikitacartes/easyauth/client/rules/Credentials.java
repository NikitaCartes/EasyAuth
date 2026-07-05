package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-server credentials from {@code config/easyauth-client/credentials.json}.
 * Stored as plain text for now — encryption is the last plan phase (§7);
 * the load path warns about it instead.
 */
public final class Credentials {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    public String password;
    public String totpSecret;    // Base32; presence = explicit opt-in to auto-{otp}
    public boolean autoLogin = true;     // built-in auto-/login rules
    public boolean autoRegister = false; // opt-in: registering sets the account password

    /** Whole credentials.json: global auto-auth settings plus the per-server entries. */
    public static final class Store {
        public boolean autoLogin = true;     // master switch for the built-in /login
        public boolean autoRegister = true;  // register on new servers that expose /register
        public String defaultPassword = "";  // used by auto-register; empty = random per server
        public Map<String, Credentials> servers = new LinkedHashMap<>();
    }

    /** Never null; creates a skeleton file on first run. */
    public static Store load(Path file) {
        try {
            if (!Files.exists(file)) {
                Store store = new Store();
                save(file, store);
                return store;
            }
            Store store = new GsonBuilder().create().fromJson(Files.readString(file), Store.class);
            if (store == null) {
                return new Store();
            }
            if (store.servers == null) {
                store.servers = new LinkedHashMap<>();
            }
            for (Credentials entry : store.servers.values()) {
                if (entry != null && entry.password != null && entry.password.isEmpty()) {
                    entry.password = null;
                }
            }
            return store;
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
            return new Store();
        }
    }

    public static void save(Path file, Store store) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(store));
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", file, e.toString());
        }
    }
}
