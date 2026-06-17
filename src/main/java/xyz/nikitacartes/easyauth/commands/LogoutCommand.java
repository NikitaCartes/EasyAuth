package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xyz.nikitacartes.easyauth.integrations.FabricPermissions;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static net.minecraft.commands.Commands.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.getUnixZero;
import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

public class LogoutCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Registering the "/logout" command
        dispatcher.register(literal("logout")
                .requires(FabricPermissions.require("easyauth.commands.logout", true))
                .executes(ctx -> logout(ctx.getSource())) // Tries to de-authenticate the user
        );
    }

    private static int logout(CommandSourceStack serverCommandSource) throws CommandSyntaxException {
        ServerPlayer player = serverCommandSource.getPlayerOrException();
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
