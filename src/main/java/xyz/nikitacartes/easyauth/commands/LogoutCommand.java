package xyz.nikitacartes.easyauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import static net.minecraft.server.command.CommandManager.literal;
import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.mojangAccountNamesCache;

public class LogoutCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/logout" command
        dispatcher.register(literal("logout")
                .executes(ctx -> logout(ctx.getSource())) // Tries to de-authenticate the user
        );
    }

    private static int logout(ServerCommandSource serverCommandSource) throws CommandSyntaxException {
        ServerPlayerEntity player = serverCommandSource.getPlayer();

        if(!mojangAccountNamesCache.contains(player.getGameProfile().getName().toLowerCase())) {
            ((PlayerAuth) player).setAuthenticated(false);
            player.sendMessage(new LiteralText(config.lang.successfulLogout), false);
        }
        else
            player.sendMessage(new LiteralText(config.lang.cannotLogout), false);
        return 1;
    }
}
