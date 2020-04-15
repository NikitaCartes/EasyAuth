package org.samo_lego.simpleauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.samo_lego.simpleauth.SimpleAuth.config;


@Mixin(ServerLoginNetworkHandler.class)
public abstract class MixinServerLoginNetworkHandler {

    @Shadow @Final
    private MinecraftServer server;

    @Shadow
    private GameProfile profile;

    @Inject(method = "acceptPlayer()V", at = @At("HEAD"), cancellable = true)
    private void acceptPlayer(CallbackInfo ci) {
        //  Player pre-join event, we don't do standard callback, since
        // there are lots of variables that would need to be passed over
        PlayerEntity onlinePlayer = this.server.getPlayerManager().getPlayer(this.profile.getName());

        // Getting network handler
        ServerLoginNetworkHandler handler = (ServerLoginNetworkHandler) (Object) this;

        if (config.experimental.disableAnotherLocationKick && onlinePlayer != null) {
            // Player needs to be kicked, since there's already a player with that name
            // playing on the server
            handler.disconnect(new LiteralText(String.format(config.lang.playerAlreadyOnline, onlinePlayer.getName().asString())));
            ci.cancel();
        }
    }
}
