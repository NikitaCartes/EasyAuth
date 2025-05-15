package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.integrations.Permissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.time.ZonedDateTime;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.checkGlobalPassword;
import static xyz.nikitacartes.easyauth.utils.AuthHelper.hashPassword;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogRegister;


public class RegisterCommand {

    // Registering the "/reg" alias
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> node = registerRegister(dispatcher);
        if (extendedConfig.aliases.register) {
            dispatcher.register(literal("reg")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .redirect(node));
        }
    }

    // Registering the "/register" command
    public static LiteralCommandNode<ServerCommandSource> registerRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            return dispatcher.register(literal("register")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .then(argument("globalPassword", string())
                            .then(argument("password", string())
                                    .then(argument("passwordAgain", string())
                                            .executes(ctx -> register(ctx.getSource(),
                                                    getString(ctx, "globalPassword"),
                                                    getString(ctx, "password"),
                                                    getString(ctx, "passwordAgain")))
                                    )
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPassword.send(ctx.getSource());
                        return 0;
                    }));
        } else {
            return dispatcher.register(literal("register")
                    .requires(Permissions.require("easyauth.commands.register", true))
                    .then(argument("password", string())
                            .then(argument("passwordAgain", string())
                                    .executes(ctx -> register(ctx.getSource(),
                                            getString(ctx, "password"),
                                            getString(ctx, "passwordAgain")))
                            )
                    )
                    .executes(ctx -> {
                        langConfig.enterPassword.send(ctx.getSource());
                        return 0;
                    }));
        }
    }

    private static int register(ServerCommandSource source, String globalPassword, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            if (checkGlobalPassword(globalPassword.toCharArray())) {
                return register(source, pass1, pass2);
            } else {
                PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();

                playerData.loginTries++;
                if (playerData.loginTries >= config.maxLoginTries && config.maxLoginTries != -1) { // Player exceeded maxLoginTries
                    LogRegister("Player " + player.getNameForScoreboard() + " exceeded global password tries limit.");
                    playerData.lastKickedDate = ZonedDateTime.now();
                    playerData.loginTries = 0;
                    playerData.update();
                    player.networkHandler.disconnect(langConfig.wrongGlobalPassword.get());
                    return 0;
                }
                langConfig.wrongGlobalPassword.send(source);
                return 0;
            }
        }
        return 0;
    }

    // Method called for hashing the password & writing to DB
    private static int register(ServerCommandSource source, String pass1, String pass2) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        // If self-registration is disabled in the config,
        // prevent players from registering themselves.
        if (!config.allowSelfRegister) {
            langConfig.registerOperatorOnly.send(source);
            return 0;
        }

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.loginRequired.send(source);
            return 0;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.alreadyAuthenticated.send(source);
            return 0;
        } else if (!pass1.equals(pass2)) {
            langConfig.matchPassword.send(source);
            return 0;
        }

        if (pass1.length() < extendedConfig.minPasswordLength) {
            langConfig.minPasswordChars.send(source, extendedConfig.minPasswordLength);
            return 0;
        } else if (pass1.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.maxPasswordChars.send(source, extendedConfig.maxPasswordLength);
            return 0;
        }

        PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();
        if (!playerData.password.isEmpty()) {
            langConfig.alreadyRegistered.send(source);
            return 0;
        }
        playerAuth.easyAuth$setAuthenticated(true);
        playerAuth.easyAuth$restoreTrueLocation();
        langConfig.registerSuccess.send(source);
        // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));

        THREADPOOL.submit(() -> {
            playerData.password = hashPassword(pass1.toCharArray());
            playerData.registrationDate = ZonedDateTime.now();
            playerData.lastIp = playerAuth.easyAuth$getIpAddress();
            playerData.lastAuthenticatedDate = ZonedDateTime.now();
            playerAuth.easyAuth$setPlayerEntryV1(playerData);
            playerData.update();

            LogRegister("Player " + player.getNameForScoreboard() + "{" + player.getUuidAsString() + "} successfully registered with password: " + playerData.password);
        });
        return 0;
    }
}
