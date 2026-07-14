package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.sun.jna.platform.win32.Crypt32Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * Encryption at rest for the secret fields of credentials.json, plus the storage
 * locations. Secrets are encrypted with AES-256-GCM under one random data key. The key lives
 * in a file OUTSIDE the instance (default: the user's home directory), so an exported modpack,
 * a synced instance or a zipped config folder contains only ciphertext. The key file itself is
 * protected by the best available rung:
 * <ul>
 *   <li>Windows: DPAPI, tying it to the OS account (jna-platform ships with Minecraft);</li>
 *   <li>elsewhere: a plain key file with 0600 permissions;</li>
 *   <li>any platform, opt-in: a master password (PBKDF2-wrapped, asked once per launch).</li>
 * </ul>
 * Nothing local protects against malware running as the same user — that is the ceiling of
 * every option, OS keystores included.
 *
 * <p>Both the data directory (credentials.json + rules.json) and the key file path can be
 * redirected via {@code config/easyauth-client/storage.properties} so several instances share
 * one store. All access happens on the client main thread.
 */
public final class Vault {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ENC_PREFIX = "enc:v1:";
    private static final int GCM_NONCE_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 600_000;
    static final String MODE_DPAPI = "dpapi";
    static final String MODE_PLAIN = "plain";
    static final String MODE_PASSWORD = "password";

    private static Path instanceDir; // config/easyauth-client of this instance
    private static Path dataDir;     // where credentials.json/rules.json live (may be shared)
    private static Path keyFile;
    private static String dataDirOverride = "";
    private static String keyFileOverride = "";

    private static boolean keyLoaded;
    private static SecretKey key;             // null while locked
    private static KeyFileData loadedKeyData; // parsed key file, kept for unlock/rewrap
    private static boolean promptShown;       // the once-per-launch multiplayer-screen prompt

    /** Key file contents. {@code salt}/{@code iterations} are only set in password mode. */
    private static final class KeyFileData {
        int v = 1;
        String mode;
        String key;
        String salt;
        Integer iterations;
    }

    private Vault() {
    }

    public static void init(Path easyAuthClientDir) {
        instanceDir = easyAuthClientDir;
        Properties props = new Properties();
        Path pointer = pointerFile();
        if (Files.exists(pointer)) {
            try (var in = Files.newBufferedReader(pointer)) {
                props.load(in);
            } catch (IOException e) {
                LOGGER.warn("Could not read {}: {}", pointer, e.toString());
            }
        }
        dataDirOverride = props.getProperty("data-dir", "").trim();
        keyFileOverride = props.getProperty("key-file", "").trim();
        applyLocations();
    }

    private static Path pointerFile() {
        return instanceDir.resolve("storage.properties");
    }

    private static void applyLocations() {
        dataDir = dataDirOverride.isEmpty() ? instanceDir : Path.of(dataDirOverride);
        keyFile = keyFileOverride.isEmpty() ? defaultKeyFile() : Path.of(keyFileOverride);
        keyLoaded = false;
        key = null;
        loadedKeyData = null;
    }

    public static Path defaultDataDir() {
        return instanceDir;
    }

    public static Path defaultKeyFile() {
        return Path.of(System.getProperty("user.home"), ".easyauth-client.key");
    }

    public static Path credentialsFile() {
        return dataDir.resolve("credentials.json");
    }

    public static Path rulesFile() {
        return dataDir.resolve("rules.json");
    }

    /** Raw override strings for the storage screen; empty = default location. */
    public static String dataDirOverride() {
        return dataDirOverride;
    }

    public static String keyFileOverride() {
        return keyFileOverride;
    }

    /**
     * Applies new storage locations (empty string = default), copying the existing files to a
     * target that lacks them, and persists the per-instance pointer file. The caller must flush
     * pending credential edits first — the copy reads what is on disk.
     */
    public static boolean setLocations(String newDataDir, String newKeyFile) {
        String dataOverride = newDataDir == null ? "" : newDataDir.trim();
        String keyOverride = newKeyFile == null ? "" : newKeyFile.trim();
        Path oldCredentials = credentialsFile();
        Path oldRules = rulesFile();
        Path oldKey = keyFile;
        Path newData = dataOverride.isEmpty() ? instanceDir : Path.of(dataOverride);
        Path newKey = keyOverride.isEmpty() ? defaultKeyFile() : Path.of(keyOverride);
        try {
            Files.createDirectories(newData);
            copyIfTargetMissing(oldCredentials, newData.resolve("credentials.json"));
            copyIfTargetMissing(oldRules, newData.resolve("rules.json"));
            if (newKey.getParent() != null) {
                Files.createDirectories(newKey.getParent());
            }
            copyIfTargetMissing(oldKey, newKey);
        } catch (IOException | InvalidPathException e) {
            LOGGER.warn("Could not switch storage location: {}", e.toString());
            return false;
        }
        dataDirOverride = dataOverride;
        keyFileOverride = keyOverride;
        Properties props = new Properties();
        if (!dataOverride.isEmpty()) {
            props.setProperty("data-dir", dataOverride);
        }
        if (!keyOverride.isEmpty()) {
            props.setProperty("key-file", keyOverride);
        }
        try {
            Files.createDirectories(instanceDir);
            try (var out = Files.newBufferedWriter(pointerFile())) {
                props.store(out, "EasyAuth Client storage locations (shared between instances when pointed at the same paths)");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", pointerFile(), e.toString());
        }
        applyLocations();
        LOGGER.info("EasyAuth Client storage: data at {}, key file at {}", dataDir, keyFile);
        return true;
    }

    private static void copyIfTargetMissing(Path from, Path to) throws IOException {
        if (!from.equals(to) && Files.exists(from) && !Files.exists(to)) {
            Files.copy(from, to);
        }
    }

    // --- key handling ---

    private static void ensureKeyLoaded() {
        if (keyLoaded) {
            return;
        }
        keyLoaded = true;
        key = null;
        loadedKeyData = null;
        try {
            if (!Files.exists(keyFile)) {
                createNewKey();
                return;
            }
            KeyFileData data = GSON.fromJson(Files.readString(keyFile), KeyFileData.class);
            if (data == null || data.mode == null || data.key == null) {
                LOGGER.warn("Key file {} is malformed; stored secrets stay locked", keyFile);
                return;
            }
            loadedKeyData = data;
            switch (data.mode) {
                case MODE_PLAIN -> key = new SecretKeySpec(Base64.getDecoder().decode(data.key), "AES");
                case MODE_DPAPI -> {
                    byte[] raw = dpapiUnprotect(Base64.getDecoder().decode(data.key));
                    if (raw != null) {
                        key = new SecretKeySpec(raw, "AES");
                    } else {
                        LOGGER.warn("Key file {} could not be unwrapped with DPAPI (different user or computer?); stored secrets stay locked", keyFile);
                    }
                }
                case MODE_PASSWORD -> {
                    // locked until unlock(); the once-per-launch prompt handles it
                }
                default -> LOGGER.warn("Key file {} has unknown mode '{}'; stored secrets stay locked", keyFile, data.mode);
            }
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            LOGGER.warn("Could not read key file {}: {}", keyFile, e.toString());
        }
    }

    private static void createNewKey() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        KeyFileData data = wrapAuto(raw);
        if (writeKeyFile(data)) {
            key = new SecretKeySpec(raw, "AES");
            loadedKeyData = data;
            LOGGER.info("Created encryption key file {} ({})", keyFile, data.mode);
        } else {
            // Never encrypt with a key that was not persisted — those secrets would be
            // unreadable next launch. Staying locked keeps everything plaintext-but-working.
            LOGGER.warn("Stored secrets will not be encrypted until the key file is writable");
        }
    }

    /** Packages a raw key with the best automatic protection: DPAPI on Windows, plain elsewhere. */
    private static KeyFileData wrapAuto(byte[] raw) {
        KeyFileData data = new KeyFileData();
        byte[] blob = System.getProperty("os.name", "").startsWith("Windows") ? dpapiProtect(raw) : null;
        if (blob != null) {
            data.mode = MODE_DPAPI;
            data.key = Base64.getEncoder().encodeToString(blob);
        } else {
            data.mode = MODE_PLAIN;
            data.key = Base64.getEncoder().encodeToString(raw);
        }
        return data;
    }

    private static byte[] dpapiProtect(byte[] data) {
        try {
            return Crypt32Util.cryptProtectData(data);
        } catch (Throwable t) { // no JNA in this runtime, or the call itself failed
            LOGGER.warn("DPAPI unavailable ({}); falling back to a plain key file", t.toString());
            return null;
        }
    }

    private static byte[] dpapiUnprotect(byte[] data) {
        try {
            return Crypt32Util.cryptUnprotectData(data);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean writeKeyFile(KeyFileData data) {
        try {
            if (keyFile.getParent() != null) {
                Files.createDirectories(keyFile.getParent());
            }
            Files.writeString(keyFile, GSON.toJson(data));
            try {
                Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) { // Windows
            }
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not write key file {}: {}", keyFile, e.toString());
            return false;
        }
    }

    /** True while the data key is unavailable: master password not entered yet, or the key file is unreadable/foreign. */
    public static boolean locked() {
        ensureKeyLoaded();
        return key == null;
    }

    /** True when the key file is master-password protected (regardless of unlock state). */
    public static boolean passwordProtected() {
        ensureKeyLoaded();
        return loadedKeyData != null && MODE_PASSWORD.equals(loadedKeyData.mode);
    }

    /** Tries the master password against the key file; false = wrong password (or not password mode). */
    public static boolean unlock(String password) {
        ensureKeyLoaded();
        if (key != null) {
            return true;
        }
        KeyFileData data = loadedKeyData;
        if (data == null || !MODE_PASSWORD.equals(data.mode) || data.salt == null) {
            return false;
        }
        try {
            int iterations = data.iterations == null || data.iterations <= 0 ? PBKDF2_ITERATIONS : data.iterations;
            SecretKey kek = deriveKek(password, Base64.getDecoder().decode(data.salt), iterations);
            byte[] raw = gcmDecrypt(Base64.getDecoder().decode(data.key), kek);
            key = new SecretKeySpec(raw, "AES");
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false; // wrong password (GCM tag mismatch) or a corrupt key file
        }
    }

    /** Wraps the data key under a master password (PBKDF2). Requires the vault to be unlocked. */
    public static boolean setMasterPassword(String password) {
        ensureKeyLoaded();
        if (key == null) {
            return false;
        }
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            SecretKey kek = deriveKek(password, salt, PBKDF2_ITERATIONS);
            KeyFileData data = new KeyFileData();
            data.mode = MODE_PASSWORD;
            data.salt = Base64.getEncoder().encodeToString(salt);
            data.iterations = PBKDF2_ITERATIONS;
            data.key = Base64.getEncoder().encodeToString(gcmEncrypt(key.getEncoded(), kek));
            if (!writeKeyFile(data)) {
                return false;
            }
            loadedKeyData = data;
            return true;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Could not set the master password: {}", e.toString());
            return false;
        }
    }

    /** Back to automatic protection (DPAPI or a plain key file). Requires the vault to be unlocked. */
    public static boolean clearMasterPassword() {
        ensureKeyLoaded();
        if (key == null) {
            return false;
        }
        KeyFileData data = wrapAuto(key.getEncoded());
        if (!writeKeyFile(data)) {
            return false;
        }
        loadedKeyData = data;
        return true;
    }

    /** One-shot per launch: prompt at the multiplayer screen only for password-protected locked vaults. */
    public static boolean shouldPromptUnlock() {
        return !promptShown && passwordProtected() && locked();
    }

    public static void markPrompted() {
        promptShown = true;
    }

    // --- field crypto ---

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }

    /** The plaintext of a secret field, or null when the field is absent or still-locked ciphertext. */
    public static String usable(String value) {
        return isEncrypted(value) ? null : value;
    }

    /** Plaintext → {@code enc:v1:…}; returns the input unchanged when locked (callers gate on that). */
    static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        ensureKeyLoaded();
        if (key == null) {
            return plain;
        }
        try {
            return ENC_PREFIX + Base64.getEncoder().encodeToString(gcmEncrypt(plain.getBytes(StandardCharsets.UTF_8), key));
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Could not encrypt a credential field: {}", e.toString());
            return plain;
        }
    }

    /** {@code enc:v1:…} → plaintext; returns the input unchanged when locked or undecryptable (foreign key). */
    static String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        ensureKeyLoaded();
        if (key == null) {
            return stored;
        }
        try {
            byte[] blob = Base64.getDecoder().decode(stored.substring(ENC_PREFIX.length()));
            return new String(gcmDecrypt(blob, key), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            LOGGER.warn("Could not decrypt a credential field (recreated or foreign key file?); leaving it locked");
            return stored;
        }
    }

    private static byte[] gcmEncrypt(byte[] plain, SecretKey key) throws GeneralSecurityException {
        byte[] nonce = new byte[GCM_NONCE_LEN];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] ciphertext = cipher.doFinal(plain);
        byte[] out = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);
        return out;
    }

    private static byte[] gcmDecrypt(byte[] blob, SecretKey key) throws GeneralSecurityException {
        if (blob.length <= GCM_NONCE_LEN) {
            throw new GeneralSecurityException("truncated ciphertext");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, Arrays.copyOf(blob, GCM_NONCE_LEN)));
        return cipher.doFinal(blob, GCM_NONCE_LEN, blob.length - GCM_NONCE_LEN);
    }

    private static SecretKey deriveKek(String password, byte[] salt, int iterations) throws GeneralSecurityException {
        // ponytail: PBKDF2 from the stdlib, not Argon2 — no new dependency for this threat model.
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        byte[] kek = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(kek, "AES");
    }
}
