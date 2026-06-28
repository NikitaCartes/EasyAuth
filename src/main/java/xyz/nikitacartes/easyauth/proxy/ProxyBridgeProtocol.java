package xyz.nikitacartes.easyauth.proxy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Wire format and HMAC for the (unofficial) AuthMeReloaded proxy-bridge protocol on channel
 * {@code authme:main}. This is a faithful, dependency-free re-implementation of the format used by
 * AuthMe's {@code BungeeReceiver}/{@code BungeeSender}/{@code VelocityProxyBridge}, so an EasyAuth
 * backend can talk to an AuthMe Velocity plugin without any change to AuthMe.
 *
 * <p>Pure Java (only {@code java.*} + {@code javax.crypto}); no Minecraft references, so it is unit
 * testable on its own — see {@code ProxyBridgeProtocolTest}.
 *
 * <p>Messages are Java {@link DataOutputStream}/{@link DataInputStream} streams (modified UTF-8 via
 * {@code writeUTF}, big-endian {@code writeLong}) — byte-compatible with Guava's
 * {@code ByteArrayDataOutput} that AuthMe uses.
 */
public final class ProxyBridgeProtocol {

    public static final String CHANNEL_NAMESPACE = "authme";
    public static final String CHANNEL_PATH = "main";
    /** Channel identifier as it appears in packet ids / the allowed-custom-packets whitelist. */
    public static final String CHANNEL = CHANNEL_NAMESPACE + ":" + CHANNEL_PATH;

    /** Replay window: a {@code perform.login} is rejected if its timestamp is older/newer than this. */
    public static final long MAX_AGE_MILLIS = 30_000L;

    private static final String HMAC_ALGO = "HmacSHA256";

    // Message type ids (mirror fr.xephi.authme.service.bungeecord.MessageType).
    public static final String TYPE_PROXY_STARTED = "proxy.started";
    public static final String TYPE_PERFORM_LOGIN = "perform.login";
    public static final String TYPE_PERFORM_LOGIN_ACK = "perform.login.ack";
    public static final String TYPE_PREMIUM_SET = "premium.set";
    public static final String TYPE_PREMIUM_UNSET = "premium.unset";
    public static final String TYPE_PREMIUM_PENDING_SET = "premium.pending.set";
    public static final String TYPE_PREMIUM_LIST_CHUNK = "premium.list.chunk";

    private ProxyBridgeProtocol() {
    }

    /** A verified {@code perform.login}: player name plus the proxy-verified premium UUID (or null). */
    public record PerformLogin(String name, UUID verifiedPremiumUuid) {
    }

    /**
     * HMAC-SHA256 over {@code name:timestamp:uuid} (empty string for a null uuid), hex lowercase.
     * Identical to AuthMe's {@code ProxyMessageSecurity.computeHmac}.
     */
    public static String computeHmac(String secret, String name, long timestamp, UUID uuid) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            String payload = name + ":" + timestamp + ":" + (uuid == null ? "" : uuid);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    /** Reads only the message type id, or {@code null} if the payload is malformed/empty. */
    public static String readType(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return in.readUTF();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the argument of a {@code proxy.started} message (the proxy identity, e.g. "velocity"),
     * or {@code null} if this is not a well-formed {@code proxy.started}.
     */
    public static String readProxyStartedArgument(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (!TYPE_PROXY_STARTED.equals(in.readUTF())) {
                return null;
            }
            return in.readUTF();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses and HMAC-verifies a {@code perform.login} message. Mirrors AuthMe's
     * {@code BungeeReceiver.parseAndVerifyPerformLogin} + {@code verifyHmac}, including the
     * old-format (no UUID field) fallback.
     *
     * @return the verified login, or {@code null} if it is malformed, expired, or fails verification
     */
    public static PerformLogin parseAndVerifyPerformLogin(byte[] data, String secret, long now) {
        if (secret == null || secret.isEmpty()) {
            return null;
        }
        String name;
        long timestamp;
        UUID verifiedPremiumUuid;
        String hmac;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (!TYPE_PERFORM_LOGIN.equals(in.readUTF())) {
                return null;
            }
            name = in.readUTF();
            timestamp = in.readLong();
            String uuidOrHmac = in.readUTF();
            UUID parsed = parseUuidSafely(uuidOrHmac);
            if (parsed != null || uuidOrHmac.isEmpty()) {
                // New format: field is the (possibly empty) UUID, HMAC follows.
                verifiedPremiumUuid = parsed;
                hmac = in.readUTF();
            } else {
                // Old format: no UUID field, the value read is the HMAC.
                verifiedPremiumUuid = null;
                hmac = uuidOrHmac;
            }
        } catch (IOException e) {
            return null;
        }

        if (Math.abs(now - timestamp) > MAX_AGE_MILLIS) {
            return null;
        }
        String expected = computeHmac(secret, name, timestamp, verifiedPremiumUuid);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                hmac.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return new PerformLogin(name, verifiedPremiumUuid);
    }

    // --- Outgoing message builders (backend -> proxy) ---

    /** {@code perform.login.ack} so the proxy stops retrying the auto-login. */
    public static byte[] performLoginAck(String name) {
        return twoField(TYPE_PERFORM_LOGIN_ACK, lower(name));
    }

    /** {@code premium.set}: the player is a confirmed premium account. */
    public static byte[] premiumSet(String name) {
        return twoField(TYPE_PREMIUM_SET, lower(name));
    }

    /** {@code premium.unset}: the player is no longer premium. */
    public static byte[] premiumUnset(String name) {
        return twoField(TYPE_PREMIUM_UNSET, lower(name));
    }

    /**
     * {@code premium.pending.set}: force Mojang verification for this player on their next connect
     * but do not auto-login them until the first successful verification confirms it.
     */
    public static byte[] premiumPendingSet(String name) {
        return twoField(TYPE_PREMIUM_PENDING_SET, lower(name));
    }

    /**
     * One chunk of the full premium list. Argument format is {@code seq:last:csv}; the proxy resets
     * its buffer on {@code seq==0} and applies the list on {@code last==true}.
     */
    public static byte[] premiumListChunk(int seq, boolean last, String csv) {
        return twoField(TYPE_PREMIUM_LIST_CHUNK, seq + ":" + (last ? "1" : "0") + ":" + csv);
    }

    private static byte[] twoField(String type, String argument) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(type);
            out.writeUTF(argument);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode " + type, e);
        }
    }

    private static String lower(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    private static UUID parseUuidSafely(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
