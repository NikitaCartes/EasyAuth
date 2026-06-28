package xyz.nikitacartes.easyauth.commands;
//? if fabric {

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.EasyAuthPermissions;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.proxy.ProxyBridge;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.io.IOException;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.integrations.MojangApi.isValidUsername;

/**
 * {@code /premium enable|disable <password>} — opt a premium player into passwordless auto-login via
 * the AuthMe proxy-bridge. {@code enable} starts a pending verification (the proxy will Mojang-verify
 * the player on their next reconnect); the account is confirmed only when that verification succeeds.
 *
 * <p>Fabric only, matching {@link ProxyBridge}; registered only when the proxy bridge is enabled.
 */
public class PremiumCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("premium")
                .requires(EasyAuthPermissions.require("easyauth.commands.premium", true))
                .executes(ctx -> usage(ctx.getSource()))
                .then(literal("enable")
                        .then(argument("password", string())
                                .executes(ctx -> enable(ctx.getSource(), getString(ctx, "password")))))
                .then(literal("disable")
                        .then(argument("password", string())
                                .executes(ctx -> disable(ctx.getSource(), getString(ctx, "password"))))));
    }

    private static int usage(CommandSourceStack source) {
        langConfig.premium.usage.send(source);
        return 1;
    }

    private static int enable(CommandSourceStack source, String password) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (!ProxyBridge.isEnabled()) {
            langConfig.premium.bridgeDisabled.send(source);
            return 0;
        }
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }

        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, password.toCharArray()) != AuthHelper.PasswordOptions.CORRECT) {
                langConfig.password.incorrect.send(source);
                return;
            }
            String username = StoneCutterUtils.getUsername(player);
            try {
                if (!isValidUsername(username)) {
                    langConfig.account.onlineNotFound.send(source);
                    return;
                }
            } catch (IOException e) {
                langConfig.error.mojangUnavailable.send(source);
                return;
            }
            player.server.execute(() -> {
                ProxyBridge.enrollPending(player);
                langConfig.premium.enabled.send(source);
            });
        });
        return 1;
    }

    private static int disable(CommandSourceStack source, String password) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }

        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, password.toCharArray()) != AuthHelper.PasswordOptions.CORRECT) {
                langConfig.password.incorrect.send(source);
                return;
            }
            PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
            entry.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
            entry.update();
            player.server.execute(() -> {
                ProxyBridge.unenroll(player);
                langConfig.premium.disabled.send(source);
            });
        });
        return 1;
    }
}
//?}
