package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Per-server credentials from {@code config/easyauth-client/credentials.json}.
 * Stored as plain text for now — encryption is the last plan phase (§7);
 * the load path warns about it instead.
 */
public final class Credentials {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // File writes happen off the render thread (token rotation saves on every login);
    // single-threaded so they stay ordered. ponytail: a write queued at game exit may be
    // lost — worst case the freshly rotated token, recovered by the password rung next join.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EasyAuthClient-Credentials-IO");
        thread.setDaemon(true);
        return thread;
    });

    // Single shared store: RuleEngine and the config screens mutate the same object, so a
    // whole-file save from one side no longer clobbers the other's edits. All access happens
    // on the client main thread. ponytail: hand-edits to the file while the game runs are
    // picked up only after a restart.
    private static Store cached;
    private static Path cachedFile;

    public String password;
    public String totpSecret;    // Base32; presence = explicit opt-in to auto-{otp}
    public boolean autoLogin = true;     // built-in auto-/login rules
    public boolean autoRegister = false; // opt-in: registering sets the account password
    public String sessionToken;   // "remember me" token from the server; rotated on every use
    public String passkeyPublic;  // base64 X.509/SPKI Ed25519 public key registered on the server
    public String passkeyPrivate; // base64 PKCS#8 Ed25519 private key (never leaves this machine)

    /** Whole credentials.json: global auto-auth settings plus the per-server entries. */
    public static final class Store {
        public boolean autoLogin = true;     // master switch for the built-in /login
        public boolean autoRegister = true;  // register on new servers that expose /register
        public boolean useSessionToken = true; // accept + use "remember me" session tokens
        public boolean usePasskey = true;    // enroll + use Ed25519 passkeys on supporting servers
        public String defaultPassword = "";  // used by auto-register; empty = random per server
        // Templates for the built-in auto-auth; the first word doubles as the command-tree
        // detection literal (e.g. "/reg {password}" waits for /reg). {otp} is appended to the
        // login command automatically when the server entry has a TOTP secret.
        public String loginCommand = "/login {password}";
        public String registerCommand = "/register {password} {password}";
        public Map<String, Credentials> servers = new LinkedHashMap<>();
    }

    /** Never null; creates a skeleton file on first run. */
    public static Store load(Path file) {
        if (cached != null && file.equals(cachedFile)) {
            return cached;
        }
        Store store;
        try {
            if (!Files.exists(file)) {
                store = new Store();
                save(file, store);
            } else {
                store = GSON.fromJson(Files.readString(file), Store.class);
                if (store == null) {
                    store = new Store();
                }
                if (store.servers == null) {
                    store.servers = new LinkedHashMap<>();
                }
                for (Credentials entry : store.servers.values()) {
                    if (entry != null && entry.password != null && entry.password.isEmpty()) {
                        entry.password = null;
                    }
                }
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
            store = new Store();
        }
        cached = store;
        cachedFile = file;
        return store;
    }

    /**
     * Serializes on the caller thread (consistent snapshot — the store is main-thread-only)
     * and writes the bytes on the background IO thread, so saving never freezes a frame.
     */
    public static void save(Path file, Store store) {
        cached = store;
        cachedFile = file;
        String json = GSON.toJson(store);
        IO.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, json);
            } catch (IOException e) {
                LOGGER.warn("Could not write {}: {}", file, e.toString());
            }
        });
    }
}
