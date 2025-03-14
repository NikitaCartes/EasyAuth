package xyz.nikitacartes.easyauth.event;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import xyz.nikitacartes.easyauth.utils.PlayersCache;
import xyz.nikitacartes.easyauth.telegram.TelegramManager;
import xyz.nikitacartes.easyauth.config.TelegramConfigV1;
import xyz.nikitacartes.easyauth.EasyAuth;

import java.net.SocketAddress;
import java.time.ZonedDateTime;
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
    public static Text checkCanPlayerJoinServer(GameProfile profile, PlayerManager manager, SocketAddress socketAddress) {
        // Getting the player. By this point, the player's game profile has been authenticated so the UUID is legitimate.
        String incomingPlayerUsername = profile.getName();
        PlayerEntity onlinePlayer = manager.getPlayer(incomingPlayerUsername);

        if ((onlinePlayer != null && !((PlayerAuth) onlinePlayer).easyAuth$canSkipAuth()) && extendedConfig.preventAnotherLocationKick) {
            // Player needs to be kicked, since there's already a player with that name
            // playing on the server

            // if joining from same IP, allow the player to join
            String string = socketAddress.toString();
            if (string.contains("/")) {
                string = string.substring(string.indexOf(47) + 1);
            }

            if (string.contains(":")) {
                string = string.substring(0, string.indexOf(58));
            }

            if (!((PlayerAuth) onlinePlayer).easyAuth$getIpAddress().equals(string)) {
                return langConfig.playerAlreadyOnline.get(incomingPlayerUsername);
            }
        }

        // Checking if player username is valid. The pattern is generated when the config is (re)loaded.
        Matcher matcher = usernamePattern.matcher(incomingPlayerUsername);

        if (!(matcher.matches() || (technicalConfig.floodgateLoaded && extendedConfig.floodgateBypassRegex && FloodgateApiHelper.isFloodgatePlayer(profile.getId())))) {
            return langConfig.disallowedUsername.get(extendedConfig.usernameRegexp);
        }
        // If the player name and registered name are different, kick the player if differentUsernameCase is enabled
        // Create in case of Floodgate player
        PlayerEntryV1 playerEntryV1 = PlayersCache.getFloodgate(incomingPlayerUsername);

        if (!extendedConfig.allowCaseInsensitiveUsername && !playerEntryV1.username.equals(incomingPlayerUsername)) {
            return langConfig.differentUsernameCase.get(incomingPlayerUsername);
        }

        if (config.maxLoginTries != -1 && playerEntryV1.lastKickedDate.plusSeconds(config.resetLoginAttemptsTimeout).isAfter(ZonedDateTime.now())) {
            return langConfig.loginTriesExceeded.get();
        }

        return null;
    }

    public static void loadPlayerData(ServerPlayerEntity player, ClientConnection connection) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        // Create in case of Carpet player
        PlayerEntryV1 cache = PlayersCache.getCarpet(player.getNameForScoreboard());
        boolean update = false;
        if (cache.uuid == null) {
            cache.uuid = player.getUuid();
            update = true;
        }
        playerAuth.easyAuth$setPlayerEntryV1(cache);

        playerAuth.easyAuth$setIpAddress(connection);
        playerAuth.easyAuth$setSkipAuth();

        if (playerAuth.easyAuth$canSkipAuth()) {
            playerAuth.easyAuth$setAuthenticated(true);

            player.setInvulnerable(false);
            player.setInvisible(false);
            update = false;
        } else if (cache.lastIp.equals(playerAuth.easyAuth$getIpAddress()) && cache.lastAuthenticatedDate.plusSeconds(config.sessionTimeout).isAfter(ZonedDateTime.now())) {
            playerAuth.easyAuth$setAuthenticated(true);

            player.setInvulnerable(false);
            player.setInvisible(false);

            cache.lastAuthenticatedDate = ZonedDateTime.now();
            update = true;
        }

        if (update) {
            cache.update();
        }

        if (extendedConfig.skipAllAuthChecks) {
            playerAuth.easyAuth$setAuthenticated(true);
        }
    }

    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        if (playerAuth.easyAuth$canSkipAuth()) {
            langConfig.onlinePlayerLogin.send(player);
            
            // Отправляем уведомление в Telegram для онлайн-игроков
            if (EasyAuth.telegramManager != null && EasyAuth.telegramManager.isEnabled() && EasyAuth.telegramConfig.notifications.enableLoginNotifications) {
                final String username = player.getNameForScoreboard();
                String ip = playerAuth.easyAuth$getIpAddress();
                String message = String.format("🔐 Вход онлайн-игрока!\n\nИмя игрока: %s\nIP-адрес: %s\nВремя: %s", 
                        username, ip, ZonedDateTime.now().toString());
                
                // Отправляем асинхронно
                THREADPOOL.execute(() -> {
                    boolean sent = EasyAuth.telegramManager.sendNotification(username, message);
                    if (sent) {
                        LogDebug("Sent Telegram online player login notification to user: " + username);
                    }
                });
            }
            
            return;
        } else if (playerAuth.easyAuth$isAuthenticated()) {
            langConfig.validSession.send(player);
            
            // Отправляем уведомление в Telegram для игроков с действительной сессией
            if (EasyAuth.telegramManager != null && EasyAuth.telegramManager.isEnabled() && EasyAuth.telegramConfig.notifications.enableLoginNotifications) {
                final String username = player.getNameForScoreboard();
                String ip = playerAuth.easyAuth$getIpAddress();
                String message = String.format("🔐 Вход с действительной сессией!\n\nИмя игрока: %s\nIP-адрес: %s\nВремя: %s", 
                        username, ip, ZonedDateTime.now().toString());
                
                // Отправляем асинхронно
                THREADPOOL.execute(() -> {
                    boolean sent = EasyAuth.telegramManager.sendNotification(username, message);
                    if (sent) {
                        LogDebug("Sent Telegram valid session login notification to user: " + username);
                    }
                });
            }
            
            return;
        } else if (extendedConfig.skipAllAuthChecks) {
            return;
        }

        // Tries to rescue player from nether portal
        if (extendedConfig.tryPortalRescue) {
            BlockPos pos = player.getBlockPos();
            player.teleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5, false);
            if (player.getBlockStateAtPos().getBlock().equals(Blocks.NETHER_PORTAL) || player.getWorld().getBlockState(player.getBlockPos().up()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                // Faking portal blocks to be air
                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);

                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        if (playerAuth.easyAuth$canSkipAuth())
            return;

        if (playerAuth.easyAuth$isAuthenticated()) {
            PlayerEntryV1 playerCache = playerAuth.easyAuth$getPlayerEntryV1();
            playerCache.lastAuthenticatedDate = ZonedDateTime.now();
            playerCache.update();
        } else if (config.hidePlayerCoords) {
            ((PlayerAuth) player).easyAuth$restoreTrueLocation();

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
                || (extendedConfig.aliases.login && command.startsWith("l "))
                || (extendedConfig.aliases.register && command.startsWith("reg "))) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).easyAuth$isAuthenticated()) {
            for (String allowedCommand : extendedConfig.allowedCommands) {
                if (command.startsWith(allowedCommand)) {
                    LogDebug("Player " + player.getNameForScoreboard() + " executed command " + command + " without being authenticated.");
                    return ActionResult.PASS;
                }
            }
            LogDebug("Player " + player.getNameForScoreboard() + " tried to execute command " + command + " without being authenticated.");
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player chatting
    public static ActionResult onPlayerChat(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowChat) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(ServerPlayerEntity player) {
        // Player will fall if enabled (prevent fly kick)
        boolean auth = ((PlayerAuth) player).easyAuth$isAuthenticated();
        // Otherwise, movement should be disabled
        if (!auth && !extendedConfig.allowMovement) {
            if (System.nanoTime() >= lastAcceptedPacket + extendedConfig.teleportationTimeoutMs * 1000000) {
                player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                lastAcceptedPacket = System.nanoTime();
            }
            if (!player.isInvulnerable())
                player.setInvulnerable(extendedConfig.playerInvulnerable);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Breaking a block
    public static boolean onBreakBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowBlockBreaking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return false;
        }
        return true;
    }

    // Using an item
    public static ActionResult onUseItem(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemUsing) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemDropping) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowItemMoving) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityAttacking) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).easyAuth$isAuthenticated() && !extendedConfig.allowEntityInteraction) {
            ((PlayerAuth) player).easyAuth$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    public static void onPreLogin(ServerLoginNetworkHandler netHandler, MinecraftServer server, PacketSender packetSender, ServerLoginNetworking.LoginSynchronizer sync) {
        if (extendedConfig.forcedOfflineUuid && netHandler.profile != null) {
            netHandler.profile = Uuids.getOfflinePlayerProfile(netHandler.profile.getName());
        }
    }

}
