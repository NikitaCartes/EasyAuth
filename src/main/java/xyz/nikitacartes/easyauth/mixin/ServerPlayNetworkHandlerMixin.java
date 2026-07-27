package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//? if >= 1.21 {
import net.minecraft.network.DisconnectionInfo;
//?} else {
/*import net.minecraft.text.Text;
*///?}
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import xyz.nikitacartes.easyauth.integrations.VanishIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import static net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND;
import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.extendedConfig;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    // Afaik we don't really care if this is cancelled before or after the validateMessage
    // In case the player is not allowed to send message anyway then doing it before should save resources
    @Inject(
            method = "onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.20.5 {
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateAcknowledgment(Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    //?} else if >= 1.20.3 {
                    /*target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateMessage(Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    *///?} else {
                    /*target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;validateMessage(Ljava/lang/String;Ljava/time/Instant;Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)Ljava/util/Optional;",
                    *///?}
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void onPlayerChat(ChatMessageC2SPacket packet, CallbackInfo ci) {
        ActionResult result = AuthEventHandler.onPlayerChat(this.player);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onPlayerAction(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() == SWAP_ITEM_WITH_OFFHAND) {
            ActionResult result = AuthEventHandler.onTakeItem(this.player);
            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket, CallbackInfo ci) {
        ActionResult result = AuthEventHandler.onPlayerMove(player);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        ActionResult result = AuthEventHandler.onTakeItem(this.player);

        if (result == ActionResult.FAIL) {
            // Canceling the item taking
            // Updating is not working yet
            ci.cancel();
        }
    }

    //? if >= 1.20.2 {
    @WrapMethod(method = "onPlayerSession(Lnet/minecraft/network/packet/c2s/play/PlayerSessionC2SPacket;)V")
    private void onPlayerSessionWrap(PlayerSessionC2SPacket packet, Operation<Void> original) {
        if (!extendedConfig.forcedOfflineUuid || !((PlayerAuth) this.player).easyAuth$isUsingMojangAccount()) {
            original.call(packet);
        }
    }
    //?}

    //? if >= 1.21 {
    @Inject(method = "onDisconnected(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At("TAIL"))
    private void onPlayerDisconnectUnVanish(DisconnectionInfo info, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "onDisconnected(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void onPlayerDisconnectUnVanish(Text reason, CallbackInfo ci) {
    *///?}
        PlayerAuth playerAuth = (PlayerAuth) this.player;
        if (playerAuth.easyAuth$canSkipAuth() || playerAuth.easyAuth$isAuthenticated()) {
            return;
        }
        if (config.vanishUntilAuth) {
            VanishIntegration.setVanished(this.player, playerAuth.easyAuth$wasVanished());
        }
    }
}
