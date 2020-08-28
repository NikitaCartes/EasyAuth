package org.samo_lego.simpleauth.event;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
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

        if((onlinePlayer != null && !isPlayerFake(onlinePlayer)) && config.experimental.preventAnotherLocationKick) {
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
        PlayerCache playerCache = playerCacheMap.getOrDefault(uuid, null);

        if (playerCache != null) {
            if (
                playerCache.isAuthenticated &&
                playerCache.validUntil >= System.currentTimeMillis() &&
                player.getIp().equals(playerCache.lastIp)
            ) {
                authenticatePlayer(player, null); // Makes player authenticated
                return;
            }
            player.setInvulnerable(config.experimental.playerInvulnerable);
            player.setInvisible(config.experimental.playerInvisible);

            // Invalidating session
            playerCache.isAuthenticated = false;
            player.sendMessage(notAuthenticated(player), false);
        }
        else {
            deauthenticatePlayer(player);
            playerCache = playerCacheMap.get(uuid);
            playerCache.wasOnFire = false;
        }

        if(config.main.spawnOnJoin)
            teleportPlayer(player, true);


        // Tries to rescue player from nether portal
        if(config.main.tryPortalRescue && player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL)) {
            BlockPos pos = player.getBlockPos();

            // Faking portal blocks to be air
            BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
            player.networkHandler.sendPacket(feetPacket);

            BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
            player.networkHandler.sendPacket(headPacket);

            // Teleporting player to the middle of the block
            player.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        if(isPlayerFake(player))
            return;
        String uuid = convertUuid(player);
        PlayerCache playerCache = playerCacheMap.get(uuid);

        playerCache.lastIp = player.getIp();
        playerCache.lastAir = player.getAir();
        playerCache.wasOnFire = player.isOnFire();
        playerCache.wasInPortal = player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL);
        playerCache.lastDim = String.valueOf(player.getEntityWorld().getRegistryKey().getValue());
        playerCache.lastX = player.getX();
        playerCache.lastY = player.getY();
        playerCache.lastZ = player.getZ();

        // Setting the session expire time
        if(isAuthenticated(player) && config.main.sessionTimeoutTime != -1)
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
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(PlayerEntity player) {
        // Player will fall if enabled (prevent fly kick)
        boolean auth = isAuthenticated((ServerPlayerEntity) player);
        if(!auth && config.main.allowFalling && !player.isOnGround() && !player.isInsideWaterOrBubbleColumn()) {
            if(player.isInvulnerable())
                player.setInvulnerable(false);
            return ActionResult.PASS;
        }
        // Otherwise movement should be disabled
        else if(!auth && !config.experimental.allowMovement) {
            if(!player.isInvulnerable())
                player.setInvulnerable(true);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowBlockUse) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowBlockPunch) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemUse) {
            player.sendMessage(notAuthenticated(player), false);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }
    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemDrop) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowItemMoving) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.experimental.allowEntityPunch) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if(!isAuthenticated((ServerPlayerEntity) player) && !config.main.allowEntityInteract) {
            player.sendMessage(notAuthenticated(player), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}