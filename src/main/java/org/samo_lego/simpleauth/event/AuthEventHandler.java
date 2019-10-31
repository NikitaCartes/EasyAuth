package org.samo_lego.simpleauth.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.SimpleAuth;

public class AuthEventHandler {
    public static void onPlayerJoin(ServerPlayerEntity player) {
        // Player not authenticated
        if (!SimpleAuth.isAuthenticated(player))
            System.out.println("Not authenticated!");
    }

    // Breaking block
    public static boolean onBlockBroken(PlayerEntity player) {
        // Player not authenticated
        if (!SimpleAuth.isAuthenticated((ServerPlayerEntity) player))
        {
            System.out.println("Not authenticated!");
            return true;
        }
        return false;
    }

    // Interacting with block
    public static ActionResult onInteractBlock(ServerPlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player))
            return ActionResult.FAIL;
        return ActionResult.PASS;
    }
    // Interacting with item
    public static ActionResult onInteractItem(ServerPlayerEntity player) {
        System.out.println("Called");
        if(!SimpleAuth.authenticatedUsers.contains(player))
            return ActionResult.FAIL;
        return ActionResult.PASS;
    }
}
