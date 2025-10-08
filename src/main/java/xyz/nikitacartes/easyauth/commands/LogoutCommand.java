package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.integrations.Permissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.getUnixZero;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

public class LogoutCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/logout" command
        dispatcher.register(literal("logout")
                .requires(Permissions.require("easyauth.commands.logout", true))
                .executes(ctx -> logout(ctx.getSource())) // Tries to de-authenticate the user
        );
    }

    private static int logout(ServerCommandSource serverCommandSource) throws CommandSyntaxException {
        ServerPlayerEntity player = serverCommandSource.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (playerAuth.easyAuth$isAuthenticated() && !playerAuth.easyAuth$canSkipAuth()) {
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
            playerAuth.easyAuth$setAuthenticated(false);

            PlayerEntryV1 playerData = playerAuth.easyAuth$getPlayerEntryV1();
            playerData.lastAuthenticatedDate = getUnixZero();
            playerData.update();

            langConfig.successfulLogout.send(serverCommandSource);
        } else {
            langConfig.cannotLogout.send(serverCommandSource);
        }
        return 1;
    }
}
