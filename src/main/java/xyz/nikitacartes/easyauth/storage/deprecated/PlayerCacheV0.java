package xyz.nikitacartes.easyauth.storage.deprecated;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCacheV0 {

    public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    // public final LastLocation lastLocation = new LastLocation();
    /**
     * Whether player is authenticated.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayer) session validation}.
     */
    // @Expose
    // @SerializedName("is_authenticated")
    // public boolean isAuthenticated = false;
    /**
     * Hashed password of player.
     */
    @Expose
    public String password = "";
    /**
     * Stores how many times the player has tried to log in.
     * Cleared on every successful login and every time the player is kicked for too many incorrect logins.
     */
    @Expose
    @SerializedName("login_tries")
    public AtomicInteger loginTries = new AtomicInteger();
    /**
     * Stores the last time a player was kicked for too many logins (unix ms).
     */
    @Expose
    @SerializedName("last_kicked")
    public long lastKicked = 0;
    /**
     * Last recorded IP of player.
     * Used for {@link AuthEventHandler#onPlayerJoin(ServerPlayer) sessions}.
     */
    @Expose
    @SerializedName("last_ip")
    public String lastIp;
    /**
     * Time until session is valid.
     */
    @Expose
    @SerializedName("valid_until")
    public long validUntil;
    /**
     * Contains the UUID of the entity that the player was riding before leaving the server.
     */
    // @Expose
    // @SerializedName("riding_entity_uuid")
    // public UUID ridingEntityUUID = null;
    /**
     * Whether player was dead
     */
    // @Expose
    // @SerializedName("was_dead")
    // public boolean wasDead = false;

    public static PlayerCacheV0 fromJson(String json) {
        return gson.fromJson(json, PlayerCacheV0.class);
    }
}
