package xyz.nikitacartes.easyauth.protocol;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Wire format of the EasyAuth Client companion protocol, compiled into BOTH the server mod and
 * the client mod (this directory is an extra source dir in every build script — the
 * dependency-free-protocol pattern of {@code ProxyBridgeProtocol}). Field order lives only here,
 * so the two sides cannot drift apart.
 *
 * <p>Keep this file free of stonecutter comments and loader/version-specific classes: it is
 * compiled as-is for every target. {@code FriendlyByteBuf} and the methods used here
 * (readUtf/writeUtf, readVarInt/writeVarInt, readBoolean/writeBoolean, readByteArray/writeByteArray)
 * are stable across 1.19.4..26.x under Mojang mappings.
 */
public final class ClientModProtocol {

    public static final int PROTOCOL_VERSION = 2;

    public static final String HELLO_CHANNEL = "easyauth:hello";
    /** Kept in sync by reference with the pre-auth whitelist in AuthEventHandler.isAllowedPacket. */
    public static final String AUTH_CHANNEL = "easyauth:auth";
    public static final String RESULT_CHANNEL = "easyauth:result";

    // C2S easyauth:auth modes.
    public static final int MODE_PASSWORD = 0;
    public static final int MODE_TOKEN = 1;
    public static final int MODE_PASSKEY = 2;
    public static final int MODE_REGISTER_PASSKEY = 3;

    // S2C easyauth:result codes. Rejections tell the client to fall back to the next
    // credential (passkey -> token -> password); password failures keep using chat messages.
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_TOKEN_REJECTED = 1;
    public static final int RESULT_PASSKEY_REJECTED = 2;

    /** Signed alongside the passkey challenge for domain separation (no cross-protocol signature reuse). */
    public static final byte[] PASSKEY_DOMAIN = "easyauth-passkey-v1".getBytes(StandardCharsets.US_ASCII);

    private ClientModProtocol() {
    }

    /**
     * S2C capability + auth-state announce, sent right after join.
     * Challenge is empty unless a passkey login is possible.
     */
    public record Hello(int protocolVersion, boolean canAutoLogin, boolean canAutoRegister,
                        boolean registered, boolean authenticated,
                        boolean canSessionToken, boolean canPasskey, boolean hasPasskey,
                        byte[] challenge) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(protocolVersion);
            buf.writeBoolean(canAutoLogin);
            buf.writeBoolean(canAutoRegister);
            buf.writeBoolean(registered);
            buf.writeBoolean(authenticated);
            buf.writeBoolean(canSessionToken);
            buf.writeBoolean(canPasskey);
            buf.writeBoolean(hasPasskey);
            buf.writeByteArray(challenge);
        }

        public static Hello read(FriendlyByteBuf buf) {
            return new Hello(buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readByteArray());
        }
    }

    /**
     * C2S credentials; the mode selects which fields matter (the rest stay empty):
     * PASSWORD -> password/otp, TOKEN -> token, PASSKEY -> publicKey/signature,
     * REGISTER_PASSKEY (authenticated players only) -> publicKey.
     */
    public record Auth(int mode, String password, String otp, String token,
                       byte[] publicKey, byte[] signature) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(mode);
            buf.writeUtf(password);
            buf.writeUtf(otp);
            buf.writeUtf(token);
            buf.writeByteArray(publicKey);
            buf.writeByteArray(signature);
        }

        public static Auth read(FriendlyByteBuf buf) {
            return new Auth(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readByteArray(), buf.readByteArray());
        }
    }

    /** S2C auth outcome; sessionToken is non-empty only on SUCCESS with tokens enabled (issue/rotation). */
    public record Result(int code, String sessionToken) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(code);
            buf.writeUtf(sessionToken);
        }

        public static Result read(FriendlyByteBuf buf) {
            return new Result(buf.readVarInt(), buf.readUtf());
        }
    }
}
