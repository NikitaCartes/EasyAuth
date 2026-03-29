package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    // Denying item moving etc.
    @Inject(method = "mayPickup(Lnet/minecraft/world/entity/player/Player;)Z", at = @At(value = "HEAD"), cancellable = true)
    private void canTakeItems(Player playerEntity, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) playerEntity;
        InteractionResult result = AuthEventHandler.onTakeItem(player);

        if (result == InteractionResult.FAIL) {
            // Canceling the item taking
            player.connection.send(
                    new ClientboundContainerSetSlotPacket(
                            -2,
                            0,
                            player.getInventory().selected,
                            player.getInventory().getItem(player.getInventory().selected))
            );
            cir.setReturnValue(false);
        }
    }
}
