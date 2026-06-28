package xyz.nikitacartes.easyauth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

/**
 * Proxy integration settings (currently the unofficial AuthMeReloaded proxy-bridge on channel
 * {@code authme:main}). Kept in its own {@code proxy.conf} so proxy setup lives in one place.
 *
 * <p>Note: the {@code skip-all-auth-checks*} options remain in {@code extended.conf} for backward
 * compatibility.
 */
@ConfigSerializable
public class ProxyConfigV1 extends ConfigTemplate {

    @Comment("""
            Enable the AuthMe proxy-bridge integration on channel authme:main.
            Lets premium players auto-login without a password once an AuthMeReloaded Velocity plugin
            has cryptographically verified them on the proxy.
            Requires: backend online-mode=false behind a firewalled port, a modern-forwarding mod
            (e.g. FabricProxy-Lite), and Minecraft >= 1.20.5. See the Proxies wiki page.""")
    public boolean enabled = false;

    @Comment("""

            Shared secret, identical to Hooks.proxySharedSecret in the AuthMe Velocity plugin.
            Used to sign/verify perform.login messages (HMAC-SHA256).
            This is NOT the same as Velocity's forwarding.secret (that one goes in the forwarding mod).""")
    public String proxySharedSecret = "";

    public ProxyConfigV1() {
        super("proxy.conf", """
                ##                       ##
                ##        EasyAuth        ##
                ##   Proxy Configuration  ##
                ##                       ##""");
    }

    public static ProxyConfigV1 create() {
        ProxyConfigV1 config = loadConfig(ProxyConfigV1.class, "proxy.conf");
        if (config == null) {
            config = new ProxyConfigV1();
            config.save();
        }
        return config;
    }

    public static ProxyConfigV1 load() {
        ProxyConfigV1 config = loadConfig(ProxyConfigV1.class, "proxy.conf");
        if (config == null) {
            LogError("proxy.conf was not found, creating new one with default values");
            config = new ProxyConfigV1();
            config.save();
        }
        return config;
    }

    @Override
    public void save() {
        save(ProxyConfigV1.class, this);
    }
}
