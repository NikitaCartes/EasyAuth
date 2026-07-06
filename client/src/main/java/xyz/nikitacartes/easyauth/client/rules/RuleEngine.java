package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nikitacartes.easyauth.client.EasyAuthPackets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final Gson GSON = new Gson();

    private static Path configFile;
    private static Path credentialsFile;

    // Session state; empty when not connected or no rules for the current server.
    private static List<ActiveRule> active = List.of();
    private static final List<Pending> queue = new ArrayList<>();
    // Built-in auto-auth; fires once /login or /register appears in the server's command tree (see onTick).
    private static boolean authPending;
    private static String serverAddress = "";
    private static Credentials.Store store;
    private static Credentials credentials;

    private RuleEngine() {
    }

    public static void init(Path configDir) {
        Path dir = configDir.resolve("easyauth-client");
        configFile = dir.resolve("rules.json");
        credentialsFile = dir.resolve("credentials.json");
    }

    public static Path getCredentialsFile() {
        return credentialsFile;
    }

    public static void onJoin() {
        queue.clear();
        active = List.of();
        authPending = false;
        store = null;
        credentials = null;
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null) {
            return; // singleplayer/realms
        }
        serverAddress = normalizeAddress(server.ip);
        store = Credentials.load(credentialsFile);
        credentials = store.servers.get(serverAddress);
        if (credentials != null && credentials.password != null) {
            LOGGER.warn("Using credentials for {} from {} — this file is stored as plain text", serverAddress, credentialsFile);
        }
        authPending = store.autoLogin || store.autoRegister;
        active = loadRules(serverAddress);
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
     * Auto-auth (plan §4, variant A): fires once the login/register command shows up in the
     * command tree the server pushes after join (see onTick) — detection of an installed auth
     * mod, with no dependency on its messages, locales, or chat. The commands themselves are
     * the configurable templates in {@link Credentials.Store}. A server without a stored
     * password gets auto-registered with the default (or a random) password, which is saved
     * back to credentials.json. Global-password servers are not supported.
     */
    private static void sendAuthCommands(boolean hasLogin, boolean hasRegister) {
        if (credentials == null || credentials.password == null) {
            if (!store.autoRegister || !hasRegister) {
                return;
            }
            ensureStoredPassword();
            LOGGER.info("Auto-registering on {}; the password is saved in {}", serverAddress, credentialsFile);
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
        if (credentials.totpSecret != null && !credentials.totpSecret.isEmpty() && !login.contains("{otp}")) {
            login += " {otp}";
        }
        sendResolved(login);
    }

    /**
     * Hello from the server's EasyAuth (packet path, plan §6): supersedes the command fallback.
     * The server routes the credentials to register or login by account state itself, and the
     * capability flags let the admin veto auto-auth for compliant clients entirely.
     * Called on the client main thread by the payload receiver in EasyAuthPackets.
     */
    public static void onHello(boolean canAutoLogin, boolean canAutoRegister, boolean registered, boolean authenticated) {
        if (!authPending) {
            return;
        }
        authPending = false;
        if (authenticated) {
            return; // session still valid (or the server skips auth for this player)
        }
        if (!registered) {
            if (!canAutoRegister || !store.autoRegister) {
                return;
            }
            ensureStoredPassword();
            LOGGER.info("Auto-registering on {} via packet; the password is saved in {}", serverAddress, credentialsFile);
            EasyAuthPackets.sendCredentials(credentials.password, null);
            return;
        }
        if (!canAutoLogin || !store.autoLogin
                || credentials == null || credentials.password == null || !credentials.autoLogin) {
            return;
        }
        EasyAuthPackets.sendCredentials(credentials.password, Totp.currentCode(credentials.totpSecret));
    }

    /** Ensures {@link #credentials} has a stored password for this server, generating and saving one if needed. */
    private static void ensureStoredPassword() {
        Credentials entry = credentials != null ? credentials : new Credentials();
        if (entry.password == null) {
            entry.password = store.defaultPassword == null || store.defaultPassword.isEmpty()
                    ? randomPassword()
                    : store.defaultPassword;
        }
        credentials = entry;
        store.servers.put(serverAddress, entry);
        Credentials.save(credentialsFile, store);
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
        if (authPending) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            // When the server declared the packet channel (EasyAuth 26.1+ on Fabric), wait for
            // its hello instead — see onHello. The command fallback covers everything else.
            if (connection != null && !EasyAuthPackets.serverSupportsPacketAuth()) {
                var root = connection.getCommands().getRoot();
                boolean hasLogin = root.getChild(commandLiteral(store.loginCommand)) != null;
                boolean hasRegister = root.getChild(commandLiteral(store.registerCommand)) != null;
                if (hasLogin || hasRegister) {
                    authPending = false;
                    sendAuthCommands(hasLogin, hasRegister);
                }
            }
        }
        long now = System.currentTimeMillis();
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
        long now = System.currentTimeMillis();
        for (ActiveRule r : active) {
            if (r.rule.trigger.equals("leave")) {
                tryFire(r, now, 0); // a delayed send would outlive the connection
            }
        }
        active = List.of();
        queue.clear();
        authPending = false;
        store = null;
        credentials = null;
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
        // Never let an unresolved placeholder leak into chat as literal text.
        if (out.contains("{password}")) {
            if (credentials == null || credentials.password == null) {
                LOGGER.warn("Skipping rule line: no password stored for {} in credentials.json", serverAddress);
                return null;
            }
            out = out.replace("{password}", credentials.password);
        }
        if (out.contains("{otp}")) {
            String code = credentials == null ? null : Totp.currentCode(credentials.totpSecret);
            if (code == null) {
                LOGGER.warn("Skipping rule line: no valid totpSecret stored for {} in credentials.json", serverAddress);
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
        RulesFile file;
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                Files.writeString(configFile, "{\n  \"servers\": {}\n}\n");
                LOGGER.info("Created empty rules config at {}", configFile);
                return List.of();
            }
            file = GSON.fromJson(Files.readString(configFile), RulesFile.class);
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Could not read {}: {}", configFile, e.toString());
            return List.of();
        }
        if (file == null || file.servers == null) {
            return List.of();
        }
        ServerRules entry = file.servers.get(address);
        if (entry == null || entry.rules == null) {
            return List.of();
        }
        List<ActiveRule> result = new ArrayList<>();
        for (AutoInputRule rule : entry.rules) {
            if (rule == null || rule.send == null || rule.trigger == null) {
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

    private static final class RulesFile {
        Map<String, ServerRules> servers;
    }

    private static final class ServerRules {
        List<AutoInputRule> rules;
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
