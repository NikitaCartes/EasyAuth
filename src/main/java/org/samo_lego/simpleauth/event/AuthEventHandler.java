package org.samo_lego.simpleauth.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.samo_lego.simpleauth.SimpleAuth;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {
    private static TranslatableText notAuthenticated = new TranslatableText(SimpleAuth.config.lang.notAuthenticated);
    private static TranslatableText timeExpired = new TranslatableText(SimpleAuth.config.lang.timeExpired);
    private static int delay = SimpleAuth.config.main.delay;

    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        SimpleAuth.deauthenticatedUsers.add(player);
        // Player not authenticated
        // If clause actually not needed, since we add player to deauthenticated hashset above
        if (!SimpleAuth.isAuthenticated(player)) {
            player.sendMessage(notAuthenticated);
            // Setting the player to be invisible to mobs and also invulnerable
            player.setInvulnerable(SimpleAuth.config.main.playerInvulnerable);
            player.setInvisible(SimpleAuth.config.main.playerInvisible);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(!SimpleAuth.isAuthenticated(player)) // Kicking player if not authenticated
                        player.networkHandler.disconnect(timeExpired);
                }
            }, delay * 1000);
        }
    }

    // Player chatting
    public static ActionResult onPlayerChat(PlayerEntity player, ChatMessageC2SPacket chatMessageC2SPacket) {
        // Getting the message to then be able to check it
        String msg = chatMessageC2SPacket.getChatMessage();
        if(
            !SimpleAuth.isAuthenticated((ServerPlayerEntity) player) &&
            !msg.startsWith("/login") &&
            !msg.startsWith("/register") &&
            (!SimpleAuth.config.main.allowChat || msg.startsWith("/"))
        ) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    // Player movement
    public static ActionResult onPlayerMove(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowMovement) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowBlockUse) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowBlockPunch) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowItemUse) {
            player.sendMessage(notAuthenticated);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }
    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowItemDrop) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowEntityPunch) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowEntityInteract) {
            player.sendMessage(notAuthenticated);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}