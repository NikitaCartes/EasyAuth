package xyz.nikitacartes.easyauth.client.rules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.EasyAuthPackets;
import xyz.nikitacartes.easyauth.protocol.ClientModProtocol;
import xyz.nikitacartes.easyauth.utils.Totp;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat/command automation: runs rules from {@code config/easyauth-client/rules.json},
 * keyed by normalized server address. All entry points are called on the client main
 * thread (join/disconnect/chat/tick events), so no synchronization is needed.
 */
public final class RuleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("EasyAuthClient");

    // Session state; empty when not connected or no rules for the current server.
    private static List<ActiveRule> active = List.of();
    private static final List<Pending> queue = new ArrayList<>();
    // Built-in auto-auth; fires once /login or /register appears in the server's command tree
    // (see onTick), but only until the deadline — a server with neither an auth mod nor the
    // packet bridge should not be scanned forever, 20 times a second.
    private static boolean authPending;
    private static long authPendingUntil;
    private static final long AUTH_PENDING_WINDOW_MS = 60_000;
    // Gap between the login command and a separate 2FA command (see sendAuthCommands).
    private static final long TOTP_COMMAND_DELAY_MS = 1_000;
    // Detection literals precomputed from the command templates at join (hot path: onTick).
    private static String loginLiteral = "";
    private static String registerLiteral = "";
    // Whether the current rule set contains chat/leave triggers; lets the chat callbacks skip
    // flattening every message and the pause-screen hook skip touching vanilla buttons.
    private static boolean hasChatRules;
    private static boolean hasLeaveRules;
    private static String serverAddress = "";
    private static Credentials.Store store;
    private static Credentials credentials;
    // Snapshot of the server hello (packet path) for fallback decisions after a token/passkey rejection.
    private static boolean helloCanAutoLogin;
    private static boolean helloCanSessionToken;
    private static boolean helloCanPasskey;
    private static boolean helloHasPasskey;

    private RuleEngine() {
    }

    public static void init(Path configDir) {
        Vault.init(configDir.resolve("easyauth-client"));
    }

    public static Path getCredentialsFile() {
        return Vault.credentialsFile();
    }

    public static void onJoin() {
        queue.clear();
        active = List.of();
        authPending = false;
        hasChatRules = hasLeaveRules = false;
        store = null;
        credentials = null;
        helloCanAutoLogin = helloCanSessionToken = helloCanPasskey = helloHasPasskey = false;
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null) {
            return; // singleplayer/realms
        }
        serverAddress = normalizeAddress(server.ip);
        store = Credentials.load(Vault.credentialsFile());
        credentials = store.servers.get(serverAddress);
        if (credentials != null && Vault.locked()) {
            LOGGER.warn("Credential storage is locked; auto-login on {} stays off until it is unlocked in the config screen", serverAddress);
        }
        authPending = store.autoLogin || store.autoRegister;
        authPendingUntil = System.currentTimeMillis() + AUTH_PENDING_WINDOW_MS;
        loginLiteral = commandLiteral(store.loginCommand);
        registerLiteral = commandLiteral(store.registerCommand);
        active = loadRules(serverAddress);
        hasChatRules = active.stream().anyMatch(r -> r.rule.trigger.equals("chat"));
        hasLeaveRules = active.stream().anyMatch(r -> r.rule.trigger.equals("leave"));
        long now = System.currentTimeMillis();
        for (ActiveRule r : active) {
            switch (r.rule.trigger) {
                case "join" -> tryFire(r, now, r.rule.delayMs);
                case "timer" -> r.nextTimerAt = now + (r.rule.delayMs > 0 ? r.rule.delayMs : r.rule.cooldownMs);
                default -> {
                }
            }
        }
    }

    /**
     * Auto-auth command fallback: fires once the login/register command shows up in the
     * command tree the server pushes after join (see onTick) — detection of an installed auth
     * mod, with no dependency on its messages, locales, or chat. The commands themselves are
     * the configurable templates in {@link Credentials.Store}. A server without a stored
     * password gets auto-registered with the default (or a random) password, which is saved
     * back to credentials.json. Global-password servers are not supported.
     */
    private static void sendAuthCommands(boolean hasLogin, boolean hasRegister) {
        if (Vault.locked()) {
            LOGGER.info("Skipping auto-auth on {}: credential storage is locked", serverAddress);
            return;
        }
        if (credentials == null || credentials.password == null) {
            if (!store.autoRegister || !hasRegister) {
                return;
            }
            ensureStoredPassword();
            LOGGER.info("Auto-registering on {}; the password is saved in {}", serverAddress, Vault.credentialsFile());
            sendResolved(store.registerCommand);
            return;
        }
        if (!store.autoLogin || !credentials.autoLogin || !hasLogin) {
            return;
        }
        // /register goes first when opted in: on a fresh account it registers (EasyAuth
        // authenticates right away), on an existing one it just fails and the /login applies.
        if (credentials.autoRegister && hasRegister) {
            sendResolved(store.registerCommand);
        }
        String login = store.loginCommand;
        String totpSecret = Vault.usable(credentials.totpSecret);
        boolean hasTotp = totpSecret != null && !totpSecret.isEmpty();
        // A separate 2FA command (AuthMe-style) takes precedence over EasyAuth's inline {otp}.
        boolean separateTotp = hasTotp && store.totpCommand != null && !store.totpCommand.isEmpty();
        if (hasTotp && !separateTotp && !login.contains("{otp}")) {
            login += " {otp}";
        }
        sendResolved(login);
        if (separateTotp) {
            // The code is only accepted once the server has verified the password, which it does
            // off the main thread (AuthMe hashes with BCrypt) — so this cannot go out in the same
            // tick. ponytail: fixed delay, make it configurable if a slow datasource needs longer.
            queueResolved(store.totpCommand, TOTP_COMMAND_DELAY_MS);
        }
    }

    /**
     * Hello from the server's EasyAuth (packet path): supersedes the command fallback.
     * The server routes the credentials to register or login by account state itself, and the
     * capability flags let the admin veto auto-auth for compliant clients entirely.
     * Login ladder: passkey (signed one-time challenge) -> session token -> password; a rejected
     * rung falls through to the next one in {@link #onResult}.
     * Called on the client main thread by the payload receiver in EasyAuthPackets.
     */
    public static void onHello(boolean canAutoLogin, boolean canAutoRegister, boolean registered, boolean authenticated,
                               boolean canSessionToken, boolean canPasskey, boolean hasPasskey, byte[] challenge) {
        if (!authPending) {
            return;
        }
        authPending = false;
        helloCanAutoLogin = canAutoLogin;
        helloCanSessionToken = canSessionToken;
        helloCanPasskey = canPasskey;
        helloHasPasskey = hasPasskey;
        if (Vault.locked()) {
            LOGGER.info("Skipping auto-auth on {}: credential storage is locked", serverAddress);
            return;
        }
        if (authenticated) {
            // Session still valid (or the server skips auth for this player) — nothing to log in
            // with, but a good moment to enroll a passkey for the next join (first key only).
            maybeEnrollPasskey();
            return;
        }
        if (!registered) {
            if (!canAutoRegister || !store.autoRegister) {
                return;
            }
            ensureStoredPassword();
            LOGGER.info("Auto-registering on {} via packet; the password is saved in {}", serverAddress, Vault.credentialsFile());
            EasyAuthPackets.sendCredentials(credentials.password, null);
            return;
        }
        if (tryPasskeyLogin(challenge) || tryTokenLogin()) {
            return;
        }
        tryPasswordLogin();
    }

    /**
     * Auth outcome from the server (packet path). SUCCESS carries the freshly issued/rotated
     * session token; rejections trigger the next rung of the login ladder.
     * Called on the client main thread by the payload receiver in EasyAuthPackets.
     */
    public static void onResult(int code, String sessionToken) {
        if (store == null) {
            return; // not connected / no tracked server
        }
        switch (code) {
            case ClientModProtocol.RESULT_SUCCESS -> {
                // No token writes while locked: it could not be encrypted, and the stored
                // (still-locked) token is dead anyway after this rotation — the password rung
                // recovers on the next unlocked join.
                if (sessionToken != null && !sessionToken.isEmpty() && store.useSessionToken && !Vault.locked()) {
                    ensureEntry().sessionToken = sessionToken;
                    Credentials.save(Vault.credentialsFile(), store);
                }
                maybeEnrollPasskey();
            }
            case ClientModProtocol.RESULT_TOKEN_REJECTED -> {
                // Rotated by another device or expired — drop it and fall back to the password.
                if (credentials != null && credentials.sessionToken != null) {
                    credentials.sessionToken = null;
                    Credentials.save(Vault.credentialsFile(), store);
                }
                tryPasswordLogin();
            }
            case ClientModProtocol.RESULT_PASSKEY_REJECTED -> {
                helloHasPasskey = false;
                // The server explicitly does not know this key (evicted, or revoked while other
                // keys remained) and we never auto-re-enroll an existing key — it is dead weight.
                // Drop it so the next successful login can enroll a fresh pair.
                if (credentials != null && credentials.passkeyPrivate != null) {
                    credentials.passkeyPublic = null;
                    credentials.passkeyPrivate = null;
                    Credentials.save(Vault.credentialsFile(), store);
                }
                if (!tryTokenLogin()) {
                    tryPasswordLogin();
                }
            }
            default -> {
            }
        }
    }

    /** Common gate of every auto-login rung: global switch + per-server entry + its own switch. */
    private static boolean autoAuthAllowed() {
        return store.autoLogin && credentials != null && credentials.autoLogin;
    }

    private static boolean tryPasskeyLogin(byte[] challenge) {
        if (!autoAuthAllowed() || !helloCanPasskey || !helloHasPasskey || !store.usePasskey
                || challenge == null || challenge.length == 0) {
            return false;
        }
        String privateKey = Vault.usable(credentials.passkeyPrivate);
        if (privateKey == null || credentials.passkeyPublic == null) {
            return false;
        }
        byte[] publicKey = Passkey.decodePublic(credentials.passkeyPublic);
        byte[] signature = Passkey.sign(privateKey, challenge);
        if (publicKey == null || signature == null) {
            return false;
        }
        LOGGER.info("Logging in on {} with a passkey", serverAddress);
        EasyAuthPackets.sendPasskey(publicKey, signature);
        return true;
    }

    private static boolean tryTokenLogin() {
        if (!autoAuthAllowed() || !helloCanSessionToken || !store.useSessionToken) {
            return false;
        }
        String token = Vault.usable(credentials.sessionToken);
        if (token == null || token.isEmpty()) {
            return false;
        }
        LOGGER.info("Logging in on {} with a session token", serverAddress);
        EasyAuthPackets.sendToken(token);
        return true;
    }

    private static void tryPasswordLogin() {
        if (!autoAuthAllowed() || !helloCanAutoLogin) {
            return;
        }
        String password = Vault.usable(credentials.password);
        if (password == null) {
            return;
        }
        EasyAuthPackets.sendCredentials(password, Totp.currentCode(Vault.usable(credentials.totpSecret)));
    }

    /**
     * Enrolls a freshly generated Ed25519 key while authenticated — only when we have no key
     * for this server yet. An existing local key the server does not know means it was revoked
     * server-side (/account passkey revoke) or evicted; auto-re-enrolling it would silently undo
     * that, so re-arming requires clearing the key in the config screen first.
     */
    private static void maybeEnrollPasskey() {
        if (!helloCanPasskey || !store.usePasskey || Vault.locked()) {
            return; // locked: a fresh private key could not be encrypted at rest
        }
        if (credentials != null && credentials.passkeyPrivate != null && credentials.passkeyPublic != null) {
            return;
        }
        String[] pair = Passkey.generate();
        if (pair == null) {
            return; // no Ed25519 in this JRE
        }
        Credentials entry = ensureEntry();
        entry.passkeyPublic = pair[0];
        entry.passkeyPrivate = pair[1];
        Credentials.save(Vault.credentialsFile(), store);
        byte[] publicKey = Passkey.decodePublic(entry.passkeyPublic);
        if (publicKey == null) {
            return;
        }
        LOGGER.info("Registering a passkey on {}", serverAddress);
        EasyAuthPackets.sendRegisterPasskey(publicKey);
    }

    /** The credentials entry for the current server, created (and registered in the store) on demand. */
    private static Credentials ensureEntry() {
        if (credentials == null) {
            credentials = new Credentials();
            store.servers.put(serverAddress, credentials);
        }
        return credentials;
    }

    /** Ensures {@link #credentials} has a stored password for this server, generating and saving one if needed. */
    private static void ensureStoredPassword() {
        Credentials entry = ensureEntry();
        if (entry.password == null) {
            entry.password = store.defaultPassword == null || store.defaultPassword.isEmpty()
                    ? randomPassword()
                    : store.defaultPassword;
        }
        Credentials.save(Vault.credentialsFile(), store);
    }

    /** First word of a command template, without the leading slash — the detection literal. */
    private static String commandLiteral(String template) {
        String s = template == null ? "" : template.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        int space = s.indexOf(' ');
        return space < 0 ? s : s.substring(0, space);
    }

    private static void sendResolved(String template) {
        String resolved = resolvePlaceholders(template);
        if (resolved != null && !resolved.isEmpty()) {
            send(resolved);
        }
    }

    /** Like {@link #sendResolved}, but sent by {@link #onTick} once the delay has elapsed. */
    private static void queueResolved(String template, long delay) {
        String resolved = resolvePlaceholders(template);
        if (resolved != null && !resolved.isEmpty()) {
            queue.add(new Pending(System.currentTimeMillis() + delay, resolved));
        }
    }

    private static String randomPassword() {
        // No look-alike characters (0/O, 1/l/I) — the password may need to be retyped by hand.
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return password.toString();
    }

    /**
     * @param sender profile id for player chat, null for system messages.
     */
    public static void onChat(String message, boolean system, UUID sender) {
        if (active.isEmpty()) {
            return;
        }
        // Anti-loop: never react to the echo of our own messages.
        var player = Minecraft.getInstance().player;
        if (sender != null && player != null && sender.equals(player.getUUID())) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ActiveRule r : active) {
            if (!r.rule.trigger.equals("chat") || r.pattern == null) {
                continue;
            }
            if (system ? r.rule.source.equals("player") : r.rule.source.equals("system")) {
                continue;
            }
            if (r.pattern.matcher(message).find()) {
                tryFire(r, now, r.rule.delayMs);
            }
        }
    }

    public static void onTick() {
        if (active.isEmpty() && queue.isEmpty() && !authPending) {
            return;
        }
        long now = System.currentTimeMillis();
        if (authPending) {
            if (now > authPendingUntil) {
                // Neither a hello nor the auth commands showed up — stop scanning the command
                // tree every tick for the rest of the session (most servers have no auth mod).
                authPending = false;
            } else {
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                // When the server declared the packet channel (EasyAuth 26.1+ on Fabric), wait for
                // its hello instead — see onHello. The command fallback covers everything else.
                if (connection != null && !EasyAuthPackets.serverSupportsPacketAuth()) {
                    var root = connection.getCommands().getRoot();
                    boolean hasLogin = root.getChild(loginLiteral) != null;
                    boolean hasRegister = root.getChild(registerLiteral) != null;
                    if (hasLogin || hasRegister) {
                        authPending = false;
                        sendAuthCommands(hasLogin, hasRegister);
                    }
                }
            }
        }
        for (ActiveRule r : active) {
            if (r.rule.trigger.equals("timer") && now >= r.nextTimerAt) {
                tryFire(r, now, 0);
                r.nextTimerAt = now + r.rule.cooldownMs;
            }
        }
        for (Iterator<Pending> it = queue.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (now >= p.dueAt) {
                send(p.line);
                it.remove();
            }
        }
    }

    public static void onDisconnect() {
        active = List.of();
        queue.clear();
        authPending = false;
        hasChatRules = hasLeaveRules = false;
        store = null;
        credentials = null;
        helloCanAutoLogin = helloCanSessionToken = helloCanPasskey = helloHasPasskey = false;
    }

    /** True when the current server has chat-triggered rules; gates the message flattening. */
    public static boolean hasChatRules() {
        return hasChatRules;
    }

    /** True when the current server has leave-triggered rules; gates the pause-screen hook. */
    public static boolean hasLeaveRules() {
        return hasLeaveRules;
    }

    /**
     * Fires the "leave" rules. Called from the wrapped pause-menu disconnect button (see
     * QuitHook) while the connection is still open — by onDisconnect the channel is already
     * closed and anything sent would go nowhere. Sends run immediately: a delayed send would
     * outlive the connection.
     */
    public static void onQuit() {
        long now = System.currentTimeMillis();
        for (ActiveRule r : active) {
            if (r.rule.trigger.equals("leave")) {
                tryFire(r, now, 0);
            }
        }
    }

    private static void tryFire(ActiveRule r, long now, long delay) {
        if (r.rule.maxRuns >= 0 && r.runs >= r.rule.maxRuns) {
            return;
        }
        if (r.rule.cooldownMs > 0 && now - r.lastRun < r.rule.cooldownMs) {
            return;
        }
        r.runs++;
        r.lastRun = now;
        for (String line : r.rule.send) {
            String resolved = resolvePlaceholders(line);
            if (resolved == null || resolved.isEmpty()) {
                continue;
            }
            if (delay <= 0) {
                send(resolved);
            } else {
                queue.add(new Pending(now + delay, resolved));
            }
        }
    }

    private static String resolvePlaceholders(String line) {
        String out = line
                .replace("{username}", Minecraft.getInstance().getUser().getName())
                .replace("{server}", serverAddress);
        // Never let an unresolved placeholder leak into chat as literal text — including a
        // still-encrypted (locked) value, which Vault.usable() reports as absent.
        if (out.contains("{password}")) {
            String password = credentials == null ? null : Vault.usable(credentials.password);
            if (password == null) {
                LOGGER.warn("Skipping rule line: no usable password for {} (none stored, or storage is locked)", serverAddress);
                return null;
            }
            out = out.replace("{password}", password);
        }
        if (out.contains("{otp}")) {
            String code = credentials == null ? null : Totp.currentCode(Vault.usable(credentials.totpSecret));
            if (code == null) {
                LOGGER.warn("Skipping rule line: no usable totpSecret for {} (none stored, invalid, or storage is locked)", serverAddress);
                return null;
            }
            out = out.replace("{otp}", code);
        }
        return out;
    }

    private static void send(String line) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        if (line.startsWith("/")) {
            connection.sendCommand(line.substring(1));
        } else {
            connection.sendChat(line);
        }
    }

    private static List<ActiveRule> loadRules(String address) {
        Rules.ServerRules entry = Rules.load(Vault.rulesFile()).servers.get(address);
        if (entry == null) {
            return List.of();
        }
        List<ActiveRule> result = new ArrayList<>();
        for (AutoInputRule rule : entry.rules) {
            if (rule == null || rule.send == null || rule.trigger == null) {
                continue;
            }
            // Gson happily deserializes ["cmd", null] — drop unusable lines instead of NPEing in tryFire.
            rule.send = rule.send.stream().filter(line -> line != null && !line.isEmpty()).toList();
            if (rule.send.isEmpty()) {
                LOGGER.warn("Ignoring rule for {} with no usable 'send' lines", address);
                continue;
            }
            rule.trigger = rule.trigger.toLowerCase(Locale.ROOT);
            rule.source = rule.source == null ? "any" : rule.source.toLowerCase(Locale.ROOT);
            Pattern pattern = null;
            switch (rule.trigger) {
                case "join", "leave" -> {
                }
                case "timer" -> {
                    if (rule.cooldownMs <= 0) {
                        LOGGER.warn("Ignoring timer rule for {} without cooldownMs > 0", address);
                        continue;
                    }
                }
                case "chat" -> {
                    if (rule.match == null) {
                        LOGGER.warn("Ignoring chat rule for {} without 'match' regex", address);
                        continue;
                    }
                    try {
                        pattern = Pattern.compile(rule.match);
                    } catch (PatternSyntaxException e) {
                        LOGGER.warn("Ignoring chat rule for {} with invalid regex '{}': {}", address, rule.match, e.getMessage());
                        continue;
                    }
                }
                default -> {
                    LOGGER.warn("Ignoring rule for {} with unknown trigger '{}'", address, rule.trigger);
                    continue;
                }
            }
            result.add(new ActiveRule(rule, pattern));
        }
        if (!result.isEmpty()) {
            LOGGER.info("Loaded {} auto-input rule(s) for {}", result.size(), address);
        }
        return result;
    }

    public static String normalizeAddress(String address) {
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(":25565")) {
            a = a.substring(0, a.length() - ":25565".length());
        }
        return a;
    }

    private static final class ActiveRule {
        final AutoInputRule rule;
        final Pattern pattern;
        int runs = 0;
        long lastRun = Long.MIN_VALUE / 2;
        long nextTimerAt = Long.MAX_VALUE;

        ActiveRule(AutoInputRule rule, Pattern pattern) {
            this.rule = rule;
            this.pattern = pattern;
        }
    }

    private record Pending(long dueAt, String line) {
    }
}
