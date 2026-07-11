package xyz.nikitacartes.easyauth.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Self-contained TOTP (RFC 6238) implementation: HMAC-SHA1, 6 digits, 30s step.
 * No external dependency — authenticator apps (Google Authenticator, Aegis, etc.)
 * use exactly these defaults, so they interoperate without configuration.
 *
 * <p>Shared source: compiled into both the server mod (enrollment + verify) and the client
 * companion (code generation for auto-login), so the two sides cannot drift. Locale.ROOT
 * everywhere — the client runs on arbitrary player machines (Turkish dotless-i, localized digits).
 * Keep this file free of Minecraft classes.
 */
public final class Totp {

    private static final int DIGITS = 6;
    private static final int PERIOD = 30;
    private static final int SECRET_BYTES = 20; // 160-bit, the RFC-recommended SHA1 key size
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Totp() {}

    /** A fresh random secret, Base32-encoded (no padding) for use in an otpauth URI. */
    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Verifies {@code code} against {@code base32Secret}, accepting codes within
     * {@code window} steps of the current time (±window) to tolerate clock drift.
     */
    public static boolean verify(String base32Secret, String code, int window) {
        if (base32Secret == null || code == null) {
            return false;
        }
        String trimmed = code.trim();
        if (trimmed.length() != DIGITS) {
            return false;
        }
        byte[] key;
        try {
            key = base32Decode(base32Secret);
        } catch (IllegalArgumentException e) {
            return false;
        }
        long counter = System.currentTimeMillis() / 1000L / PERIOD;
        for (long i = -window; i <= window; i++) {
            if (constantTimeEquals(generate(key, counter + i), trimmed)) {
                return true;
            }
        }
        return false;
    }

    /** The current 6-digit code, or null when the secret is missing/invalid (client auto-login). */
    public static String currentCode(String base32Secret) {
        if (base32Secret == null || base32Secret.isEmpty()) {
            return null;
        }
        byte[] key;
        try {
            key = base32Decode(base32Secret);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return generate(key, System.currentTimeMillis() / 1000L / PERIOD);
    }

    /** The {@code otpauth://} URI an authenticator app scans to provision the account. */
    public static String uri(String issuer, String account, String base32Secret) {
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD;
    }

    /** The 6-digit code for a given key and time-step counter, zero-padded. */
    static String generate(byte[] key, long counter) {
        byte[] msg = new byte[8];
        for (int i = 7; i >= 0; i--) {
            msg[i] = (byte) (counter & 0xff);
            counter >>= 8;
        }
        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            hash = mac.doFinal(msg);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(BASE32.charAt((buffer >> bits) & 0x1f));
            }
        }
        if (bits > 0) {
            sb.append(BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String s) {
        String clean = s.trim().replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0, bits = 0, index = 0;
        byte[] out = new byte[clean.length() * 5 / 8];
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32.indexOf(clean.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out[index++] = (byte) ((buffer >> bits) & 0xff);
            }
        }
        return out;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
