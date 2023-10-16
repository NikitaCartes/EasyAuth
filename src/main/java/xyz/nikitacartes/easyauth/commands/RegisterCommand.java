package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.TranslationHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.hashPassword;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;


public class RegisterCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Registering the "/register" command
        dispatcher.register(literal("register")
                .requires(Permissions.require("easyauth.commands.register", true))
                .then(argument("password", string())
                        .then(argument("passwordAgain", string())
                                .executes(ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
                        ))
                .executes(ctx -> {
                    ctx.getSource().getPlayerOrThrow().sendMessage(TranslationHelper.getEnterPassword(), false);
                    return 0;
                }));
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if (config.main.enableGlobalPassword) {
            player.sendMessage(TranslationHelper.getLoginRequired(), false);
            return 0;
        } else if (((PlayerAuth) player).isAuthenticated()) {
            player.sendMessage(TranslationHelper.getAlreadyAuthenticated(), false);
            return 0;
        } else if (!pass1.equals(pass2)) {
            player.sendMessage(TranslationHelper.getMatchPassword(), false);
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (pass1.length() < config.main.minPasswordChars) {
                player.sendMessage(TranslationHelper.getMinPasswordChars(), false);
                return;
            } else if (pass1.length() > config.main.maxPasswordChars && config.main.maxPasswordChars != -1) {
                player.sendMessage(TranslationHelper.getMaxPasswordChars(), false);
                return;
            }

            PlayerCache playerCache = playerCacheMap.get(((PlayerAuth) player).getFakeUuid());
            if (playerCache.password.isEmpty()) {
                ((PlayerAuth) player).setAuthenticated(true);
                player.sendMessage(TranslationHelper.getRegisterSuccess(), false);
                // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                playerCache.password = hashPassword(pass1.toCharArray());
                LogDebug("Player " + player.getName().getString() + "(" + player.getUuidAsString() + ") successfully registered with password: " + playerCache.password);
                return;
            }
            player.sendMessage(TranslationHelper.getAlreadyRegistered(), false);
        });
        return 0;
    }
}
