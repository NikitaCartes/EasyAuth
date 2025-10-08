package xyz.nikitacartes.easyauth.mixin;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.PrepareSpawnTask;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.integrations.FloodgateApiHelper;
import xyz.nikitacartes.easyauth.utils.PlayersCache;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.ZonedDateTime;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;

@Mixin(ServerConfigurationNetworkHandler.class)
public abstract class ServerConfigurationNetworkHandlerMixin {

    @Shadow
    private PrepareSpawnTask prepareSpawnTask;

    @Final
    @Shadow
    private GameProfile profile;

    @Inject(method = "endConfiguration()V",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z", ordinal = 0))
    private void determineAuthenticationStatus(CallbackInfo ci) {
        PrepareSpawnTaskInterface spawnTask = (PrepareSpawnTaskInterface) prepareSpawnTask;

        PlayerEntryV1 entry = PlayersCache.get(profile.name());
        if ((entry == null) ||
                (entry.onlineAccount == PlayerEntryV1.OnlineAccount.TRUE) ||
                (config.floodgateAutoLogin && FloodgateApiHelper.isFloodgatePlayer(profile.id())) ||
                (extendedConfig.skipAllAuthChecks)) {
            spawnTask.easyAuth$setAuthenticated(true);
            LogDebug(String.format("Player %s is considered authenticated by default", profile.name()));

            return;
        }

        if (entry.lastIp.isEmpty()) {
            spawnTask.easyAuth$setAuthenticated(false);
            LogDebug(String.format("Player %s is not authenticated: no IP", profile.name()));

            return;
        }

        SocketAddress socketAddress = ((ServerConfigurationNetworkHandler)(Object)this).connection.getAddress();
        String ipAddress = socketAddress instanceof InetSocketAddress inetSocketAddress ? InetAddresses.toAddrString(inetSocketAddress.getAddress()) : "<unknown>";
        if (entry.lastIp.equals(ipAddress) && entry.lastAuthenticatedDate.plusSeconds(config.sessionTimeout).isAfter(ZonedDateTime.now())) {
            spawnTask.easyAuth$setAuthenticated(true);
            LogDebug(String.format("Player %s is authenticated by alive session", profile.name()));

            return;
        }

        spawnTask.easyAuth$setAuthenticated(false);
        LogDebug(String.format("Player %s is not authenticated", profile.name()));
    }
}
