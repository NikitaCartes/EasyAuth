//~ resource_location
package xyz.nikitacartes.easyauth.dialog;
//? if >= 1.21.6 {

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.EasyAuthPermissions;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
//?}

/**
 * Builds and opens the EasyAuth Dialog windows (1.21.6+).
 * Layouts are built in code, but a datapack may override any of them by id
 * (see {@code allowDatapackOverride}); the mod only relies on the submit
 * action id and the input keys, not on the layout.
 */
public class AuthDialogs {
    //? if >= 1.21.6 {
    // login / registration (submit ids)
    public static final Identifier LOGIN = id("login");
    public static final Identifier REGISTER = id("register");

    // account menu (open ids = also the datapack-override ids)
    public static final Identifier ACCOUNT = id("account");
    public static final Identifier CHANGE_PASSWORD_FORM = id("change_password_form");
    public static final Identifier UNREGISTER_FORM = id("unregister_form");
    public static final Identifier ACCOUNT_ONLINE_FORM = id("account_online_form");
    public static final Identifier SETTINGS_FORM = id("settings_form");
    public static final Identifier CHANGE_PASSWORD = id("change_password");
    public static final Identifier UNREGISTER = id("unregister");
    public static final Identifier ACCOUNT_ONLINE = id("account_online");
    public static final Identifier SETTINGS = id("settings");
    public static final Identifier LOGOUT = id("logout");
    public static final Identifier OTP_SETUP_FORM = id("otp_setup_form");
    public static final Identifier OTP_ENABLE = id("otp_enable");
    public static final Identifier OTP_DISABLE_FORM = id("otp_disable_form");
    public static final Identifier OTP_DISABLE = id("otp_disable");

    private static final int WIDTH = 300;
    private static final int BUTTON_WIDTH = 200;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("easyauth", path);
    }

    /**
     * Opens the login or registration window depending on account state.
     *
     * @return true if a window was opened, false if dialogs are disabled for this case
     *         (caller should fall back to the chat prompt).
     */
    public static boolean openAuthPrompt(ServerPlayer player) {
        boolean registered = !((PlayerAuth) player).easyAuth$getPlayerEntryV1().password.isEmpty();
        boolean wantLogin = registered || (config.enableGlobalPassword && !config.singleUseGlobalPassword);
        if (wantLogin) {
            if (!dialogConfig.login) {
                return false;
            }
            open(player, LOGIN, buildLogin(null, requireOtp(player)));
            return true;
        }
        if (!dialogConfig.register) {
            return false;
        }
        open(player, REGISTER, buildRegister(null));
        return true;
    }

    public static void reopenLogin(ServerPlayer player, Component error) {
        open(player, LOGIN, buildLogin(error, requireOtp(player)));
    }

    public static void reopenRegister(ServerPlayer player, Component error) {
        open(player, REGISTER, buildRegister(error));
    }

    private static boolean requireOtp(ServerPlayer player) {
        return ((PlayerAuth) player).easyAuth$getPlayerEntryV1().hasOtp();
    }

    static MultiActionDialog buildLogin(Component error, boolean requireOtp) {
        List<DialogBody> body = new ArrayList<>();
        body.add(new PlainMessage(langConfig.dialog.login.prompt.get(), WIDTH));
        if (error != null) {
            body.add(new PlainMessage(error, WIDTH));
        }
        List<Input> inputs = new ArrayList<>();
        inputs.add(new Input("password", passwordField(langConfig.dialog.login.password.get())));
        if (requireOtp) {
            inputs.add(new Input("otp", new TextInput(WIDTH, langConfig.dialog.login.otp.get(), true, "", 6, Optional.empty())));
        }
        return new MultiActionDialog(
                common(langConfig.dialog.login.title.get(), body, inputs, dialogConfig.canCloseWithEscape, DialogAction.NONE),
                List.of(submit(langConfig.dialog.login.submit.get(), LOGIN)), Optional.empty(), 1);
    }

    static MultiActionDialog buildRegister(Component error) {
        List<DialogBody> body = new ArrayList<>();
        body.add(new PlainMessage(langConfig.dialog.register.prompt.get(), WIDTH));
        if (error != null) {
            body.add(new PlainMessage(error, WIDTH));
        }
        List<Input> inputs = new ArrayList<>();
        if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
            inputs.add(new Input("global_password", passwordField(langConfig.dialog.register.globalPassword.get())));
        }
        inputs.add(new Input("password", passwordField(langConfig.dialog.register.password.get())));
        inputs.add(new Input("password_confirm", passwordField(langConfig.dialog.register.passwordConfirm.get())));
        return new MultiActionDialog(
                common(langConfig.dialog.register.title.get(), body, inputs, dialogConfig.canCloseWithEscape, DialogAction.NONE),
                List.of(submit(langConfig.dialog.register.submit.get(), REGISTER)), Optional.empty(), 1);
    }

    public static void openAccountMenu(ServerPlayer player) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(submit(langConfig.dialog.changePassword.title.get(), langConfig.dialog.account.changePasswordTooltip.get(), CHANGE_PASSWORD_FORM));
        buttons.add(submit(langConfig.dialog.unregister.title.get(), langConfig.dialog.account.unregisterTooltip.get(), UNREGISTER_FORM));
        buttons.add(submit(langConfig.dialog.online.title.get(), langConfig.dialog.account.onlineTooltip.get(), ACCOUNT_ONLINE_FORM));
        buttons.add(submit(langConfig.dialog.settings.title.get(), langConfig.dialog.account.settingsTooltip.get(), SETTINGS_FORM));
        if (((PlayerAuth) player).easyAuth$getPlayerEntryV1().hasOtp()) {
            buttons.add(submit(langConfig.dialog.account.otpDisableButton.get(), langConfig.dialog.account.otpDisableTooltip.get(), OTP_DISABLE_FORM));
        } else if (config.enableOtp) {
            buttons.add(submit(langConfig.dialog.account.otpEnableButton.get(), langConfig.dialog.account.otpEnableTooltip.get(), OTP_SETUP_FORM));
        }
        buttons.add(submit(langConfig.dialog.account.logoutButton.get(), langConfig.dialog.account.logoutTooltip.get(), LOGOUT));
        MultiActionDialog menu = new MultiActionDialog(
                common(langConfig.dialog.account.title.get(), List.of(), List.of(), true, DialogAction.NONE),
                buttons, Optional.empty(), 1);
        open(player, ACCOUNT, menu);
    }

    /** Setup window: shows the QR code + secret and asks for a confirmation code. */
    public static void openOtpSetup(ServerPlayer player, String secret, String uri) {
        open(player, OTP_SETUP_FORM, buildOtpSetup(player, secret, uri, null));
    }

    public static void reopenOtpSetup(ServerPlayer player, String secret, String uri, Component error) {
        open(player, OTP_SETUP_FORM, buildOtpSetup(player, secret, uri, error));
    }

    private static ConfirmationDialog buildOtpSetup(ServerPlayer player, String secret, String uri, Component error) {
        List<DialogBody> body = new ArrayList<>();
        body.add(new PlainMessage(langConfig.dialog.otp.setupPrompt.get(), WIDTH));
        body.add(new PlainMessage(langConfig.dialog.otp.link.get(copyable(uri)), WIDTH));
        body.add(new PlainMessage(langConfig.dialog.otp.secret.get(copyable(secret)), WIDTH));
        if (error != null) {
            body.add(new PlainMessage(error, WIDTH));
        }
        List<Input> inputs = List.of(
                new Input("otp_code", new TextInput(WIDTH, langConfig.dialog.otp.codeLabel.get(), true, "", 6, Optional.empty())));
        return new ConfirmationDialog(
                common(langConfig.dialog.otp.setupTitle.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.otp.setupSubmit.get(), OTP_ENABLE),
                cancelButton());
    }

    public static void openOtpDisable(ServerPlayer player) {
        open(player, OTP_DISABLE_FORM, buildOtpDisable(null));
    }

    public static void reopenOtpDisable(ServerPlayer player, Component error) {
        open(player, OTP_DISABLE_FORM, buildOtpDisable(error));
    }

    private static ConfirmationDialog buildOtpDisable(Component error) {
        List<DialogBody> body = new ArrayList<>();
        body.add(new PlainMessage(langConfig.dialog.otp.disablePrompt.get(), WIDTH));
        if (error != null) {
            body.add(new PlainMessage(error, WIDTH));
        }
        List<Input> inputs = List.of(
                new Input("otp_code", new TextInput(WIDTH, langConfig.dialog.otp.codeLabel.get(), true, "", 6, Optional.empty())));
        return new ConfirmationDialog(
                common(langConfig.dialog.otp.disableTitle.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.otp.disableSubmit.get(), OTP_DISABLE),
                cancelButton());
    }

    public static void openChangePassword(ServerPlayer player) {
        List<DialogBody> body = List.of();
        List<Input> inputs = List.of(
                new Input("old_password", passwordField(langConfig.dialog.changePassword.oldPassword.get())),
                new Input("new_password", passwordField(langConfig.dialog.changePassword.newPassword.get())));
        ConfirmationDialog dialog = new ConfirmationDialog(
                common(langConfig.dialog.changePassword.title.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.changePassword.submit.get(), CHANGE_PASSWORD),
                cancelButton());
        open(player, CHANGE_PASSWORD_FORM, dialog);
    }

    public static void openUnregister(ServerPlayer player) {
        List<DialogBody> body = List.of(
                new PlainMessage(langConfig.dialog.unregister.warning.get(), WIDTH));
        List<Input> inputs = List.of(new Input("password", passwordField(langConfig.dialog.unregister.password.get())));
        ConfirmationDialog dialog = new ConfirmationDialog(
                common(langConfig.dialog.unregister.title.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.unregister.confirm.get(), UNREGISTER),
                cancelButton());
        open(player, UNREGISTER_FORM, dialog);
    }

    public static void openAccountOnline(ServerPlayer player) {
        List<DialogBody> body = List.of(
                new PlainMessage(langConfig.dialog.online.warning.get(), WIDTH));
        List<Input> inputs = List.of(new Input("password", passwordField(langConfig.dialog.online.password.get())));
        ConfirmationDialog dialog = new ConfirmationDialog(
                common(langConfig.dialog.online.title.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.online.confirm.get(), ACCOUNT_ONLINE),
                cancelButton());
        open(player, ACCOUNT_ONLINE_FORM, dialog);
    }

    public static void openSettings(ServerPlayer player) {
        PlayerEntryV1 entry = ((PlayerAuth) player).easyAuth$getPlayerEntryV1();
        List<DialogBody> body = List.of(new PlainMessage(langConfig.dialog.settings.prompt.get(), WIDTH));
        List<Input> inputs = List.of(
                new Input("session_timeout", new TextInput(WIDTH, langConfig.dialog.settings.sessionLabel.get(),
                        true, String.valueOf(entry.sessionTimeout), 20, Optional.empty())),
                new Input("show_login_dialog", new BooleanInput(langConfig.dialog.settings.dialogLabel.get(), entry.showLoginDialog, "true", "false")));
        ConfirmationDialog dialog = new ConfirmationDialog(
                common(langConfig.dialog.settings.title.get(), body, inputs, true, DialogAction.CLOSE),
                submit(langConfig.dialog.settings.submit.get(), SETTINGS),
                cancelButton());
        open(player, SETTINGS_FORM, dialog);
    }

    public static final Identifier ADMIN = id("admin");

    /** One input field of an admin form. {@code bool} fields render as a checkbox. */
    public record FormField(String key, Supplier<Component> label, boolean bool) {}

    /**
     * An entry in the admin panel: its button label, the form fields it needs,
     * whether it is destructive (confirmation), and the permission required to run it.
     */
    public record AdminAction(String key, Supplier<Component> label, Supplier<Component> tooltip, List<FormField> fields,
                              boolean confirm, String node, int level) {}

    private static FormField username() {
        return new FormField("username", () -> langConfig.dialog.field.username.get(), false);
    }

    private static FormField password() {
        return new FormField("password", () -> langConfig.dialog.field.password.get(), false);
    }

    public static final List<AdminAction> ADMIN_ACTIONS = List.of(
            new AdminAction("reload", () -> langConfig.dialog.admin.reload.get(), () -> langConfig.dialog.admin.reloadTooltip.get(),
                    List.of(), false, "easyauth.commands.auth.reload", 3),
            new AdminAction("list", () -> langConfig.dialog.admin.list.get(), () -> langConfig.dialog.admin.listTooltip.get(),
                    List.of(), false, "easyauth.commands.auth.list", 3),
            new AdminAction("online_players", () -> langConfig.dialog.admin.onlinePlayers.get(), () -> langConfig.dialog.admin.onlinePlayersTooltip.get(),
                    List.of(), false, "easyauth.commands.auth.getOnlinePlayers", 3),
            new AdminAction("set_spawn", () -> langConfig.dialog.admin.setSpawn.get(), () -> langConfig.dialog.admin.setSpawnTooltip.get(),
                    List.of(), false, "easyauth.commands.auth.setSpawn", 3),
            new AdminAction("player_info", () -> langConfig.dialog.admin.playerInfo.get(), () -> langConfig.dialog.admin.playerInfoTooltip.get(),
                    List.of(username()), false, "easyauth.commands.auth.getPlayerInfo", 3),
            new AdminAction("get_uuid", () -> langConfig.dialog.admin.getUuid.get(), () -> langConfig.dialog.admin.getUuidTooltip.get(),
                    List.of(username()), false, "easyauth.commands.auth.getUuid", 3),
            new AdminAction("mark_offline", () -> langConfig.dialog.admin.markOffline.get(), () -> langConfig.dialog.admin.markOfflineTooltip.get(),
                    List.of(username()), false, "easyauth.commands.auth.markAsOffline", 3),
            new AdminAction("mark_online", () -> langConfig.dialog.admin.markOnline.get(), () -> langConfig.dialog.admin.markOnlineTooltip.get(),
                    List.of(username()), false, "easyauth.commands.auth.markAsOnline", 3),
            new AdminAction("register", () -> langConfig.dialog.admin.register.get(), () -> langConfig.dialog.admin.registerTooltip.get(),
                    List.of(username(), password()), false, "easyauth.commands.auth.register", 3),
            new AdminAction("update", () -> langConfig.dialog.admin.update.get(), () -> langConfig.dialog.admin.updateTooltip.get(),
                    List.of(username(), password()), false, "easyauth.commands.auth.update", 3),
            new AdminAction("remove", () -> langConfig.dialog.admin.remove.get(), () -> langConfig.dialog.admin.removeTooltip.get(),
                    List.of(username()), true, "easyauth.commands.auth.remove", 3),
            new AdminAction("set_global_password", () -> langConfig.dialog.admin.setGlobalPassword.get(), () -> langConfig.dialog.admin.setGlobalPasswordTooltip.get(),
                    List.of(password(), new FormField("single_use", () -> langConfig.dialog.field.singleUse.get(), true)), false, "easyauth.commands.auth.setGlobalPassword", 4),
            new AdminAction("set_uuid", () -> langConfig.dialog.admin.setUuid.get(), () -> langConfig.dialog.admin.setUuidTooltip.get(),
                    List.of(username(), new FormField("uuid", () -> langConfig.dialog.field.uuid.get(), false)), true, "easyauth.commands.auth.setUuid", 4),
            new AdminAction("clear_uuid", () -> langConfig.dialog.admin.clearUuid.get(), () -> langConfig.dialog.admin.clearUuidTooltip.get(),
                    List.of(username()), true, "easyauth.commands.auth.clearUuid", 4));

    public static AdminAction adminAction(String key) {
        for (AdminAction action : ADMIN_ACTIONS) {
            if (action.key().equals(key)) {
                return action;
            }
        }
        return null;
    }

    public static void openAdminMenu(ServerPlayer player) {
        CommandSourceStack source = player.createCommandSourceStack();
        List<ActionButton> buttons = new ArrayList<>();
        for (AdminAction action : ADMIN_ACTIONS) {
            if (!EasyAuthPermissions.require(action.node(), action.level()).test(source)) {
                continue;
            }
            Identifier target = action.fields().isEmpty() ? id("admin/" + action.key()) : id("admin_form/" + action.key());
            buttons.add(submit(action.label().get(), action.tooltip().get(), target));
        }
        MultiActionDialog menu = new MultiActionDialog(
                common(langConfig.dialog.admin.title.get(), List.of(), List.of(), true, DialogAction.NONE),
                buttons, Optional.empty(), 2);
        open(player, ADMIN, menu);
    }

    public static void openAdminForm(ServerPlayer player, String key) {
        AdminAction action = adminAction(key);
        if (action == null) {
            return;
        }
        List<Input> inputs = new ArrayList<>();
        for (FormField field : action.fields()) {
            InputControl control = field.bool()
                    ? new BooleanInput(field.label().get(), false, "true", "false")
                    : new TextInput(WIDTH, field.label().get(), true, "", 256, Optional.empty());
            inputs.add(new Input(field.key(), control));
        }
        Identifier submitId = id("admin/" + key);
        Identifier dialogId = id("admin_form/" + key);
        ConfirmationDialog dialog;
        if (action.confirm()) {
            dialog = new ConfirmationDialog(
                    common(action.label().get(), List.of(new PlainMessage(langConfig.dialog.admin.confirm.get(), WIDTH)), inputs, true, DialogAction.CLOSE),
                    submit(action.label().get(), submitId), cancelButton());
        } else {
            dialog = new ConfirmationDialog(
                    common(action.label().get(), List.of(), inputs, true, DialogAction.CLOSE),
                    submit(action.label().get(), submitId), cancelButton());
        }
        open(player, dialogId, dialog);
    }

    /** A clickable component that copies {@code text} to the clipboard (for the setup link/secret). */
    private static MutableComponent copyable(String text) {
        return Component.literal(text).setStyle(Style.EMPTY.applyFormat(ChatFormatting.AQUA).withClickEvent(
                //? if >= 1.21.5 {
                new ClickEvent.CopyToClipboard(text)
                //?} else {
                /*new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)
                *///?}
        ));
    }

    private static TextInput passwordField(Component label) {
        long max = extendedConfig.maxPasswordLength;
        int maxLength = max > 0 ? (int) max : 256;
        return new TextInput(WIDTH, label, true, "", maxLength, Optional.empty());
    }

    private static CommonDialogData common(Component title, List<DialogBody> body, List<Input> inputs, boolean canCloseWithEscape, DialogAction afterAction) {
        return new CommonDialogData(title, Optional.empty(), canCloseWithEscape, false, afterAction, body, inputs);
    }

    private static ActionButton submit(Component label, Identifier action) {
        return new ActionButton(new CommonButtonData(label, BUTTON_WIDTH), Optional.of(new CustomAll(action, Optional.empty())));
    }

    /** A submit button that shows {@code tooltip} when hovered. */
    private static ActionButton submit(Component label, Component tooltip, Identifier action) {
        return new ActionButton(new CommonButtonData(label, Optional.of(tooltip), BUTTON_WIDTH), Optional.of(new CustomAll(action, Optional.empty())));
    }

    private static ActionButton cancelButton() {
        return new ActionButton(new CommonButtonData(langConfig.dialog.cancel.get(), BUTTON_WIDTH), Optional.empty());
    }

    /** Opens {@code fallback}, unless a datapack provides a dialog registered under {@code id}. */
    public static void open(ServerPlayer player, Identifier id, Dialog fallback) {
        player.openDialog(resolve(player, id, fallback));
    }

    private static Holder<Dialog> resolve(ServerPlayer player, Identifier id, Dialog fallback) {
        if (dialogConfig.allowDatapackOverride) {
            Holder<Dialog> custom = player.registryAccess()
                    .lookup(Registries.DIALOG)
                    .flatMap(registry -> registry.get(id))
                    .orElse(null);
            if (custom != null) {
                return custom;
            }
        }
        return Holder.direct(fallback);
    }
    //?}
}
