package org.samo_lego.simpleauth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.samo_lego.simpleauth.storage.PlayerCache;

import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.simpleauth.SimpleAuth.*;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

public class LogoutCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Registering the "/logout" command
        dispatcher.register(literal("logout")
                .executes(ctx -> logout(ctx.getSource())) // Tries to de-authenticate the user
        );
    }

    private static int logout(ServerCommandSource serverCommandSource) throws CommandSyntaxException {
        ServerPlayerEntity player = serverCommandSource.getPlayer();
        PlayerCache playerCache = playerCacheMap.get(convertUuid(player));
        playerCache.lastLocation.lastDim = String.valueOf(player.getEntityWorld().getRegistryKey().getValue());
        playerCache.lastLocation.lastX = player.getX();
        playerCache.lastLocation.lastY = player.getY();
        playerCache.lastLocation.lastZ = player.getZ();
        playerCache.lastLocation.lastYaw = player.yaw;
        playerCache.lastLocation.lastPitch = player.pitch;

        deauthenticatePlayer(player);
        player.sendMessage(new LiteralText(config.lang.successfulLogout), false);
        return 1;
    }
}
