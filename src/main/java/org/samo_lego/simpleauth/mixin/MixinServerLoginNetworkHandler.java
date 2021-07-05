package org.samo_lego.simpleauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.samo_lego.simpleauth.SimpleAuth.*;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class MixinServerLoginNetworkHandler {

    @Shadow
    private GameProfile profile;

    @Shadow protected abstract GameProfile toOfflineProfile(GameProfile profile);

    @Shadow private ServerLoginNetworkHandler.State state;

    @Inject(method = "acceptPlayer()V", at = @At("HEAD"))
    private void acceptPlayer(CallbackInfo ci) {
        if(config.experimental.forceoOfflineUuids) {
            this.profile = this.toOfflineProfile(this.profile);
        }
    }

    /**
     * Checks whether the player has purchased an account.
     * If so, server is presented as online, and continues as in normal-online mode.
     * Otherwise, player is marked as ready to be accepted into the game.
     * @param packet
     * @param ci
     */
    @Inject(
            method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;getProfile()Lcom/mojang/authlib/GameProfile;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void checkPremium(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if(config.main.premiumAutologin) {
            try {
                String playername = packet.getProfile().getName().toLowerCase();
                Pattern pattern = Pattern.compile("^[a-z0-9_]{3,16}$");
                Matcher matcher = pattern.matcher(playername);
                if(playerCacheMap.containsKey(PlayerEntity.getOfflinePlayerUuid(playername).toString()) || !matcher.matches() || config.main.forcedOfflinePlayers.contains(playername)) {
                    // Player definitely doesn't have a mojang account
                    state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;

                    this.profile = packet.getProfile();
                    ci.cancel();
                }
                else if(!mojangAccountNamesCache.contains(playername))  {
                    // Checking account status from API
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + playername).openConnection();
                    httpsURLConnection.setRequestMethod("GET");
                    httpsURLConnection.setConnectTimeout(5000);
                    httpsURLConnection.setReadTimeout(5000);

                    int response = httpsURLConnection.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        // Player has a Mojang account
                        httpsURLConnection.disconnect();


                        // Caches the request
                        mojangAccountNamesCache.add(playername);
                        // Authentication continues in original method
                    }
                    else if(response == HttpURLConnection.HTTP_NO_CONTENT) {
                        // Player doesn't have a Mojang account
                        httpsURLConnection.disconnect();
                        state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;

                        this.profile = packet.getProfile();
                        ci.cancel();
                    }
                }
            } catch (IOException e) {
                logError(e.getMessage());
            }
        }
    }
}
