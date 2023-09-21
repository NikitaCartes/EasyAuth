package xyz.nikitacartes.easyauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    GameProfile profile;

    @Shadow
    ServerLoginNetworkHandler.State state;

    @Final
    @Shadow
    MinecraftServer server;

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
                    target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"
            ),
            cancellable = true
    )
    private void checkPremium(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (server.isOnlineMode()) {
            try {
                String playername = packet.name().toLowerCase();
                Pattern pattern = Pattern.compile("^[a-z0-9_]{3,16}$");
                Matcher matcher = pattern.matcher(playername);
                if (config.main.forcedOfflinePlayers.contains(playername)) {
                    LogDebug("Player " + playername + " is forced to be offline");
                    mojangAccountNamesCache.remove(playername);
                    state = ServerLoginNetworkHandler.State.VERIFYING;

                    this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(packet.name()), packet.name());
                    ci.cancel();
                    return;
                }
                if (mojangAccountNamesCache.contains(playername) || config.experimental.verifiedOnlinePlayer.contains(playername)) {
                    LogDebug("Player " + playername + " is cached as online player. Authentication continues as vanilla");
                    mojangAccountNamesCache.add(playername);
                    return;
                }
                if ((playerCacheMap.containsKey(Uuids.getOfflinePlayerUuid(playername).toString()) || !matcher.matches())) {
                    // Player definitely doesn't have a mojang account
                    LogDebug("Player " + playername + " is cached as offline player");
                    state = ServerLoginNetworkHandler.State.VERIFYING;

                    this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(packet.name()), packet.name());
                    ci.cancel();
                } else {
                    // Checking account status from API
                    LogDebug("Checking player " + playername + " for premium status");
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + playername).openConnection();
                    httpsURLConnection.setRequestMethod("GET");
                    httpsURLConnection.setConnectTimeout(5000);
                    httpsURLConnection.setReadTimeout(5000);

                    int response = httpsURLConnection.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        // Player has a Mojang account
                        httpsURLConnection.disconnect();
                        LogDebug("Player " + playername + " has a Mojang account");

                        // Caches the request
                        mojangAccountNamesCache.add(playername);
                        config.experimental.verifiedOnlinePlayer.add(playername);
                        config.save(new File("./mods/EasyAuth/config.json"));
                        // Authentication continues in original method
                    } else if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Player doesn't have a Mojang account
                        httpsURLConnection.disconnect();
                        LogDebug("Player " + playername + " doesn't have a Mojang account");
                        state = ServerLoginNetworkHandler.State.VERIFYING;

                        this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(packet.name()), packet.name());
                        ci.cancel();
                    }
                }
            } catch (IOException e) {
                LogError("checkPremium error", e);
            }
        }
    }
}
