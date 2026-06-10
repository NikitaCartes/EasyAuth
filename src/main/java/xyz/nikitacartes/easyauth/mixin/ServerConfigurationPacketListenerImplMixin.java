//? if >= 1.21.9 {
package xyz.nikitacartes.easyauth.mixin;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.PrepareSpawnTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.integrations.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.utils.PlayersCache;
import xyz.nikitacartes.easyauth.utils.StoneCutterUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.ZonedDateTime;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.Utils.getIp;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImpl {

    @Shadow
    private PrepareSpawnTask prepareSpawnTask;

    @Final
    @Shadow
    private GameProfile gameProfile;

    @Inject(method = "returnToWorld()V",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z", ordinal = 0))
    private void determineAuthenticationStatus(CallbackInfo ci) {
        PrepareSpawnTaskInterface spawnTask = (PrepareSpawnTaskInterface) prepareSpawnTask;

        PlayerEntryV1 entry = PlayersCache.get(gameProfile.name());
        if ((entry == null) ||
                (this.server.usesAuthentication() && config.premiumAutoLogin && entry.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE) ||
                (config.floodgateAutoLogin && FloodgateApiHelper.isFloodgatePlayer(gameProfile.id())) ||
                easyAuth$isSkipAllAuthChecksApplicable(entry)) {
            spawnTask.easyAuth$setAuthenticated(true);
            LogDebug(String.format("Player %s is considered authenticated by default", gameProfile.name()));

            return;
        }

        if (entry.lastIp.isEmpty()) {
            spawnTask.easyAuth$setAuthenticated(false);
            LogDebug(String.format("Player %s is not authenticated: no IP", gameProfile.name()));

            return;
        }

        SocketAddress socketAddress = ((ServerConfigurationPacketListenerImpl)(Object)this).connection.getRemoteAddress();

        String ipAddress = getIp(socketAddress);
        if (ipAddress == null) {
            spawnTask.easyAuth$setAuthenticated(false);
            LogDebug(String.format("Player %s is not authenticated: no IP", gameProfile.name()));

            return;
        }

        if (entry.lastIp.equals(ipAddress) && entry.lastAuthenticatedDate.plusSeconds(config.sessionTimeout).isAfter(ZonedDateTime.now())) {
            spawnTask.easyAuth$setAuthenticated(true);
            LogDebug(String.format("Player %s is authenticated by alive session", gameProfile.name()));

            return;
        }

        spawnTask.easyAuth$setAuthenticated(false);
        LogDebug(String.format("Player %s is not authenticated", gameProfile.name()));
    }

    public ServerConfigurationPacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Unique
    private boolean easyAuth$isSkipAllAuthChecksApplicable(PlayerEntryV1 entry) {
        if (!extendedConfig.skipAllAuthChecks) {
            return false;
        }

        if (extendedConfig.skipAllAuthChecksNotForRegisteredPlayers && entry != null && !entry.password.isEmpty()) {
            return false;
        }

        if (extendedConfig.skipAllAuthChecksNotForOperators && StoneCutterUtils.isAdministrator(this.server.getPlayerList(), this.gameProfile)) {
            return false;
        }

        return true;
    }
}
//?}