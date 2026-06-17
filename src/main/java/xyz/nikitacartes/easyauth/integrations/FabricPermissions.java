package xyz.nikitacartes.easyauth.integrations;

//? if >= 1.21.11 {
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
//?}
//? if neoforge {
/*import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.level.ServerPlayer;
*///?}
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

//? if fabric {
import static me.lucko.fabric.api.permissions.v0.Permissions.check;
//?}
import static xyz.nikitacartes.easyauth.EasyAuth.technicalConfig;

/**
 * Permission lookup.
 *
 * <p>On Fabric this delegates to {@code fabric-permissions-api-v0} (lucko).
 * NeoForge has no equivalent, so there it queries LuckPerms natively.
 * The permission nodes  are identical on both loaders.
 */
public class FabricPermissions {

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, boolean defaultValue) {
        //? if neoforge {
        /*if (technicalConfig.luckPermsLoaded) {
            return source -> luckPermsCheck(source, permission, defaultValue);
        }
        return source -> defaultValue;
        *///?} else {
        if (technicalConfig.permissionsLoaded) {
            return source -> check(source, permission, defaultValue);
        } else {
            return source -> defaultValue;
        }
        //?}
    }

    public static @NotNull Predicate<CommandSourceStack> require(@NotNull String permission, int defaultRequiredLevel) {
        //? if neoforge {
        /*if (technicalConfig.luckPermsLoaded) {
            return source -> luckPermsCheck(source, permission, vanillaOp(source, defaultRequiredLevel));
        }
        return source -> vanillaOp(source, defaultRequiredLevel);
        *///?} else {
        if (technicalConfig.permissionsLoaded) {
            return source -> check(source, permission, defaultRequiredLevel);
        } else {
            return source -> vanillaOp(source, defaultRequiredLevel);
        }
        //?}
    }

    private static boolean vanillaOp(CommandSourceStack source, int level) {
        //? if >= 1.21.11 {
        return source.permissions().hasPermission(permissionLevelFromInt(level));
        //?} else {
        /*return source.hasPermission(level);
        *///?}
    }

    //? if neoforge {
    /*private static boolean luckPermsCheck(CommandSourceStack source, String permission, boolean fallback) {
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
            Tristate tristate = user.getCachedData().getPermissionData().checkPermission(permission);
            if (tristate == Tristate.TRUE) {
                return true;
            }
            if (tristate == Tristate.FALSE) {
                return false;
            }
            return fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }
    *///?}

    //? if >= 1.21.11 {
    static Permission permissionLevelFromInt(int level) {
        return switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;
            default -> throw new IllegalArgumentException("Invalid permission level: " + level);
        };
    }
    //?}
}
