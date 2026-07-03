package xyz.nikitacartes.easyauth.proxy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Self-check for the security/parsing path of {@link ProxyBridgeProtocol}. No test framework: run
 * {@code main} directly (the class is pure Java). It independently re-encodes a {@code perform.login}
 * exactly the way AuthMe's Velocity plugin does, then asserts the protocol accepts/rejects correctly.
 *
 * <p>Lives in the Fabric gametest source set so it compiles against {@link ProxyBridgeProtocol}
 * but is not run by any Gradle task (no {@code @GameTest}, not a JUnit test). Run standalone, e.g.
 * {@code javac -d out ProxyBridgeProtocol.java ProxyBridgeProtocolTest.java && java -ea -cp out
 * xyz.nikitacartes.easyauth.proxy.ProxyBridgeProtocolTest}.
 */
public final class ProxyBridgeProtocolTest {

    public static void main(String[] args) {
        // Run with assertions on even if -ea was forgotten.
        boolean ok = true;
        ok &= run("premium perform.login round-trips and verifies", ProxyBridgeProtocolTest::testPremiumRoundTrip);
        ok &= run("offline (no-uuid, old format) perform.login verifies", ProxyBridgeProtocolTest::testOldFormatRoundTrip);
        ok &= run("tampered HMAC is rejected", ProxyBridgeProtocolTest::testTamperedHmac);
        ok &= run("expired timestamp is rejected", ProxyBridgeProtocolTest::testExpired);
        ok &= run("empty secret is rejected", ProxyBridgeProtocolTest::testEmptySecret);
        ok &= run("wrong secret is rejected", ProxyBridgeProtocolTest::testWrongSecret);
        ok &= run("truncated payload is rejected", ProxyBridgeProtocolTest::testTruncated);
        ok &= run("HMAC matches AuthMe's algorithm exactly", ProxyBridgeProtocolTest::testHmacCrossCheck);
        ok &= run("outgoing builders frame as type+arg", ProxyBridgeProtocolTest::testOutgoingFraming);
        if (!ok) {
            System.exit(1);
        }
        System.out.println("ALL PASSED");
    }

    private static final String SECRET = "super-secret-shared-key";

    private static void testPremiumRoundTrip() {
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, uuid);
        ProxyBridgeProtocol.PerformLogin pl = ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, SECRET, ts);
        check(pl != null, "should verify");
        check("player1".equals(pl.name()), "name");
        check(uuid.equals(pl.verifiedPremiumUuid()), "uuid");
        check(ProxyBridgeProtocol.TYPE_PERFORM_LOGIN.equals(ProxyBridgeProtocol.readType(msg)), "readType");
    }

    private static void testOldFormatRoundTrip() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLoginOldFormat(SECRET, "player2", ts);
        ProxyBridgeProtocol.PerformLogin pl = ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, SECRET, ts);
        check(pl != null, "should verify");
        check("player2".equals(pl.name()), "name");
        check(pl.verifiedPremiumUuid() == null, "uuid null");
    }

    private static void testTamperedHmac() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, null);
        // Flip the last byte (inside the HMAC field).
        msg[msg.length - 1] ^= 0x01;
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, SECRET, ts) == null, "tampered rejected");
    }

    private static void testExpired() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, null);
        long later = ts + ProxyBridgeProtocol.MAX_AGE_MILLIS + 1000;
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, SECRET, later) == null, "expired rejected");
    }

    private static void testEmptySecret() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, null);
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, "", ts) == null, "empty secret rejected");
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, null, ts) == null, "null secret rejected");
    }

    private static void testWrongSecret() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, null);
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(msg, "other-secret", ts) == null, "wrong secret rejected");
    }

    private static void testTruncated() {
        long ts = System.currentTimeMillis();
        byte[] msg = encodePerformLogin(SECRET, "player1", ts, null);
        byte[] cut = new byte[5];
        System.arraycopy(msg, 0, cut, 0, 5);
        check(ProxyBridgeProtocol.parseAndVerifyPerformLogin(cut, SECRET, ts) == null, "truncated rejected");
        check(ProxyBridgeProtocol.readType(cut) == null || ProxyBridgeProtocol.readType(cut) != null, "readType no throw");
    }

    private static void testHmacCrossCheck() {
        // Reference value computed the AuthMe way (ProxyMessageSecurity.computeHmac).
        long ts = 1700000000000L;
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String reference = authMeHmac(SECRET, "alice", ts, uuid);
        String ours = ProxyBridgeProtocol.computeHmac(SECRET, "alice", ts, uuid);
        check(reference.equals(ours), "hmac equals AuthMe reference");
        check(ours.equals(ours.toLowerCase(java.util.Locale.ROOT)), "hmac is lowercase hex");
    }

    private static void testOutgoingFraming() {
        check(ProxyBridgeProtocol.TYPE_PREMIUM_SET.equals(
                ProxyBridgeProtocol.readType(ProxyBridgeProtocol.premiumSet("Bob"))), "premium.set type");
        check(ProxyBridgeProtocol.TYPE_PREMIUM_PENDING_SET.equals(
                ProxyBridgeProtocol.readType(ProxyBridgeProtocol.premiumPendingSet("Bob"))), "pending type");
        check(ProxyBridgeProtocol.TYPE_PREMIUM_LIST_CHUNK.equals(
                ProxyBridgeProtocol.readType(ProxyBridgeProtocol.premiumListChunk(0, true, "a,b,c"))), "chunk type");
        check(ProxyBridgeProtocol.TYPE_PERFORM_LOGIN_ACK.equals(
                ProxyBridgeProtocol.readType(ProxyBridgeProtocol.performLoginAck("Bob"))), "ack type");
    }

    // --- AuthMe-equivalent encoders, written independently of ProxyBridgeProtocol ---

    private static byte[] encodePerformLogin(String secret, String name, long ts, UUID uuid) {
        String hmac = authMeHmac(secret, name, ts, uuid);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("perform.login");
            out.writeUTF(name);
            out.writeLong(ts);
            out.writeUTF(uuid == null ? "" : uuid.toString());
            out.writeUTF(hmac);
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] encodePerformLoginOldFormat(String secret, String name, long ts) {
        String hmac = authMeHmac(secret, name, ts, null);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("perform.login");
            out.writeUTF(name);
            out.writeLong(ts);
            out.writeUTF(hmac); // no uuid field; hmac sits where the uuid would be
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String authMeHmac(String secret, String name, long ts, UUID uuid) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = name + ":" + ts + ":" + (uuid == null ? "" : uuid);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(boolean cond, String what) {
        if (!cond) {
            throw new AssertionError(what);
        }
    }

    private interface Case {
        void run() throws Exception;
    }

    private static boolean run(String name, Case c) {
        try {
            c.run();
            System.out.println("  ok: " + name);
            return true;
        } catch (Throwable t) {
            System.out.println("FAIL: " + name + " -> " + t);
            return false;
        }
    }
}
