package xyz.nikitacartes.easyauth.storage;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.utils.Totp;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class PlayerEntryV1 {

    public static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
            .create();

    public String username;
    public String usernameLowerCase;
    public UUID uuid = null;

    /**
     * Hashed password of player.
     */
    @Expose
    public String password = "";

    /**
     * Last recorded IP of player.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayer) sessions}.
     */
    @Expose
    @SerializedName("last_ip")
    public String lastIp = "";

    /**
     * Stores the last time a player was successfully authenticated (unix ms).
     */
    @Expose
    @SerializedName("last_authenticated_date")
    public ZonedDateTime lastAuthenticatedDate = getUnixZero();

    /**
     * Stores how many times the player has tried to log in.
     * Cleared on every successful login and every time the player is kicked for too many incorrect logins.
     */
    @Expose
    @SerializedName("login_tries")
    public long loginTries = 0;

    /**
     * Stores the last time a player was kicked for too many logins (unix ms).
     */
    @Expose
    @SerializedName("last_kicked_date")
    public ZonedDateTime lastKickedDate = getUnixZero();

    /**
     * Does the player have an online account?
     */
    @Expose
    @SerializedName("online_account")
    public OnlineAccount onlineAccount = OnlineAccount.UNKNOWN;

    /**
     * Registration date of the player.
     */
    @Expose
    @SerializedName("registration_date")
    public ZonedDateTime registrationDate = getUnixZero();

    /**
     * Stores version of the player data.
     */
    @Expose
    @SerializedName("data_version")
    public int dataVersion = 1;

    /**
     * Forced UUID for the player.
     * When set, this UUID will be used instead of the default offline/online UUID.
     * Useful for preserving player data when switching between online/offline modes.
     */
    @Expose
    @SerializedName("forced_uuid")
    public String forcedUuid = null;

    /**
     * Player-chosen session length in seconds (auto-login window).
     * 0 = follow the server default ({@code config.sessionTimeout}).
     * -1 = never keep a session (always require a fresh login).
     * Any positive value is clamped to the server default, so a player can only
     * shorten their session, never extend it beyond the admin's policy.
     */
    @Expose
    @SerializedName("session_timeout")
    public long sessionTimeout = 0;

    /**
     * Whether the login Dialog window is shown to this player on join.
     * When false, the player gets the chat prompt instead. Boxed so a missing
     * value in old data deserializes to {@code true}.
     */
    @Expose
    @SerializedName("show_login_dialog")
    public boolean showLoginDialog = true;

    /**
     * Base32-encoded TOTP secret, or {@code null} if 2FA is not set up.
     * Set when the player starts enrollment; 2FA only enforced once {@link #otpEnabled} is true.
     */
    @Expose
    @SerializedName("otp_secret")
    public String otpSecret = null;

    /**
     * Whether two-factor authentication is active for this player (confirmed enrollment).
     */
    @Expose
    @SerializedName("otp_enabled")
    public boolean otpEnabled = false;

    public PlayerEntryV1(String username, String usernameLowerCase, String uuid, String json) {
        PlayerEntryV1 entry = gson.fromJson(json, PlayerEntryV1.class);
        ZonedDateTime startOfTime = getUnixZero();

        this.username = username;
        this.usernameLowerCase = usernameLowerCase;
        this.uuid = uuid == null ? null : UUID.fromString(uuid);

        this.password = entry.password == null ? "" : entry.password;
        this.lastIp = entry.lastIp == null ? "" : entry.lastIp;
        this.loginTries = entry.loginTries;
        this.onlineAccount = entry.onlineAccount == null ? OnlineAccount.UNKNOWN : entry.onlineAccount;
        this.lastAuthenticatedDate = entry.lastAuthenticatedDate == null ? startOfTime : entry.lastAuthenticatedDate;
        this.lastKickedDate = entry.lastKickedDate == null ? startOfTime : entry.lastKickedDate;
        this.registrationDate = entry.registrationDate == null ? startOfTime : entry.registrationDate;
        this.dataVersion = entry.dataVersion;
        this.forcedUuid = entry.forcedUuid;
        this.sessionTimeout = entry.sessionTimeout;
        this.showLoginDialog = entry.showLoginDialog;
        this.otpSecret = entry.otpSecret;
        this.otpEnabled = entry.otpEnabled;
    }

    /** True if two-factor authentication is active and a code must be supplied at login. */
    public boolean hasOtp() {
        return otpEnabled && otpSecret != null;
    }

    /** Verifies a TOTP code against this player's secret (±1 time step for clock drift). */
    public boolean verifyOtp(String code) {
        return hasOtp() && Totp.verify(otpSecret, code, 1);
    }

    public PlayerEntryV1(String username) {
        this.username = username;
        this.usernameLowerCase = username.toLowerCase(Locale.ENGLISH);
    }

    public PlayerEntryV1(String username, UUID uuid) {
        this(username);
        this.uuid = uuid;
    }

    /**
     * Copies this entry's account and auth state onto {@code target}, leaving identity fields
     * (username, usernameLowerCase, uuid, forcedUuid) untouched. Used by {@code /auth migrate}.
     * <p>
     * When adding a new persisted account field above, copy it here too so migration keeps it.
     */
    public void copyAccountDataTo(PlayerEntryV1 target) {
        target.password = this.password;
        target.lastIp = this.lastIp;
        target.lastAuthenticatedDate = this.lastAuthenticatedDate;
        target.loginTries = this.loginTries;
        target.lastKickedDate = this.lastKickedDate;
        target.onlineAccount = this.onlineAccount;
        target.registrationDate = this.registrationDate;
        target.dataVersion = this.dataVersion;
        target.sessionTimeout = this.sessionTimeout;
        target.showLoginDialog = this.showLoginDialog;
        target.otpSecret = this.otpSecret;
        target.otpEnabled = this.otpEnabled;
    }

    public String toJson() {
        return gson.toJson(this);
    }

    /*
     * Update entry in database.
     */
    public void update() {
        THREADPOOL.execute(() -> DB.updateUserData(this));
    }

    public enum OnlineAccount {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

        @Override
        public JsonElement serialize(ZonedDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws JsonParseException {
            return ZonedDateTime.parse(json.getAsString(), formatter);
        }
    }
}


