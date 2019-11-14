package org.samo_lego.simpleauth.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import org.samo_lego.simpleauth.SimpleAuth;

/**
 * This class will take care of actions players try to do,
 * and cancels them if they aren't authenticated
 */
public class AuthEventHandler {
    private static TranslatableText notAuthenticated = new TranslatableText("command.simpleauth.notAuthenticated");

    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        // Player not authenticated
        if (!SimpleAuth.isAuthenticated(player))
            player.sendMessage(notAuthenticated);
    }

    // Player leaving the server
    public static void onPlayerLeave(ServerPlayerEntity player) {
        SimpleAuth.authenticatedUsers.remove(player);
    }

    // Breaking block
    public static boolean onBlockBroken(PlayerEntity player) {
        if (!SimpleAuth.isAuthenticated((ServerPlayerEntity) player)) {
            player.sendMessage(notAuthenticated);
            return true;
        }
        return false;
    }

    // Interacting with block
    public static ActionResult onInteractBlock(ServerPlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity playerEntity) {
        if(!SimpleAuth.authenticatedUsers.contains(playerEntity)) {
            playerEntity.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Interacting with item
    public static ActionResult onInteractItem(ServerPlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Dropping an item
    public static boolean onDropItem(ServerPlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return true;
        }
        return false;
    }
}
