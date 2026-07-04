package xyz.nikitacartes.easyauth.client.rules;

import java.util.List;

/**
 * One user-configured automation rule, deserialized from
 * {@code config/easyauth-client/rules.json}. Field names are the JSON keys.
 */
public final class AutoInputRule {
    public String trigger = "join"; // join | chat | timer | leave
    public String match;            // regex (find semantics), chat trigger only
    public String source = "any";  // any | system | player, chat trigger only
    public List<String> send = List.of(); // lines; leading '/' sends a command
    public long delayMs = 0;        // delay before sending; for timer: initial delay
    public int maxRuns = -1;        // fires per session; -1 = unlimited
    public long cooldownMs = 0;     // min interval between fires; for timer: the period
}
