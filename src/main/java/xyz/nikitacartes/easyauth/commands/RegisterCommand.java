package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.hashPassword;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.TranslationHelper.*;


public class RegisterCommand {

    // Registering the "/reg" alias
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> node = registerRegister(dispatcher);
        if (extendedConfig.enableAliases) {
            dispatcher.register(literal("reg")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .redirect(node));
        }
    }

    // Registering the "/register" command
    public static LiteralCommandNode<ServerCommandSource> registerRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(literal("register")
                .requires(Permissions.require("easyauth.commands.register", true))
                .then(argument("password", string())
                        .then(argument("passwordAgain", string())
                                .executes(ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
                        ))
                .executes(ctx -> {
                    sendEnterPassword(ctx.getSource());
                    return 0;
                }));
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if (config.enableGlobalPassword) {
            sendLoginRequired(source);
            return 0;
        } else if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            sendAlreadyAuthenticated(source);
            return 0;
        } else if (!pass1.equals(pass2)) {
            sendMatchPassword(source);
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (pass1.length() < extendedConfig.minPasswordLength) {
                sendMinPasswordChars(source);
                return;
            } else if (pass1.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
                sendMaxPasswordChars(source);
                return;
            }

            PlayerCache playerCache = playerCacheMap.get(((PlayerAuth) player).easyAuth$getFakeUuid());
            if (playerCache.password.isEmpty()) {
                ((PlayerAuth) player).easyAuth$setAuthenticated(true);
                sendRegisterSuccess(source);
                // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                playerCache.password = hashPassword(pass1.toCharArray());
                LogDebug("Player " + player.getName().getString() + "(" + player.getUuidAsString() + ") successfully registered with password: " + playerCache.password);
                return;
            }
            sendAlreadyRegistered(source);
        });
        return 0;
    }
}
