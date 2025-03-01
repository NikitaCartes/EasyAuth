package xyz.nikitacartes.easyauth.storage;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTPGenerator;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

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
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
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
     * OTP secret key for 2FA.
     */
    @Expose
    @SerializedName("otp_secret")
    public String otpSecret = null;

    /**
     * OTP enabled for the player.
     */
    @Expose
    @SerializedName("otp_enabled")
    public boolean otpEnabled = false;

    /**
     * Does player need both password and OTP to login.
     */
    @Expose
    @SerializedName("2fa_required")
    public boolean twoFactorAuthRequired = false;

    /**
     * Stores version of the player data.
     */
    @Expose
    @SerializedName("data_version")
    public int dataVersion = 1;

    private TOTPGenerator totpGenerator = null;

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

        if (entry.otpSecret != null && config.otpEnabled) {
            this.totpGenerator = new TOTPGenerator.Builder(entry.otpSecret)
                    .withHOTPGenerator(builder -> {
                        builder.withPasswordLength(6);
                        builder.withAlgorithm(HMACAlgorithm.SHA1);
                    })
                    .withPeriod(Duration.ofSeconds(30))
                    .build();
            this.otpSecret = entry.otpSecret;
        } else {
            this.otpSecret = null;
        }
    }

    public PlayerEntryV1(String username) {
        this.username = username;
        this.usernameLowerCase = username.toLowerCase(Locale.ENGLISH);
    }

    public String toJson() {
        return gson.toJson(this);
    }

    /*
     * Update entry in database.
     */
    public void update() {
        LogDebug("Updating player data for " + username + " in database: " + toJson());
        THREADPOOL.execute(() -> DB.updateUserData(this));
    }

    public enum OnlineAccount {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ZonedDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    public boolean verifyOtp(String otp) {
        if (otpSecret == null) {
            return false;
        }
        if (totpGenerator == null) {
            this.totpGenerator = new TOTPGenerator.Builder(otpSecret)
                    .withHOTPGenerator(builder -> {
                        builder.withPasswordLength(6);
                        builder.withAlgorithm(HMACAlgorithm.SHA1);
                    })
                    .withPeriod(Duration.ofSeconds(30))
                    .build();
        }
        return totpGenerator.verify(otp, 1);
    }
}


