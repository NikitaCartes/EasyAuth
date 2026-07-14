package xyz.nikitacartes.easyauth.client.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.protocol.ClientModProtocol;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Client half of the passkey login: an Ed25519 keypair per server. The public key
 * (X.509/SPKI) is registered on the server after a successful login; later logins sign the
 * server's one-time challenge, so the secret never leaves this machine. Keys are stored base64
 * in credentials.json (the private key encrypted at rest via {@link Vault}, like the password).
 */
public final class Passkey {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    private Passkey() {
    }

    /** [0] = base64 X.509/SPKI public key, [1] = base64 PKCS#8 private key; null if Ed25519 is unavailable. */
    public static String[] generate() {
        try {
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            return new String[]{
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())};
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Ed25519 is not available in this JRE; passkeys disabled: {}", e.toString());
            return null;
        }
    }

    /** Signs the server challenge (domain-separated); null on any failure (corrupt key, no Ed25519). */
    public static byte[] sign(String privateKeyBase64, byte[] challenge) {
        try {
            PrivateKey key = KeyFactory.getInstance("Ed25519")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(key);
            signer.update(ClientModProtocol.PASSKEY_DOMAIN);
            signer.update(challenge);
            return signer.sign();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            LOGGER.warn("Could not sign passkey challenge: {}", e.toString());
            return null;
        }
    }

    public static byte[] decodePublic(String publicKeyBase64) {
        try {
            return Base64.getDecoder().decode(publicKeyBase64);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
