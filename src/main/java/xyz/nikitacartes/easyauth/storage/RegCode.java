package xyz.nikitacartes.easyauth.storage;

import java.time.ZonedDateTime;

/**
 * A registration code: an admin-issued token that lets a limited number of players register
 * within a limited time window. Persisted as JSON in {@code config/EasyAuth/regcodes.json}
 * (see {@link RegCodeStore}); which code a player used is stored on the player entry
 * ({@link PlayerEntryV1#registeredWithCode}).
 */
public class RegCode {
    /** The secret players type in {@code /register <code> ...}. Also the map key in the store. */
    public String code;
    /** Optional human-readable label, used interchangeably with the code in admin commands. May be null. */
    public String alias;
    /** Maximum number of registrations allowed. 0 = unlimited. */
    public int maxUses;
    /** How many players have registered with this code so far. */
    public int uses;
    /** Moment the code stops working. null = never expires. */
    public ZonedDateTime expiresAt;
    /** Who created the code (player name, or "Console"). */
    public String createdBy;
    /** When the code was created. */
    public ZonedDateTime createdAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(ZonedDateTime.now());
    }

    public boolean isExhausted() {
        return maxUses > 0 && uses >= maxUses;
    }
}
