package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static net.minecraft.server.command.CommandManager.literal;
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

        if (((PlayerAuth) player).easyAuth$isAuthenticated()) {
            // player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
            ((PlayerAuth) player).easyAuth$setAuthenticated(false);
            langConfig.successfulLogout.send(serverCommandSource);
        } else {
            langConfig.cannotLogout.send(serverCommandSource);
        }
        return 1;
    }
}
