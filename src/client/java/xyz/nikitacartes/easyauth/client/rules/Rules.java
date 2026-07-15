package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Whole {@code config/easyauth-client/rules.json}: the auto-input rules keyed by normalized
 * server address. Read by {@link RuleEngine} on join and read/written by the rules screens.
 * ponytail: no cache — the engine reads once per join, the screens once per open, so edits
 * made here apply on the next join.
 */
public final class Rules {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Store {
        public Map<String, ServerRules> servers = new LinkedHashMap<>();
    }

    public static final class ServerRules {
        public List<AutoInputRule> rules = new ArrayList<>();
    }

    private Rules() {
    }

    /** Never null; creates a skeleton file on first run. */
    public static Store load(Path file) {
        Store store = null;
        try {
            if (!Files.exists(file)) {
                store = new Store();
                save(file, store);
                LOGGER.info("Created empty rules config at {}", file);
                return store;
            }
            store = GSON.fromJson(Files.readString(file), Store.class);
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
        }
        if (store == null) {
            store = new Store();
        }
        if (store.servers == null) {
            store.servers = new LinkedHashMap<>();
        }
        store.servers.values().removeIf(entry -> entry == null || entry.rules == null);
        return store;
    }

    /** Drops servers left without rules; run before saving from the rules screens. */
    public static void pruneEmpty(Store store) {
        store.servers.values().removeIf(entry -> entry.rules.isEmpty());
    }

    public static void save(Path file, Store store) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(store));
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", file, e.toString());
        }
    }
}
