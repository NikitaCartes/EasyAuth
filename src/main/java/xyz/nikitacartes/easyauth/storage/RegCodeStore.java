package xyz.nikitacartes.easyauth.storage;

import com.google.gson.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static xyz.nikitacartes.easyauth.EasyAuth.gameDirectory;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

/**
 * In-memory, file-backed store of {@link RegCode}s (config/EasyAuth/regcodes.json).
 * The set is small (admin-managed) so the whole file is rewritten on each change and lookups
 * are plain map/scan operations. All mutating methods are synchronized; the server is single-node
 * so an in-memory lock is the whole story. ponytail: global lock, fine until codes are redeemed
 * thousands of times per second (they never will be).
 */
public class RegCodeStore {
    public enum Status { OK, NOT_FOUND, EXPIRED, EXHAUSTED }

    private static final SecureRandom RANDOM = new SecureRandom();
    // No ambiguous chars (0/O, 1/I/L) so codes can be read off chat and retyped.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    private static final Type LIST_TYPE = new com.google.gson.reflect.TypeToken<List<RegCode>>() {}.getType();

    private final Path file;
    private final Map<String, RegCode> codes = new LinkedHashMap<>();

    private RegCodeStore(Path file) {
        this.file = file;
    }

    public static RegCodeStore load() {
        RegCodeStore store = new RegCodeStore(gameDirectory.resolve("config/EasyAuth/regcodes.json"));
        if (Files.exists(store.file)) {
            try {
                List<RegCode> loaded = GSON.fromJson(Files.readString(store.file), LIST_TYPE);
                if (loaded != null) {
                    for (RegCode code : loaded) {
                        store.codes.put(code.code, code);
                    }
                }
            } catch (IOException | JsonParseException e) {
                LogError("Failed to load regcodes.json; starting with an empty set", e);
            }
        }
        return store;
    }

    private void save() {
        try {
            Files.writeString(file, GSON.toJson(new ArrayList<>(codes.values()), LIST_TYPE));
        } catch (IOException e) {
            LogError("Failed to save regcodes.json", e);
        }
    }

    /** Creates and persists a new code with a freshly generated secret. */
    public synchronized RegCode create(String alias, int maxUses, ZonedDateTime expiresAt, String createdBy) {
        RegCode code = new RegCode();
        code.code = generateCode();
        code.alias = (alias == null || alias.isBlank()) ? null : alias;
        code.maxUses = Math.max(0, maxUses);
        code.uses = 0;
        code.expiresAt = expiresAt;
        code.createdBy = createdBy;
        code.createdAt = ZonedDateTime.now();
        codes.put(code.code, code);
        save();
        return code;
    }

    /** Resolves by exact code (case-insensitive) or alias, returning null if nothing matches. */
    public synchronized RegCode resolve(String codeOrAlias) {
        if (codeOrAlias == null) {
            return null;
        }
        for (RegCode code : codes.values()) {
            if (codeOrAlias.equalsIgnoreCase(code.code) || codeOrAlias.equalsIgnoreCase(code.alias)) {
                return code;
            }
        }
        return null;
    }

    /**
     * Maps a code-or-alias to the raw code string used in {@link PlayerEntryV1#registeredWithCode},
     * so {@code /auth regcode players} keeps working even after the code itself is deleted (the input
     * is returned unchanged when it no longer resolves).
     */
    public synchronized String resolveToCode(String codeOrAlias) {
        RegCode code = resolve(codeOrAlias);
        return code != null ? code.code : codeOrAlias;
    }

    /** Removes a code by code-or-alias. Returns the removed code, or null if none matched. */
    public synchronized RegCode delete(String codeOrAlias) {
        RegCode code = resolve(codeOrAlias);
        if (code != null) {
            codes.remove(code.code);
            save();
        }
        return code;
    }

    public synchronized List<RegCode> list() {
        return new ArrayList<>(codes.values());
    }

    /** Checks redeemability without mutating (used up front so bad codes are rejected before registering). */
    public synchronized Status check(String code) {
        RegCode entry = codes.get(resolveKey(code));
        if (entry == null) {
            return Status.NOT_FOUND;
        }
        if (entry.isExpired()) {
            return Status.EXPIRED;
        }
        if (entry.isExhausted()) {
            return Status.EXHAUSTED;
        }
        return Status.OK;
    }

    /** Atomically re-checks and consumes one use. Call once a registration has actually succeeded. */
    public synchronized Status redeem(String code) {
        Status status = check(code);
        if (status == Status.OK) {
            codes.get(resolveKey(code)).uses++;
            save();
        }
        return status;
    }

    // Players type the exact code; look it up case-insensitively against the stored keys.
    private String resolveKey(String code) {
        if (code == null) {
            return null;
        }
        for (String key : codes.keySet()) {
            if (key.equalsIgnoreCase(code)) {
                return key;
            }
        }
        return code;
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(9);
            for (int i = 0; i < 8; i++) {
                if (i == 4) {
                    sb.append('-');
                }
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (codes.containsKey(code));
        return code;
    }

    private static final class ZonedDateTimeAdapter
            implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        @Override
        public JsonElement serialize(ZonedDateTime src, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return ZonedDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }
}
