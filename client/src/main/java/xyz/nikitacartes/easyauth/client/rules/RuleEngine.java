package xyz.nikitacartes.easyauth.client.rules;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static String serverAddress = "";
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
        credentials = null;
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null) {
            return; // singleplayer/realms
        }
        serverAddress = normalizeAddress(server.ip);
        credentials = Credentials.load(credentialsFile, serverAddress);
        long now = System.currentTimeMillis();
        List<ActiveRule> rules = new ArrayList<>(loadRules(serverAddress));
        addBuiltinAuthRules(rules, now);
        active = rules;
        if (active.isEmpty()) {
            return;
        }
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
     * Auto-login (plan §4, variant A): built-in rules for servers with saved credentials.
     * The chat rule matches EasyAuth's default en/ru prompts; the one-shot join fallback
     * covers 1.21.6+ dialog servers and custom locales, where no chat prompt is ever sent.
     * Both share one ActiveRule, so the cooldown prevents double-sending.
     */
    private static void addBuiltinAuthRules(List<ActiveRule> rules, long now) {
        if (credentials == null || credentials.password == null || !credentials.autoLogin) {
            return;
        }
        AutoInputRule login = new AutoInputRule();
        login.trigger = "chat";
        login.match = "Use /login|Используйте /login|Введите /login";
        login.send = List.of(credentials.totpSecret == null || credentials.totpSecret.isEmpty()
                ? "/login {password}"
                : "/login {password} {otp}");
        login.maxRuns = 5;
        login.cooldownMs = 5000;
        ActiveRule loginRule = new ActiveRule(login, Pattern.compile(login.match));
        loginRule.oneShotAt = now + 3000;
        rules.add(loginRule);

        if (credentials.autoRegister) {
            AutoInputRule register = new AutoInputRule();
            register.trigger = "chat";
            // Deliberately not matching the <global password> prompt variants:
            // the global password is not stored, a 2-argument /register would be wrong there.
            register.match = "Use /register <password>|Введите /register <пароль>";
            register.send = List.of("/register {password} {password}");
            register.maxRuns = 1;
            register.cooldownMs = 5000;
            rules.add(new ActiveRule(register, Pattern.compile(register.match)));
        }
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
        if (active.isEmpty() && queue.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ActiveRule r : active) {
            if (r.rule.trigger.equals("timer") && now >= r.nextTimerAt) {
                tryFire(r, now, 0);
                r.nextTimerAt = now + r.rule.cooldownMs;
            }
            if (now >= r.oneShotAt) {
                r.oneShotAt = Long.MAX_VALUE;
                tryFire(r, now, 0);
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
        long oneShotAt = Long.MAX_VALUE; // built-in join fallback (see addBuiltinAuthRules)

        ActiveRule(AutoInputRule rule, Pattern pattern) {
            this.rule = rule;
            this.pattern = pattern;
        }
    }

    private record Pending(long dueAt, String line) {
    }
}
