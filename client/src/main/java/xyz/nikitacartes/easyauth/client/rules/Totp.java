package xyz.nikitacartes.easyauth.client.rules;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generator half of the server's {@code xyz.nikitacartes.easyauth.utils.Totp}
 * (RFC 6238: HmacSHA1, 6 digits, 30s step), copied because the client branch has no
 * shared module yet — extract one when the handshake protocol (plan phase 5) needs it.
 */
final class Totp {

    private static final int DIGITS = 6;
    private static final int PERIOD = 30;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Totp() {
    }

    /** The current 6-digit code, or null when the secret is missing/invalid. */
    static String currentCode(String base32Secret) {
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
        return String.format("%0" + DIGITS + "d", otp);
    }

    static byte[] base32Decode(String s) {
        String clean = s.trim().replace(" ", "").replace("=", "").toUpperCase();
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
}
