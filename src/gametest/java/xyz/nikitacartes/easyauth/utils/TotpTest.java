package xyz.nikitacartes.easyauth.utils;

//? if >= 1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
*///?}
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.charset.StandardCharsets;

/**
 * RFC 6238 Appendix B test vectors for the SHA1 mode (the only mode authenticator
 * apps use by default, and the only one {@link Totp} implements).
 *
 * <p>The RFC table lists 8-digit codes; {@link Totp} emits 6 digits, so we compare
 * against the last 6 digits of each published value. The SHA256/SHA512 rows use
 * different (longer) seeds and do not apply to a SHA1 implementation.
 */
//? if >= 1.21.5 {
public class TotpTest {
@GameTest
//?} else {
/*public class TotpTest implements FabricGameTest {
@GameTest(template = EMPTY_STRUCTURE)
*///?}
    public void rfc6238Vectors(GameTestHelper context) {
        // RFC 6238 shared secret: ASCII "12345678901234567890".
        byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

        // time (sec) -> last 6 digits of the published SHA1 TOTP. Counter = floor(time / 30).
        check(key, 59L, "287082");          // 94287082
        check(key, 1111111109L, "081804");  // 07081804
        check(key, 1111111111L, "050471");  // 14050471
        check(key, 1234567890L, "005924");  // 89005924
        check(key, 2000000000L, "279037");  // 69279037
        check(key, 20000000000L, "353130"); // 65353130 (verifies 64-bit counter handling)

        // Round-trip: a freshly generated secret verifies its own current code and rejects a wrong one.
        String secret = Totp.generateSecret();
        long counter = System.currentTimeMillis() / 1000L / 30L;
        String current = Totp.generate(Totp.base32Decode(secret), counter);
        if (!Totp.verify(secret, current, 1)) {
            throw new AssertionError("verify() rejected a valid current code");
        }
        if (Totp.verify(secret, "000000".equals(current) ? "000001" : "000000", 0)) {
            throw new AssertionError("verify() accepted a wrong code");
        }

        context.succeed();
    }

    private static void check(byte[] key, long timeSeconds, String expected) {
        String got = Totp.generate(key, timeSeconds / 30L);
        if (!got.equals(expected)) {
            throw new AssertionError("RFC 6238 vector failed at t=" + timeSeconds + ": expected " + expected + " but got " + got);
        }
    }
}
