package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.AuthHelper;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class RegisterCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {

        // Registering the "/register" command
        dispatcher.register(literal("register")
            .then(argument("password", word())
                .then(argument("passwordAgain", word())
                    .executes( ctx -> register(ctx.getSource(), getString(ctx, "password"), getString(ctx, "passwordAgain")))
            ))
        .executes(ctx -> {
            ctx.getSource().getPlayer().sendMessage(new LiteralText(EasyAuth.config.lang.enterPassword), false);
            return 0;
        }));
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if(EasyAuth.config.main.enableGlobalPassword) {
            player.sendMessage(new LiteralText(EasyAuth.config.lang.loginRequired), false);
            return 0;
        }
        else if(((PlayerAuth) player).isAuthenticated()) {
            player.sendMessage(new LiteralText(EasyAuth.config.lang.alreadyAuthenticated), false);
            return 0;
        }
        else if(!pass1.equals(pass2)) {
            player.sendMessage(new LiteralText(EasyAuth.config.lang.matchPassword), false);
            return 0;
        }
        // Different thread to avoid lag spikes
        EasyAuth.THREADPOOL.submit(() -> {
            if(pass1.length() < EasyAuth.config.main.minPasswordChars) {
                player.sendMessage(new LiteralText(
                        String.format(EasyAuth.config.lang.minPasswordChars, EasyAuth.config.main.minPasswordChars)
                ), false);
                return;
            }
            else if(pass1.length() > EasyAuth.config.main.maxPasswordChars && EasyAuth.config.main.maxPasswordChars != -1) {
                player.sendMessage(new LiteralText(
                        String.format(EasyAuth.config.lang.maxPasswordChars, EasyAuth.config.main.maxPasswordChars)
                ), false);
                return;
            }

            PlayerCache playerCache = EasyAuth.playerCacheMap.get(((PlayerAuth) player).getFakeUuid());
            if (playerCache.password.isEmpty()) {
                ((PlayerAuth) player).setAuthenticated(true);
                player.sendMessage(new LiteralText(EasyAuth.config.lang.registerSuccess), false);

                playerCache.password = AuthHelper.hashPassword(pass1.toCharArray());
                return;
            }
            player.sendMessage(new LiteralText(EasyAuth.config.lang.alreadyRegistered), false);
        });
        return 0;
    }
}
