package xyz.nikitacartes.easyauth.integrations;

import net.minecraft.server.permissions.Permission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static me.lucko.fabric.api.permissions.v0.Permissions.check;
import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

public class PermissionsWrapper {

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, boolean defaultValue) {
        if (technicalConfig.permissionsLoaded) {
            return source -> check(source, permission, defaultValue);
        } else {
            return source -> defaultValue;
        }
    }

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, int defaultRequiredLevel) {
        if (technicalConfig.permissionsLoaded) {
            return source -> check(source, permission, defaultRequiredLevel);
        } else {
            return source -> source.permissions().hasPermission(permissionLevelFromInt(defaultRequiredLevel));
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
