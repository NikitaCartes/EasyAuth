package org.samo_lego.simpleauth.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import org.samo_lego.simpleauth.mixin.BlockUpdateS2CPacketAccess;
import org.samo_lego.simpleauth.storage.PlayerCache;

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.samo_lego.simpleauth.SimpleAuth.*;
import static org.samo_lego.simpleauth.utils.UuidConverter.convertUuid;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {
    private static Text notAuthenticated() {
        if(config.main.enableGlobalPassword) {
            return new LiteralText(config.lang.loginRequired);
        }
        return new LiteralText(config.lang.notAuthenticated);
    }

    private static Text successfulPortalRescue = new LiteralText(config.lang.successfulPortalRescue);

    // Player pre-join
    // Returns text as a reason for disconnect or null to pass
    public static LiteralText checkCanPlayerJoinServer(SocketAddress socketAddress, GameProfile profile, PlayerManager manager) {
        // Getting the player
        String incomingPlayerUsername = profile.getName();
        PlayerEntity onlinePlayer = manager.getPlayer(incomingPlayerUsername);

        // Checking if player username is valid
        String regex = config.main.usernameRegex;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(incomingPlayerUsername);

        if((onlinePlayer != null && !isPlayerFake(onlinePlayer)) && config.experimental.disableAnotherLocationKick) {
            // Player needs to be kicked, since there's already a player with that name
            // playing on the server
            return new LiteralText(
                    String.format(
                            config.lang.playerAlreadyOnline, onlinePlayer.getName().asString()
                    )
            );
        }
        else if(!matcher.matches()) {
            return new LiteralText(
                    String.format(
                            config.lang.disallowedUsername, regex
                    )
            );
        }
        return null;
    }


    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        // If player is fake auth is not needed
        if (isPlayerFake(player))
            return;
        // Checking if session is still valid
        String uuid = convertUuid(player);
        PlayerCache playerCache = deauthenticatedUsers.getOrDefault(uuid, null);

        if (playerCache != null) {
            if (
                playerCache.wasAuthenticated &&
                playerCache.validUntil >= System.currentTimeMillis() &&
                playerCache.lastIp.equals(player.getIp())
            ) {
                deauthenticatedUsers.remove(uuid); // Makes player authenticated
                return;
            }
            // Invalidating session
            playerCache.wasAuthenticated = false;
            if(playerCache.isRegistered)
                player.sendMessage(new LiteralText(config.lang.loginRequired), false);
            else
                player.sendMessage(new LiteralText(config.lang.notRegistered), false);
        }
        else
            deauthenticatePlayer(player);

        if(config.main.spawnOnJoin)
            teleportPlayer(player, true);


        // Tries to rescue player from nether portal
        if(config.main.tryPortalRescue && player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL)) {
            PlayerCache newPlayerCache = deauthenticatedUsers.get(uuid);
            BlockPos pos = player.getBlockPos();

            // Faking portal blocks to be air
            BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket();
            ((BlockUpdateS2CPacketAccess) feetPacket).setState(new BlockState(Blocks.AIR, null));
            ((BlockUpdateS2CPacketAccess) feetPacket).setBlockPos(pos);
            player.networkHandler.sendPacket(feetPacket);

            BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket();
            ((BlockUpdateS2CPacketAccess) headPacket).setState(new BlockState(Blocks.AIR, null));
            ((BlockUpdateS2CPacketAccess) headPacket).setBlockPos(pos.up());
            player.networkHandler.sendPacket(headPacket);

            // Teleporting player to the middle of the block
            player.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            newPlayerCache.wasInPortal = true;
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        if(isPlayerFake(player) || !isAuthenticated(player) || config.main.sessionTimeoutTime == -1)
            return;

        // Starting session
        // Putting player to deauthenticated player map
        deauthenticatePlayer(player);
        
        // Setting that player was actually authenticated before leaving
        PlayerCache playerCache = deauthenticatedUsers.get(convertUuid(player));
        if(playerCache == null)
            return;

        playerCache.wasAuthenticated = true;
        // Setting the session expire time
        playerCache.validUntil = System.currentTimeMillis() + config.main.sessionTimeoutTime * 1000;
    }

    // Player chatting
    public static ActionResult onPlayerChat(PlayerEntity player, ChatMessageC2SPacket chatMessageC2SPacket) {
        // Getting the message to then be able to check it
        String msg = chatMessageC2SPacket.getChatMessage();
        if(
            !isAuthenticated((ServerPlayerEntity) player) &&
            !msg.startsWith("/login") &&
            !msg.startsWith("/register") &&
            (!config.experimental.allowChat || msg.startsWith("/"))
        ) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowMovement) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowBlockUse) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowBlockPunch) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemUse) {
            player.sendMessage(notAuthenticated(), false);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }
    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemDrop) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemMoving) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowEntityPunch) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.main.allowEntityInteract) {
            player.sendMessage(notAuthenticated(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}