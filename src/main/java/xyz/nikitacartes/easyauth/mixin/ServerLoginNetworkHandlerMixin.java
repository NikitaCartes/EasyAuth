package xyz.nikitacartes.easyauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.storage.PlayerEntryV1;
import xyz.nikitacartes.easyauth.utils.PlayersCache;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    public GameProfile profile;

    @Shadow
    private ServerLoginNetworkHandler.State state;

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
            method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/GameProfile;<init>(Ljava/util/UUID;Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            cancellable = true
    )
    private void checkPremium(LoginHelloC2SPacket packet, CallbackInfo ci) {
        String username = packet.name();

        PlayerEntryV1 playerData = PlayersCache.getOrRegister(username);

        if (server.isOnlineMode()) {
            try {
                Matcher matcher = pattern.matcher(username);

                if (playerData.onlineAccount == PlayerEntryV1.OnlineAccount.FALSE) {
                    LogDebug("Player " + username + " is forced to be offline");
                    state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;

                    this.profile = new GameProfile(null, packet.name());
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
                    state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
                    playerData.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
                    playerData.update();

                    this.profile = new GameProfile(null, packet.name());
                    ci.cancel();
                } else {
                    // Checking account status from API
                    LogDebug("Checking player " + username + " for premium status");
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) URI.create(extendedConfig.mojangApiSettings.url + username).toURL().openConnection();
                    httpsURLConnection.setRequestMethod("GET");
                    httpsURLConnection.setConnectTimeout(extendedConfig.mojangApiSettings.connectionTimeout);
                    httpsURLConnection.setReadTimeout(extendedConfig.mojangApiSettings.readTimeout);

                    int response = httpsURLConnection.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        // Player has a Mojang account
                        httpsURLConnection.disconnect();
                        LogDebug("Player " + username + " has a Mojang account");

                        // Caches the request
                        playerData.onlineAccount = PlayerEntryV1.OnlineAccount.TRUE;
                        playerData.update();
                        // Authentication continues in original method
                    } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Player doesn't have a Mojang account
                        httpsURLConnection.disconnect();
                        LogDebug("Player " + username + " doesn't have a Mojang account");
                        state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;

                        playerData.onlineAccount = PlayerEntryV1.OnlineAccount.FALSE;
                        playerData.update();

                        this.profile = new GameProfile(null, packet.name());
                        ci.cancel();
                    }
                }
            } catch (IOException e) {
                LogError("checkPremium error", e);
            }
        }
    }
}
