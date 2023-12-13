package xyz.nikitacartes.easyauth.event;

import com.mojang.authlib.GameProfile;
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
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {

    public static long lastAcceptedPacket = 0;

    public static Pattern usernamePattern;
    /**
     * Player pre-join.
     * Returns text as a reason for disconnect or null to pass
     *
     * @param profile GameProfile of the player
     * @param manager PlayerManager
     * @return Text if player should be disconnected
     */
    public static Text checkCanPlayerJoinServer(GameProfile profile, PlayerManager manager) {
        // Getting the player. By this point, the player's game profile has been authenticated so the UUID is legitimate.
        String incomingPlayerUsername = profile.getName();
        PlayerEntity onlinePlayer = manager.getPlayer(incomingPlayerUsername);

        // Checking if player username is valid. The pattern is generated when the config is (re)loaded.
        Matcher matcher = usernamePattern.matcher(incomingPlayerUsername);

        if ((onlinePlayer != null && !((PlayerAuth) onlinePlayer).canSkipAuth()) && extendedConfig.preventAnotherLocationKick) {
            // Player needs to be kicked, since there's already a player with that name
            // playing on the server
            return Text.of(
                    String.format(
                            langConfig.playerAlreadyOnline, onlinePlayer.getName().getContent()
                    )
            );
        } else if (!(matcher.matches() || (technicalConfig.floodgateLoaded && extendedConfig.floodgateBypassRegex && FloodgateApiHelper.isFloodgatePlayer(profile.getId())))) {
            return Text.of(
                    String.format(
                            langConfig.disallowedUsername, extendedConfig.usernameRegexp
                    )
            );
        }
        // If the player has too many login attempts, kick them immediately.
        if (config.maxLoginTries != -1) {
            // We won't load the player cache *into the map* if it is not already present (first join since restart)
            // because loading the player cache with a null player prevents the last location from being set.
            if (profile.getId() == null) {
                LogDebug("Player UUID is null, skipping kicking attempt check.");
                return null;
            }
            String incomingPlayerUuid = profile.getId().toString();
            PlayerCache playerCache = playerCacheMap.containsKey(incomingPlayerUuid) ?
                    playerCacheMap.get(incomingPlayerUuid) : PlayerCache.fromJson(null, incomingPlayerUuid);
            if (playerCache.lastKicked >= System.currentTimeMillis() - 1000 * config.resetLoginAttemptsTimeout) {
                return Text.of(langConfig.loginTriesExceeded);
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
        if (extendedConfig.tryPortalRescue) {
            BlockPos pos = player.getBlockPos();
            player.teleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
            if (player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL) || player.getWorld().getBlockState(player.getBlockPos().up()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                // Faking portal blocks to be air
                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);

                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);

                playerCache.wasInPortal = true;
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

            // Setting the session expire time
            if (config.sessionTimeout != -1)
                playerCache.validUntil = System.currentTimeMillis() + config.sessionTimeout * 1000L;
        } else if (config.hidePlayerCoords) {
            ((PlayerAuth) player).hidePosition(false);

            player.setInvulnerable(false);
            player.setInvisible(false);
        }
    }

    // Player execute command
    public static ActionResult onPlayerCommand(ServerPlayerEntity player, String command) {
        // Getting the message to then be able to check it
        if (extendedConfig.allowCommands) {
            return ActionResult.PASS;
        }
        if (player == null) {
            return ActionResult.PASS;
        }
        if (command.startsWith("login ")
                || command.startsWith("register ")
                || (extendedConfig.enableAliases && command.startsWith("l "))) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).isAuthenticated()) {
            for (String allowedCommand : extendedConfig.allowedCommands) {
                if (command.startsWith(allowedCommand)) {
                    LogDebug("Player " + player.getName() + " executed command " + command + " without being authenticated.");
                    return ActionResult.PASS;
                }
            }
            LogDebug("Player " + player.getName() + " tried to execute command " + command + " without being authenticated.");
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player chatting
    public static ActionResult onPlayerChat(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowChat) {
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
        if (!auth && !extendedConfig.allowMovement) {
            if (System.nanoTime() >= lastAcceptedPacket + extendedConfig.teleportationTimeoutMs * 1000000) {
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
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowBlockInteraction) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Breaking a block
    public static boolean onBreakBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowBlockBreaking) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return false;
        }
        return true;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowItemUsing) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }

    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowItemDropping) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowItemMoving) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowEntityAttacking) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).isAuthenticated() && !extendedConfig.allowEntityInteraction) {
            player.sendMessage(((PlayerAuth) player).getAuthMessage(), false);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

}
