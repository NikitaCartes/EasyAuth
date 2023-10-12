package xyz.nikitacartes.easyauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.*;

@Mixin(targets = "net.minecraft.server.network.ServerLoginNetworkHandler$1")
public abstract class ServerLoginNetworkHandlerThreadMixin {

    @Shadow @Final
    ServerLoginNetworkHandler field_14176;

    @Inject(method = "run()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;disconnect(Lnet/minecraft/text/Text;)V"),
            slice = @Slice(from = @At(value = "CONSTANT",
                    args="stringValue=multiplayer.disconnect.unverified_username")),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void addToOfflineList(CallbackInfo ci, String string) {
        String playername = string.toLowerCase();
        if (config.experimental.autoAddToForcedOffline &&
                !config.main.forcedOfflinePlayers.contains(playername) &&
                !config.experimental.verifiedOnlinePlayer.contains(playername)) {
            config.main.forcedOfflinePlayers.add(playername);
            THREADPOOL.submit(() -> {
                config.save(new File("./mods/EasyAuth/config.json"));
                LogInfo("Player " + string + " has been added to the forced offline list");
            });
            field_14176.state = ServerLoginNetworkHandler.State.VERIFYING;
            field_14176.connection.encrypted = false;
            field_14176.profile = new GameProfile(Uuids.getOfflinePlayerUuid(string), string);
            ci.cancel();
        }
    }

    @Inject(method = "run()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;startVerify(Lcom/mojang/authlib/GameProfile;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void addToVerifiedOnlineList(CallbackInfo ci, String string) {
        String playername = string.toLowerCase();
        if (!config.experimental.verifiedOnlinePlayer.contains(playername) && !config.main.forcedOfflinePlayers.contains(playername)) {
            mojangAccountNamesCache.add(playername);
            config.experimental.verifiedOnlinePlayer.add(playername);
            THREADPOOL.submit(() -> {
                config.save(new File("./mods/EasyAuth/config.json"));
                LogDebug("Player " + string + " has been added to the verified online list");
            });
        }
    }
}
