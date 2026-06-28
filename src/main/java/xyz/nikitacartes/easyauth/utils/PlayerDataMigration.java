package xyz.nikitacartes.easyauth.utils;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

/**
 * One home for EasyAuth's offline&rarr;online player-data migration: the <em>decision</em> (when a
 * player's offline-UUID world data should follow them to the UUID they actually connected with) and
 * the <em>file move</em> itself.
 *
 * <p>This used to be copy-pasted across {@code PlayerDataStorage*Mixin}, {@code PlayerListMixin},
 * {@code PlayerAdvancementsMixin} and {@code AuthCommand}, each with a slightly different gate. They
 * all call {@link #shouldMigrate} now.
 */
public final class PlayerDataMigration {

    private PlayerDataMigration() {
    }

    /** The offline UUID EasyAuth keys a username's data on (Minecraft's {@code OfflinePlayer:<name>}). */
    public static UUID offlineUuid(String username) {
        return UUIDUtil.createOfflinePlayerUUID(username);
    }

    /**
     * Whether the offline-UUID world data for {@code username} should be migrated to
     * {@code connectionUuid} when the player loads.
     *
     * <p>True exactly when the player connected with a non-offline, authoritative UUID: a premium
     * player on an {@code online-mode=true} server, or a proxy-forwarded premium player on an offline
     * backend running with AuthMe's {@code keepOfflineUuidCompatibility=false}. A cracked/offline
     * player connects with the offline UUID itself, so there is nothing to migrate; it is also false
     * when {@code forced-offline-uuid} is on, since then everyone deliberately keeps the offline UUID.
     */
    public static boolean shouldMigrate(String username, UUID connectionUuid) {
        return !extendedConfig.forcedOfflineUuid && !connectionUuid.equals(offlineUuid(username));
    }

    /**
     * If {@code username} is joining with a non-offline UUID, copies their offline-UUID world data
     * (playerdata, stats, advancements) onto that connection UUID and renames the offline originals to
     * {@code .migrated} backups, so Minecraft loads the right files when it constructs the player.
     *
     * <p>Call this <em>before</em> the {@link net.minecraft.server.level.ServerPlayer} is built (its
     * constructor already loads stats and advancements) — EasyAuth runs it from the
     * {@code canPlayerLogin} hook. Idempotent: once migrated the offline files are gone and the
     * online ones exist, so later joins do nothing. Never overwrites existing online data.
     */
    public static void migrateOnJoin(MinecraftServer server, String username, UUID connectionUuid) {
        if (!shouldMigrate(username, connectionUuid)) {
            return;
        }
        UUID offline = offlineUuid(username);
        try {
            copyThenBackup(server, offline, connectionUuid, username, true);
        } catch (IOException e) {
            LogWarn("Failed to migrate offline data for " + username + " (" + offline + " -> " + connectionUuid + "): " + e.getMessage());
        }
    }

    /**
     * Moves a player's data files (playerdata, stats, advancements) from one UUID to another.
     * Refuses (returns {@code false}) without moving anything if any destination file already exists.
     */
    public static boolean copyThenBackup(MinecraftServer server, UUID offline, UUID online, @Nullable String username, boolean force) throws IOException {
        Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path stats = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path advancements = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);

        Path[][] files = {
                {playerData.resolve(offline + ".dat"), playerData.resolve(online + ".dat")},
                {playerData.resolve(offline + ".dat_old"), playerData.resolve(online + ".dat_old")},
                {stats.resolve(offline + ".json"), stats.resolve(online + ".json")},
                {advancements.resolve(offline + ".json"), advancements.resolve(online + ".json")},
        };

        if (!force) {
            for (Path[] move : files) {
                if (Files.exists(move[1])) {
                    return false;
                }
            }
        }

        for (Path[] file : files) {
            Path src = file[0];
            Path dst = file[1];
            // Only migrate when the source exists and we would not clobber existing online data.
            if (Files.exists(src) && !Files.exists(dst)) {
                Files.copy(src, dst);
                Files.move(src, src.resolveSibling(src.getFileName() + ".migrated"));
                LogDebug("Migrated " + src.getFileName() + " -> " + dst.getFileName() + (username != null ? " for player " + username : ""));
            }
        }
        return true;
    }
}
