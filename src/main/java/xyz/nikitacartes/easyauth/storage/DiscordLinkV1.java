package xyz.nikitacartes.easyauth.storage;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

/**
 * Class for storing Discord link data
 */
public class DiscordLinkV1 {

    public static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
            .create();

    /**
     * Adapter for serializing and deserializing ZonedDateTime
     */
    private static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return ZonedDateTime.parse(json.getAsString(), formatter);
            } catch (Exception e) {
                LogDebug("Error parsing ZonedDateTime: " + e.getMessage());
                return getUnixZero();
            }
        }
    }

    /**
     * Minecraft username (case-sensitive).
     */
    @Expose
    @SerializedName("username")
    public String username;

    /**
     * Verification code for linking with Discord.
     */
    @Expose
    @SerializedName("code")
    public String code = "";

    /**
     * When the verification code expires.
     */
    @Expose
    @SerializedName("code_expires_at")
    public ZonedDateTime codeExpiresAt = getUnixZero();

    /**
     * Discord user ID.
     */
    @Expose
    @SerializedName("discord_id")
    public Long discordId = null;

    /**
     * When the account was linked with Discord.
     */
    @Expose
    @SerializedName("linked_at")
    public ZonedDateTime linkedAt = null;

    /**
     * Number of link attempts today.
     */
    @Expose
    @SerializedName("link_attempts_today")
    public int linkAttemptsToday = 0;

    /**
     * When the link attempts count will reset.
     */
    @Expose
    @SerializedName("attempts_reset_at")
    public ZonedDateTime attemptsResetAt = getUnixZero();

    public DiscordLinkV1(String username) {
        this.username = username;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Creates a new DiscordLinkV1 from JSON
     */
    public static DiscordLinkV1 fromJson(String username, String json) {
        if (json == null || json.isEmpty()) {
            return new DiscordLinkV1(username);
        }

        try {
            DiscordLinkV1 link = gson.fromJson(json, DiscordLinkV1.class);
            link.username = username; // Ensure username is set correctly
            return link;
        } catch (JsonSyntaxException e) {
            LogDebug("Error parsing DiscordLinkV1 JSON: " + e.getMessage());
            return new DiscordLinkV1(username);
        }
    }

    /**
     * Check if the link exists and is valid
     */
    public boolean isLinked() {
        return discordId != null && linkedAt != null;
    }

    /**
     * Check if the verification code is expired
     */
    public boolean isCodeExpired() {
        return code == null || code.isEmpty() || codeExpiresAt.isBefore(ZonedDateTime.now());
    }
} 