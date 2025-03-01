package xyz.nikitacartes.easyauth.commands;

import com.bastiaanjansen.otp.SecretGenerator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.nio.charset.StandardCharsets;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("account")
                .requires(Permissions.require("easyauth.commands.account.root", true))
                .then(literal("unregister")
                        .requires(Permissions.require("easyauth.commands.account.unregister", true))
                        .executes(ctx -> {
                            langConfig.enterPassword.send(ctx.getSource());
                            return 1;
                        })
                        .then(argument("password", string())
                                .executes(ctx -> unregister(
                                                ctx.getSource(),
                                                getString(ctx, "password")
                                        )
                                )
                        )
                )
                .then(literal("changePassword")
                        .requires(Permissions.require("easyauth.commands.account.changePassword", true))
                        .then(argument("old password", string())
                                .executes(ctx -> {
                                    langConfig.enterNewPassword.send(ctx.getSource());
                                    return 1;
                                })
                                .then(argument("new password", string())
                                        .executes(ctx -> changePassword(
                                                        ctx.getSource(),
                                                        getString(ctx, "old password"),
                                                        getString(ctx, "new password")
                                                )
                                        )
                                )
                        )
                )
                .then(literal("online")
                        .requires(Permissions.require("easyauth.commands.account.online", true))
                        .then(argument("password", string())
                                .executes(ctx -> markAsOnline(
                                        ctx.getSource(),
                                        getString(ctx, "password"),
                                        false
                                        )
                                )
                                .then(argument("confirm", bool())
                                        .executes(ctx -> markAsOnline(
                                                ctx.getSource(),
                                                getString(ctx, "password"),
                                                getBool(ctx, "confirm")
                                                )
                                        )
                                )
                        )
                )
        );
        if (config.otpEnabled) {
            dispatcher.register(literal("account")
                    .then(literal("otp")
                            .requires(Permissions.require("easyauth.commands.account.otp", true))
                            .then(literal("enable")
                                    .requires(Permissions.require("easyauth.commands.account.otp.enable", true))
                                    .executes(ctx -> generateOtp(ctx.getSource())))
                            .then(literal("enable")
                                    .requires(Permissions.require("easyauth.commands.account.otp.enable", true))
                                    .then(argument("otp code", string())
                                            .executes(ctx -> enableOtp(
                                                    ctx.getSource(),
                                                    getString(ctx, "otp code")))))
                            .then(literal("disable")
                                    .requires(Permissions.require("easyauth.commands.account.otp.disable", true))
                                    .then(argument("otp code", string())
                                            .executes(ctx -> disableOtp(
                                                    ctx.getSource(),
                                                    getString(ctx, "otp code")))))
                            .then(literal("enable2Fa").requires(Permissions.require("easyauth.commands.account.enable2Fa", true))
                                    .then(argument("password", string())
                                            .then(argument("otp code", string())
                                                    .executes(ctx -> enable2Fa(
                                                            ctx.getSource(),
                                                            getString(ctx, "password"),
                                                            getString(ctx, "otp code"))))))
                            .then(literal("disable2Fa").requires(Permissions.require("easyauth.commands.account.disable2Fa", true))
                                    .then(argument("password", string()).then(argument("otp code", string())
                                            .executes(ctx -> disable2Fa(ctx.getSource(),
                                                    getString(ctx, "password"),
                                                    getString(ctx, "otp code")))
                                            )
                                    )
                            )
                    )
            );
        }
    }

    // Method called for checking the password and then removing user's account from db
    private static int unregister(ServerCommandSource source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.cannotUnregister.send(source);
            return 0;
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.cannotUnregister.send(source);
            return 0;
        }

        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.loginRequired.send(source);
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            String username = player.getNameForScoreboard();
            if (AuthHelper.checkPassword(playerAuth, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                DB.deleteUserData(username);
                langConfig.accountDeleted.send(source);
                playerAuth.easyAuth$setAuthenticated(false);
                playerAuth.easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
                player.networkHandler.disconnect(langConfig.accountDeleted.get());
                return;
            }
            langConfig.wrongPassword.send(source);
        });
        return 0;
    }

    // Method called for checking the password and then changing it
    private static int changePassword(ServerCommandSource source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.cannotChangePassword.send(source);
            return 0;
        }
        if (newPass.length() < extendedConfig.minPasswordLength) {
            langConfig.minPasswordChars.send(source);
            return 0;
        } else if (newPass.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.maxPasswordChars.send(source);
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                // Changing password

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.password = AuthHelper.hashPassword(newPass.toCharArray());
                playerEntry.update();

                langConfig.passwordUpdated.send(source);
            } else {
                langConfig.wrongPassword.send(source);
            }
        });
        return 0;
    }

    /**
     * Set player as player with online account
     *
     * @param source   executioner of the command
     * @param password password of the player
     * @return 0
     */
    private static int markAsOnline(ServerCommandSource source, String password) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, password.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
                playerEntry.update();

                langConfig.selfMarkAsOnline.send(source);
            } else {
                langConfig.wrongPassword.send(source);
            }
        });

        return 1;
    }

    private static int markAsOnline(ServerCommandSource source, String password, boolean confirm) throws CommandSyntaxException {
        if (!confirm) {
            langConfig.selfMarkAsOnlineWarning.send(source);
            return 0;
        }
        return markAsOnline(source, password);
    }

    private static int generateOtp(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.otpProhibited.send(source);
            return 0;
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.otpProhibited.send(source);
            return 0;
        }

        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.loginRequired.send(source);
            return 0;
        }

        if (playerEntry.otpEnabled) {
            langConfig.otpAlreadyEnabled.send(source);
            return 0;
        }

        playerEntry.otpSecret = new String(SecretGenerator.generate(), StandardCharsets.UTF_8);

        MutableText message = langConfig.otpGenerated.get();
        message.append("\n")
                .append(Text.translatable(playerEntry.otpSecret)
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerEntry.otpSecret)))
                        .formatted(Formatting.YELLOW));
        source.sendMessage(message);

        playerEntry.update();
        return 1;
    }

    private static int enableOtp(ServerCommandSource source, String otpCode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.otpProhibited.send(source);
            return 0;
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.otpProhibited.send(source);
            return 0;
        }

        if (playerEntry.otpSecret == null) {
            langConfig.otpSecretNotGenerated.send(source);
            return 0;
        }

        if (playerEntry.otpEnabled) {
            langConfig.otpAlreadyEnabled.send(source);
            return 0;
        }

        if (playerEntry.verifyOtp(otpCode)) {
            playerEntry.otpEnabled = true;
            playerEntry.twoFactorAuthRequired = true;
            langConfig.otpEnabled.send(source);
            playerEntry.update();
            return 1;
        }

        playerEntry.otpSecret = null;
        langConfig.otpEnableFailed.send(source);
        playerEntry.update();
        return 0;
    }

    private static int disableOtp(ServerCommandSource source, String otpCode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();

        if (!playerEntry.otpEnabled) {
            langConfig.otpAlreadyDisabled.send(source);
            return 0;
        }

        if (playerEntry.verifyOtp(otpCode)) {
            playerEntry.otpEnabled = false;
            playerEntry.otpSecret = null;
            playerEntry.twoFactorAuthRequired = false;
            langConfig.otpDisabled.send(source);
            playerEntry.update();
            return 1;
        }

        langConfig.wrongOtp.send(source);
        return 0;
    }

    private static int enable2Fa(ServerCommandSource source, String password, String otpCode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerEntryV1 playerEntry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();

        if (!playerEntry.otpEnabled) {
            langConfig.otpNotEnabled.send(source);
            return 0;
        }

        if (playerEntry.twoFactorAuthRequired) {
            langConfig.twoFactorAuthAlreadyEnabled.send(source);
            return 0;
        }

        if (AuthHelper.checkPassword(playerEntry, password.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
            if (playerEntry.verifyOtp(otpCode)) {
                playerEntry.twoFactorAuthRequired = true;
                playerEntry.update();
                langConfig.twoFactorAuthEnabled.send(source);
                return 1;
            } else {
                langConfig.wrongOtp.send(source);
            }
        } else {
            langConfig.wrongPassword.send(source);
        }

        langConfig.twoFactorAuthEnableFailed.send(source);
        return 0;
    }

    private static int disable2Fa(ServerCommandSource source, String password, String otpCode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerEntryV1 playerEntry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();

        if (!playerEntry.otpEnabled) {
            langConfig.otpNotEnabled.send(source);
            return 0;
        }

        if (!playerEntry.twoFactorAuthRequired) {
            langConfig.twoFactorAuthAlreadyDisabled.send(source);
            return 0;
        }

        if (AuthHelper.checkPassword(playerEntry, password.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
            if (playerEntry.verifyOtp(otpCode)) {
                playerEntry.twoFactorAuthRequired = false;
                playerEntry.update();
                langConfig.twoFactorAuthDisabled.send(source);
                return 1;
            } else {
                langConfig.wrongOtp.send(source);
            }
        } else {
            langConfig.wrongPassword.send(source);
        }

        langConfig.twoFactorAuthDisableFailed.send(source);
        return 0;
    }
}
