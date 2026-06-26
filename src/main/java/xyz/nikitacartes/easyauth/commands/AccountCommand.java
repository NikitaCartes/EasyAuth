package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.dialog.AuthDialogs;
import xyz.nikitacartes.easyauth.integrations.EasyAuthPermissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.AuthHelper;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;
import xyz.nikitacartes.easyauth.utils.Totp;

import java.io.IOException;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.integrations.MojangApi.isValidUsername;

public class AccountCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("account")
                .requires(EasyAuthPermissions.require("easyauth.commands.account.root", true))
                .executes(ctx -> accountRoot(ctx.getSource()))
                .then(literal("unregister")
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.unregister", true))
                        .executes(ctx -> {
                            langConfig.password.enter.send(ctx.getSource());
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
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.changePassword", true))
                        .then(argument("old password", string())
                                .executes(ctx -> {
                                    langConfig.password.enterNew.send(ctx.getSource());
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
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.online", true))
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
                .then(literal("session")
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.session", true))
                        .executes(ctx -> showSessionTimeout(ctx.getSource()))
                        .then(argument("seconds", longArg(-1))
                                .executes(ctx -> setSessionTimeout(
                                        ctx.getSource(),
                                        getLong(ctx, "seconds")
                                ))
                        )
                )
                .then(literal("dialog")
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.dialog", true))
                        .then(argument("show", bool())
                                .executes(ctx -> setShowLoginDialog(
                                        ctx.getSource(),
                                        getBool(ctx, "show")
                                ))
                        )
                )
                .then(literal("otp")
                        .requires(EasyAuthPermissions.require("easyauth.commands.account.otp", true))
                        .executes(ctx -> otpStatus(ctx.getSource()))
                        .then(literal("enable")
                                .executes(ctx -> otpEnableStart(ctx.getSource()))
                                .then(argument("code", string())
                                        .executes(ctx -> otpEnableConfirm(
                                                ctx.getSource(),
                                                getString(ctx, "code")
                                        ))
                                )
                        )
                        .then(literal("disable")
                                .then(argument("code", string())
                                        .executes(ctx -> otpDisable(
                                                ctx.getSource(),
                                                getString(ctx, "code")
                                        ))
                                )
                        )
                )
        );
    }

    // Opens the account menu window, or prints the available actions when dialogs are off.
    private static int accountRoot(CommandSourceStack source) throws CommandSyntaxException {
        //? if >= 1.21.6 {
        if (dialogConfig.enabled && dialogConfig.account) {
            AuthDialogs.openAccountMenu(source.getPlayerOrException());
            return 1;
        }
        //?}
        langConfig.dialog.account.usage.send(source);
        return 1;
    }

    // Method called for checking the password and then removing user's account from db
    public static int unregister(CommandSourceStack source, String pass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.account.cannotUnregister.send(source);
            return 0;
        }

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.account.cannotUnregister.send(source);
            return 0;
        }

        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }

        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            String username = StoneCutterUtils.getUsername(player);
            if (AuthHelper.checkPassword(playerAuth, pass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                PlayerEntryV1 playerEntry = DB.getUserData(username);
                if (playerEntry == null) {
                    langConfig.account.cannotUnregister.send(source);
                    return;
                }

                if (!DB.deleteUserData(username)) {
                    langConfig.error.unknown.send(source);
                    return;
                }
                langConfig.account.deleted.send(source);
                playerAuth.easyAuth$setAuthenticated(false);
                playerAuth.easyAuth$setPlayerEntryV1(new PlayerEntryV1(username));
                player.connection.disconnect(langConfig.account.deleted.get());
                return;
            }
            langConfig.password.incorrect.send(source);
        });
        return 0;
    }

    // Method called for checking the password and then changing it
    public static int changePassword(CommandSourceStack source, String oldPass, String newPass) throws CommandSyntaxException {
        // Getting the player who send the command
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (config.enableGlobalPassword && !config.singleUseGlobalPassword) {
            langConfig.password.cannotChange.send(source);
            return 0;
        }
        if (newPass.length() < extendedConfig.minPasswordLength) {
            langConfig.password.tooShort.send(source, extendedConfig.minPasswordLength);
            return 0;
        } else if (newPass.length() > extendedConfig.maxPasswordLength && extendedConfig.maxPasswordLength != -1) {
            langConfig.password.tooLong.send(source, extendedConfig.maxPasswordLength);
            return 0;
        }
        // Different thread to avoid lag spikes
        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, oldPass.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
                // Changing password

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.password = AuthHelper.hashPassword(newPass.toCharArray());
                playerEntry.update();

                langConfig.password.changed.send(source);
            } else {
                langConfig.password.incorrect.send(source);
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
    private static int markAsOnline(CommandSourceStack source, String password) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;

        THREADPOOL.submit(() -> {
            if (AuthHelper.checkPassword(playerAuth, password.toCharArray()) == AuthHelper.PasswordOptions.CORRECT) {
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

                PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
                playerEntry.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
                playerEntry.update();

                langConfig.account.markedSelfOnline.send(source);
            } else {
                langConfig.password.incorrect.send(source);
            }
        });

        return 1;
    }

    public static int markAsOnline(CommandSourceStack source, String password, boolean confirm) throws CommandSyntaxException {
        if (!confirm) {
            langConfig.account.markSelfOnlineWarning.send(source);
            return 0;
        }
        return markAsOnline(source, password);
    }

    /** Shows the player's current session length setting. */
    private static int showSessionTimeout(CommandSourceStack source) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        langConfig.account.sessionCurrent.send(source, playerAuth.easyAuth$getPlayerEntryV1().sessionTimeout);
        return 1;
    }

    /**
     * Sets the player's own session length (auto-login window) in seconds.
     * 0 = follow the server default, -1 = always require a fresh login.
     * Positive values are clamped to the server default at login time, so a player can only shorten their session.
     */
    public static int setSessionTimeout(CommandSourceStack source, long seconds) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
        playerEntry.sessionTimeout = seconds;
        playerEntry.update();
        langConfig.account.sessionSet.send(source, seconds);
        return 1;
    }

    /** Toggles whether the login Dialog window is shown to this player on join (otherwise the chat prompt is used). */
    public static int setShowLoginDialog(CommandSourceStack source, boolean show) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
        playerEntry.showLoginDialog = show;
        playerEntry.update();
        (show ? langConfig.account.dialogEnabled : langConfig.account.dialogDisabled).send(source);
        return 1;
    }

    /** Applies both player settings from the settings Dialog window in one update. */
    public static int applySettings(CommandSourceStack source, String secondsStr, boolean showDialog) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        long seconds;
        try {
            seconds = Long.parseLong(secondsStr.trim());
        } catch (NumberFormatException e) {
            langConfig.account.sessionInvalid.send(source);
            return 0;
        }
        if (seconds < -1) {
            seconds = -1;
        }
        PlayerEntryV1 playerEntry = playerAuth.easyAuth$getPlayerEntryV1();
        playerEntry.sessionTimeout = seconds;
        playerEntry.showLoginDialog = showDialog;
        playerEntry.update();
        langConfig.account.settingsSaved.send(source);
        return 1;
    }

    /** Reports whether two-factor authentication is currently active for the player. */
    public static int otpStatus(CommandSourceStack source) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        (playerAuth.easyAuth$getPlayerEntryV1().hasOtp()
                ? langConfig.account.otpStatusEnabled
                : langConfig.account.otpStatusDisabled).send(source);
        return 1;
    }

    /** Begins 2FA enrollment: generates a secret and shows the setup link/secret (and QR window on 1.21.6+). */
    public static int otpEnableStart(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerAuth playerAuth = (PlayerAuth) player;
        if (!config.enableOtp) {
            langConfig.account.otpFeatureDisabled.send(source);
            return 0;
        }
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (entry.hasOtp()) {
            langConfig.account.otpAlreadyEnabled.send(source);
            return 0;
        }
        String secret = Totp.generateSecret();
        entry.otpSecret = secret;
        entry.otpEnabled = false;
        entry.update();
        String uri = Totp.uri("EasyAuth", StoneCutterUtils.getUsername(player), secret);
        langConfig.account.otpSetup.send(source);
        source.sendSystemMessage(copyable(uri, ChatFormatting.AQUA));
        source.sendSystemMessage(langConfig.account.otpSecretLabel.get().append(copyable(secret, ChatFormatting.YELLOW)));
        //? if >= 1.21.6 {
        if (dialogConfig.enabled && dialogConfig.account) {
            AuthDialogs.openOtpSetup(player, secret, uri);
        }
        //?}
        return 1;
    }

    /** Confirms enrollment: a valid code for the pending secret turns 2FA on. */
    public static int otpEnableConfirm(CommandSourceStack source, String code) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!config.enableOtp) {
            langConfig.account.otpFeatureDisabled.send(source);
            return 0;
        }
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (entry.otpEnabled) {
            langConfig.account.otpAlreadyEnabled.send(source);
            return 0;
        }
        if (entry.otpSecret == null) {
            langConfig.account.otpNoPending.send(source);
            return 0;
        }
        if (!Totp.verify(entry.otpSecret, code, 1)) {
            langConfig.account.otpInvalidCode.send(source);
            return 0;
        }
        entry.otpEnabled = true;
        entry.update();
        langConfig.account.otpEnabled.send(source);
        return 1;
    }

    /** Disables 2FA after verifying a current code, clearing the stored secret. */
    public static int otpDisable(CommandSourceStack source, String code) throws CommandSyntaxException {
        PlayerAuth playerAuth = (PlayerAuth) source.getPlayerOrException();
        if (!playerAuth.easyAuth$isAuthenticated()) {
            langConfig.session.loginRequired.send(source);
            return 0;
        }
        PlayerEntryV1 entry = playerAuth.easyAuth$getPlayerEntryV1();
        if (!entry.hasOtp()) {
            langConfig.account.otpNotEnabled.send(source);
            return 0;
        }
        if (!entry.verifyOtp(code)) {
            langConfig.account.otpInvalidCode.send(source);
            return 0;
        }
        entry.otpEnabled = false;
        entry.otpSecret = null;
        entry.update();
        langConfig.account.otpDisabled.send(source);
        return 1;
    }

    /** A literal component that copies {@code text} to the clipboard when clicked. */
    private static MutableComponent copyable(String text, ChatFormatting color) {
        return Component.literal(text).setStyle(Style.EMPTY.applyFormat(color).withClickEvent(copyClick(text)));
    }

    private static ClickEvent copyClick(String text) {
        //? if >= 1.21.5 {
        return new ClickEvent.CopyToClipboard(text);
        //?} else {
        /*return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
        *///?}
    }
}
