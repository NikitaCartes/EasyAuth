package xyz.nikitacartes.easyauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.core.UUIDUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.PlayersCache;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.integrations.MojangApi.getUuid;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    @Shadow
    public GameProfile authenticatedProfile;

    @Shadow
    private ServerLoginPacketListenerImpl.State state;

    @Final
    @Shadow
    MinecraftServer server;

    @Unique
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    /**
     * Checks whether the player has purchased an account.
     * If so, server is presented as online, and continues as in normal-online mode.
     * Otherwise, player is marked as ready to be accepted into the game.
     *
     * @param packet
     * @param ci
     */
    @Inject(
            method = "handleHello(Lnet/minecraft/network/protocol/login/ServerboundHelloPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"
            ),
            cancellable = true
    )
    private void checkPremium(ServerboundHelloPacket packet, CallbackInfo ci) {
        String username = packet.name();

        LogDebug("UUID of player " + username + " is " + packet.profileId());

        PlayerEntryV1 playerData = PlayersCache.loadOrRegister(username);

        if (server.usesAuthentication()) {
            try {
                Matcher matcher = pattern.matcher(username);

                if (playerData.onlineAccount == PlayerEntryV1.OnlineAccount.FALSE) {
                    LogDebug("Player " + username + " is forced to be offline");

                    state = getReadyState();
                    this.authenticatedProfile = getGameProfile(packet.name());
                    ci.cancel();
                    return;
                }
                if (playerData.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE) {
                    LogDebug("Player " + username + " is cached as online player. Authentication continues as vanilla");
                    return;
                }
                if (!matcher.matches()) {
                    // Player definitely doesn't have a mojang account
                    LogDebug("Player " + username + " doesn't have a valid username for Mojang account");

                    state = getReadyState();
                    playerData.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
                    playerData.update();

                    this.authenticatedProfile = getGameProfile(packet.name());
                    ci.cancel();
                } else {
                    UUID onlineUuid = getUuid(username);

                    if ((EasyAuth.extendedConfig.preventOfflinePlayersWithOnlineUsernames && onlineUuid != null) || checkUuid(packet.profileId(), onlineUuid)) {
                        // Caches the request
                        playerData.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
                        playerData.update();
                        // Authentication continues in the original method
                    } else {
                        if (onlineUuid == null) {
                            LogDebug("Player " + username + " doesn't have a Mojang account");
                            playerData.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
                            playerData.update();
                        } else {
                            LogInfo("Player " + username + " has a Mojang account, but UUID mismatch: expected " + onlineUuid + ", got " + packet.profileId());
                            if (!EasyAuth.extendedConfig.checkOfflinePlayersWithOnlineUsernames) {
                                playerData.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
                                playerData.update();
                            }
                        }
                        state = getReadyState();
                        this.authenticatedProfile = getGameProfile(packet.name());
                        ci.cancel();
                    }
                }
            } catch (IOException e) {
                LogError("checkPremium error", e);
            }
        }
    }

    @Unique
    private GameProfile getGameProfile(String name) {
        // Check if player has a forced UUID set
        PlayerEntryV1 playerData = PlayersCache.get(name);
        if (playerData != null && playerData.forcedUuid != null && !playerData.forcedUuid.isEmpty()) {
            try {
                UUID forcedUuid = UUID.fromString(playerData.forcedUuid);
                LogInfo("Using forced UUID " + forcedUuid + " for player " + name);
                return new GameProfile(forcedUuid, name);
            } catch (IllegalArgumentException e) {
                LogError("Invalid forced UUID for player " + name + ": " + playerData.forcedUuid, e);
            }
        }
        return new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
    }

    @Unique
    private ServerLoginPacketListenerImpl.State getReadyState() {
        return ServerLoginPacketListenerImpl.State.VERIFYING;
    }

    @Unique
    private boolean checkUuid(UUID uuid, UUID onlineUuid) {
        return uuid.equals(onlineUuid);
    }

}