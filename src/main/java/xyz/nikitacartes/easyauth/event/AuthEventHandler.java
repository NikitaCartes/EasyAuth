package xyz.nikitacartes.easyauth.event;

import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.TextParser;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.playerCacheMap;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {

    public static long lastAcceptedPacket = 0;

    /**
     * Player pre-join.
     * Returns text as a reason for disconnect or null to pass
     *
     * @param profile GameProfile of the player
     * @param manager PlayerManager
     * @return Text if player should be disconnected
     */
    public static Text checkCanPlayerJoinServer(GameProfile profile, PlayerManager manager) {
        // Getting the player
        String incomingPlayerUsername = profile.getName();
        PlayerEntity onlinePlayer = manager.getPlayer(incomingPlayerUsername);

        // Checking if player username is valid
        String regex = config.main.usernameRegex;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(incomingPlayerUsername);

        if ((onlinePlayer != null && !((PlayerAuth) onlinePlayer).canSkipAuth()) && config.experimental.preventAnotherLocationKick) {
            // Player needs to be kicked, since there's already a player with that name
            // playing on the server
            return TextParser.parse(
                    String.format(
                            config.lang.playerAlreadyOnline, onlinePlayer.getName().asString()
                    )
            );
        } else if (!matcher.matches()) {
            return TextParser.parse(
                    String.format(
                            config.lang.disallowedUsername, regex
                    )
            );
        }
        // If the player has too many login attempts, kick them immediately.
        // For Mojang account in offline (not mixed) mode we get offline uuid too.
        String id = PlayerEntity.getOfflinePlayerUuid(incomingPlayerUsername.toLowerCase()).toString();
        if (config.main.maxLoginTries != -1 && playerCacheMap.containsKey(id)) {
            if (playerCacheMap.get(id).lastKicked >= System.currentTimeMillis() - 1000 * config.experimental.resetLoginAttemptsTime) {
                return TextParser.parse(config.lang.loginTriesExceeded);
            } else if (playerCacheMap.get(id).getLoginTries() >= config.main.maxLoginTries) {
                // The timeout at the very least has expired, so no harm in resetting the login tries...
                playerCacheMap.get(id).resetLoginTries();
            }
        }
        return null;
    }


    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (((PlayerAuth) player).canSkipAuth()) {
            player.setInvulnerable(false);
            player.setInvisible(false);
            return;
        }
        // Checking if session is still valid
        String uuid = ((PlayerAuth) player).getFakeUuid();
        PlayerCache playerCache;

        if (!playerCacheMap.containsKey(uuid)) {
            // First join
            playerCache = PlayerCache.fromJson(player, uuid);
            playerCacheMap.put(uuid, playerCache);
        } else {
            playerCache = playerCacheMap.get(uuid);
        }
        if (
                playerCache.isAuthenticated &&
                        playerCache.validUntil >= System.currentTimeMillis() &&
                        player.getIp().equals(playerCache.lastIp)
        ) {
            // Valid session
            player.setInvulnerable(false);
            player.setInvisible(false);
            return;
        }
        ((PlayerAuth) player).setAuthenticated(false);


        // Tries to rescue player from nether portal
        if (config.main.tryPortalRescue) {
            BlockPos pos = player.getBlockPos();
            if (player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL) || ((pos = playerInPortal(player)) != null)) {

                // Teleporting player to the middle of the block
                player.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

                // Faking portal blocks to be air
                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);

                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        if (((PlayerAuth) player).canSkipAuth())
            return;
        String uuid = ((PlayerAuth) player).getFakeUuid();
        PlayerCache playerCache = playerCacheMap.get(uuid);

        if (playerCache != null && playerCache.isAuthenticated) {
            playerCache.lastIp = player.getIp();
            playerCache.wasInPortal = player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL);

            // Setting the session expire time
            if (config.main.sessionTimeoutTime != -1)
                playerCache.validUntil = System.currentTimeMillis() + config.main.sessionTimeoutTime * 1000L;
        } else if (config.main.spawnOnJoin) {
            ((PlayerAuth) player).hidePosition(false);

            player.setInvulnerable(false);
            player.setInvisible(false);
        }
    }

    // Player chatting
    public static ActionResult onPlayerChat(ServerPlayerEntity player, String message) {
        // Getting the message to then be able to check it
        if (
                !((PlayerAuth) player).isAuthenticated() &&
                        !message.startsWith("/login") &&
                        !(message.startsWith("/l") && config.experimental.enableAliases) &&
                        !message.startsWith("/register") &&
                        (!config.experimental.allowChat || message.startsWith("/"))
        ) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(ServerPlayerEntity player) {
        // Player will fall if enabled (prevent fly kick)
        boolean auth = ((PlayerAuth) player).isAuthenticated();
        // Otherwise, movement should be disabled
        if (!auth && !config.experimental.allowMovement) {
            if (System.nanoTime() >= lastAcceptedPacket + config.experimental.teleportationTimeoutInMs * 1000000) {
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                lastAcceptedPacket = System.nanoTime();
            }
            if (!player.isInvulnerable())
                player.setInvulnerable(true);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowBlockUse) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Breaking a block
    public static boolean onBreakBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowBlockPunch) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return false;
        }
        return true;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowItemUse) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowItemDrop) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowItemMoving) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.experimental.allowEntityPunch) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !config.main.allowEntityInteract) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private static BlockPos playerInPortal(ServerPlayerEntity player) {
        Box playerBox = player.getBoundingBox();
        BlockPos minBlockPos = new BlockPos(playerBox.minX, playerBox.minY, playerBox.minZ);
        BlockPos maxBlockPos = new BlockPos(playerBox.maxX, playerBox.maxY, playerBox.maxZ);
        for (int x = minBlockPos.getX(); x <= maxBlockPos.getX(); x++) {
            for (int y = minBlockPos.getY(); y <= maxBlockPos.getY(); y++) {
                for (int z = minBlockPos.getZ(); z <= maxBlockPos.getZ(); z++) {
                    if (player.getWorld().getBlockState(new BlockPos(x, y, z)).getBlock().equals(Blocks.NETHER_PORTAL)) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }
}
