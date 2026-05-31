package xyz.nikitacartes.easyauth.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

public class PermissionsWrapper {

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, boolean defaultValue) {
        if (technicalConfig.luckPermsLoaded) {
            return source -> luckPermsCheck(source, permission, defaultValue);
        }
        return source -> defaultValue;
    }

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, int defaultRequiredLevel) {
        if (technicalConfig.luckPermsLoaded) {
            return source -> luckPermsCheck(source, permission,
                    source.permissions().hasPermission(permissionLevelFromInt(defaultRequiredLevel)));
        }
        return source -> source.permissions().hasPermission(permissionLevelFromInt(defaultRequiredLevel));
    }

    private static boolean luckPermsCheck(CommandSourceStack source, String permission, boolean fallback) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                // Console / command block / RCON: treat as fully privileged.
                return true;
            }
            var user = api.getUserManager().getUser(player.getUUID());
            if (user == null) {
                return fallback;
            }
            // LuckPerms API: checkPermission returns Tristate directly in 5.x.
            Tristate tristate = user.getCachedData().getPermissionData().checkPermission(permission);
            if (tristate == Tristate.TRUE) {
                return true;
            }
            if (tristate == Tristate.FALSE) {
                return false;
            }
            return fallback;
        } catch (IllegalStateException ignored) {
            return fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    static Permission permissionLevelFromInt(int level) {
        return switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;
            default -> throw new IllegalArgumentException("Invalid permission level: " + level);
        };
    }
}
