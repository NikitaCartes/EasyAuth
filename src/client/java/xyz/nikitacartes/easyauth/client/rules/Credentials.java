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
import java.util.concurrent.TimeUnit;

/**
 * Per-server credentials from {@code config/easyauth-client/credentials.json}.
 * Secret fields (password, TOTP secret, session token, passkey private key, default
 * password) are encrypted at rest via {@link Vault}: decrypted in place on load, encrypted
 * on a deep copy on save (the live store stays plaintext for consumers). While the vault is
 * locked they stay {@code enc:v1:…} strings in memory — consumers go through
 * {@link Vault#usable} and treat them as unavailable.
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
        // detection literal (e.g. "/reg {password}" waits for /reg).
        public String loginCommand = "/login {password}";
        public String registerCommand = "/register {password} {password}";
        // Separate 2FA command, for servers whose login command takes no code (AuthMe wants
        // "/totp code {otp}" after a bare /login). Empty = EasyAuth's own syntax: {otp} is
        // appended to the login command instead when the server entry has a TOTP secret.
        public String totpCommand = "";
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
                if (decryptStore(store) && Vault.encryptionEnabled() && !Vault.locked()) {
                    save(file, store); // one-time migration: plaintext secrets → encrypted
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

    /** Drops server entries with no stored secrets; run before saving from the config screens. */
    public static void pruneEmpty(Store store) {
        store.servers.values().removeIf(entry -> entry.password == null && entry.totpSecret == null
                && entry.sessionToken == null && entry.passkeyPrivate == null);
    }

    /** Called after the vault is unlocked: decrypts the cached store in place and re-saves (migrates leftovers). */
    public static void onUnlocked() {
        if (cached != null) {
            decryptStore(cached);
            save(cachedFile, cached);
        }
    }

    /** True for a secret that sits on disk unencrypted (needs migration). */
    private static boolean plaintextSecret(String value) {
        return value != null && !value.isEmpty() && !Vault.isEncrypted(value);
    }

    /** Decrypts secret fields in place; returns true when any plaintext secret was found. */
    private static boolean decryptStore(Store store) {
        boolean plaintext = plaintextSecret(store.defaultPassword);
        store.defaultPassword = Vault.decrypt(store.defaultPassword);
        for (Credentials entry : store.servers.values()) {
            if (entry == null) {
                continue;
            }
            plaintext |= plaintextSecret(entry.password) || plaintextSecret(entry.totpSecret)
                    || plaintextSecret(entry.sessionToken) || plaintextSecret(entry.passkeyPrivate);
            entry.password = Vault.decrypt(entry.password);
            entry.totpSecret = Vault.decrypt(entry.totpSecret);
            entry.sessionToken = Vault.decrypt(entry.sessionToken);
            entry.passkeyPrivate = Vault.decrypt(entry.passkeyPrivate);
        }
        return plaintext;
    }

    /** Deep copy with secret fields encrypted; the live store stays plaintext for consumers. */
    private static Store encryptedCopy(Store store) {
        Store copy = GSON.fromJson(GSON.toJsonTree(store), Store.class);
        copy.defaultPassword = Vault.encrypt(copy.defaultPassword);
        for (Credentials entry : copy.servers.values()) {
            if (entry == null) {
                continue;
            }
            entry.password = Vault.encrypt(entry.password);
            entry.totpSecret = Vault.encrypt(entry.totpSecret);
            entry.sessionToken = Vault.encrypt(entry.sessionToken);
            entry.passkeyPrivate = Vault.encrypt(entry.passkeyPrivate);
        }
        return copy;
    }

    /**
     * Serializes on the caller thread (consistent snapshot — the store is main-thread-only)
     * and writes the bytes on the background IO thread, so saving never freezes a frame.
     */
    public static void save(Path file, Store store) {
        cached = store;
        cachedFile = file;
        String json = GSON.toJson(encryptedCopy(store));
        IO.execute(() -> writeString(file, json));
    }

    /**
     * Drains the IO queue and writes synchronously. Used before a storage-location switch,
     * which copies the file right after — an in-flight async write would race the copy.
     */
    public static void saveNow(Path file, Store store) {
        cached = store;
        cachedFile = file;
        String json = GSON.toJson(encryptedCopy(store));
        try {
            IO.submit(() -> {
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.warn("Credentials IO queue did not drain: {}", e.toString());
        }
        writeString(file, json);
    }

    private static void writeString(Path file, String json) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", file, e.toString());
        }
    }
}
