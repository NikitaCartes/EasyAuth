//~ resource_location
package xyz.nikitacartes.easyauth.dialog;
//? if >= 1.21.6 {

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.commands.AccountCommand;
import xyz.nikitacartes.easyauth.commands.AuthCommand;
import xyz.nikitacartes.easyauth.commands.LoginCommand;
import xyz.nikitacartes.easyauth.commands.LogoutCommand;
import xyz.nikitacartes.easyauth.commands.RegisterCommand;
import xyz.nikitacartes.easyauth.integrations.FabricPermissions;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static xyz.nikitacartes.easyauth.EasyAuth.*;

/**
 * Receives dialog submissions ({@link net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket})
 * and routes them to the existing command logic. Runs on the server thread.
 */
public class DialogHandler {

    /** @return true if this packet was an EasyAuth dialog action and was handled. */
    public static boolean handle(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        if (!id.getNamespace().equals("easyauth")) {
            return false;
        }
        CompoundTag data = payload.orElse(null) instanceof CompoundTag tag ? tag : new CompoundTag();
        String path = id.getPath();
        if (path.startsWith("admin_form/")) {
            adminForm(player, path.substring("admin_form/".length()));
            return true;
        }
        if (path.startsWith("admin/")) {
            adminRun(player, path.substring("admin/".length()), data);
            return true;
        }
        switch (path) {
            case "login" -> {
                login(player, data);
                return true;
            }
            case "register" -> {
                register(player, data);
                return true;
            }
            // Stage 2 — account menu navigation (open a sub-window)
            case "account" -> open(player, AuthDialogs::openAccountMenu);
            case "change_password_form" -> open(player, AuthDialogs::openChangePassword);
            case "unregister_form" -> open(player, AuthDialogs::openUnregister);
            case "account_online_form" -> open(player, AuthDialogs::openAccountOnline);
            // Stage 2 — account actions (feedback goes to chat; the player is authenticated so it is visible)
            case "change_password" -> account(player, source ->
                    AccountCommand.changePassword(source, data.getStringOr("old_password", ""), data.getStringOr("new_password", "")));
            case "unregister" -> account(player, source ->
                    AccountCommand.unregister(source, data.getStringOr("password", "")));
            case "account_online" -> account(player, source ->
                    AccountCommand.markAsOnline(source, data.getStringOr("password", ""), true));
            case "logout" -> {
                account(player, LogoutCommand::logout);
                close(player);
                return true;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface CommandCall {
        void run(CommandSourceStack source) throws Exception;
    }

    /** Runs an authenticated-player action with the player's own command source (chat feedback). */
    private static void account(ServerPlayer player, CommandCall call) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated()) {
            return;
        }
        try {
            call.run(player.createCommandSourceStack());
        } catch (Exception ignored) {
        }
    }

    private static boolean open(ServerPlayer player, java.util.function.Consumer<ServerPlayer> opener) {
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            opener.accept(player);
        }
        return true;
    }

    private static void login(ServerPlayer player, CompoundTag data) {
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            close(player);
            return;
        }
        List<Component> feedback = new ArrayList<>();
        try {
            LoginCommand.login(capturing(player, feedback), data.getStringOr("password", ""));
        } catch (Exception ignored) {
        }
        finish(player, AuthDialogs.LOGIN, feedback);
    }

    private static void register(ServerPlayer player, CompoundTag data) {
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            close(player);
            return;
        }
        List<Component> feedback = new ArrayList<>();
        CommandSourceStack source = capturing(player, feedback);
        String password = data.getStringOr("password", "");
        String confirm = data.getStringOr("password_confirm", "");
        try {
            if (config.enableGlobalPassword && config.singleUseGlobalPassword) {
                RegisterCommand.register(source, data.getStringOr("global_password", ""), password, confirm);
            } else {
                RegisterCommand.register(source, password, confirm);
            }
        } catch (Exception ignored) {
        }
        finish(player, AuthDialogs.REGISTER, feedback);
    }

    /** Login/register flip the auth flag synchronously, so we can tell success from failure here. */
    private static void finish(ServerPlayer player, Identifier which, List<Component> feedback) {
        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            close(player);
            return;
        }
        if (!player.connection.isAcceptingMessages()) {
            return; // player was kicked (e.g. too many tries)
        }
        Component error = feedback.isEmpty() ? langConfig.password.incorrect.get() : feedback.get(feedback.size() - 1);
        if (which.equals(AuthDialogs.LOGIN)) {
            AuthDialogs.reopenLogin(player, error);
        } else {
            AuthDialogs.reopenRegister(player, error);
        }
    }

    private static boolean adminAllowed(ServerPlayer player, String key) {
        AuthDialogs.AdminAction action = AuthDialogs.adminAction(key);
        return action != null
                && FabricPermissions.require(action.node(), action.level()).test(player.createCommandSourceStack());
    }

    private static void adminForm(ServerPlayer player, String key) {
        if (adminAllowed(player, key)) {
            AuthDialogs.openAdminForm(player, key);
        }
    }

    private static void adminRun(ServerPlayer player, String key, CompoundTag data) {
        if (!adminAllowed(player, key)) {
            return;
        }
        CommandSourceStack source = player.createCommandSourceStack();
        String username = data.getStringOr("username", "");
        try {
            switch (key) {
                case "reload" -> AuthCommand.reloadConfig(source);
                case "list" -> AuthCommand.getRegisteredPlayers(source);
                case "online_players" -> AuthCommand.getOnlinePlayers(source);
                case "set_spawn" -> AuthCommand.setSpawnHere(source);
                case "player_info" -> AuthCommand.getPlayerInfo(source, username);
                case "get_uuid" -> AuthCommand.getUuid(source, username);
                case "mark_offline" -> AuthCommand.markAsOffline(source, username);
                case "mark_online" -> AuthCommand.markAsOnline(source, username);
                case "register" -> AuthCommand.registerUser(source, username, data.getStringOr("password", ""));
                case "update" -> AuthCommand.updatePassword(source, username, data.getStringOr("password", ""));
                case "remove" -> AuthCommand.removeAccount(source, username);
                case "set_global_password" -> AuthCommand.setGlobalPassword(source,
                        data.getStringOr("password", ""), "true".equals(data.getStringOr("single_use", "false")));
                case "set_uuid" -> AuthCommand.setUuid(source, username, data.getStringOr("uuid", ""));
                case "clear_uuid" -> AuthCommand.clearUuid(source, username);
                default -> { }
            }
        } catch (Exception ignored) {
        }
    }

    private static void close(ServerPlayer player) {
        if (player.connection.isAcceptingMessages()) {
            player.connection.send(ClientboundClearDialogPacket.INSTANCE);
        }
    }

    /** A command source backed by the player but capturing feedback instead of sending it to chat. */
    private static CommandSourceStack capturing(ServerPlayer player, List<Component> out) {
        return player.createCommandSourceStack().withSource(new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                out.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        });
    }
}
//?}
