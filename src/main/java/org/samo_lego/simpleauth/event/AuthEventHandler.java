package org.samo_lego.simpleauth.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
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

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }
    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if(!SimpleAuth.authenticatedUsers.contains(player)) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
}