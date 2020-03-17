package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.samo_lego.simpleauth.event.entity.player.PlayerMoveCallback;
import org.samo_lego.simpleauth.event.item.DropItemCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    // Thanks to PR https://github.com/FabricMC/fabric/pull/260 and AbusedLib https://github.com/abused/AbusedLib
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        // Defining player
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Getting action result from auth event handler
        ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            // Canceling the item drop, as well as giving the items back to player (and updating inv with packet)
            player.inventory.insertStack(stack);
            player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(
                            -2,
                            player.inventory.selectedSlot,
                            player.inventory.getInvStack(player.inventory.selectedSlot))
            );
            // Packet for mouse-dropping inventory update
            player.currentScreenHandler.sendContentUpdates();
            //player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-1, -1, player.inventory.getCursorStack()));
            cir.cancel();
        }
    }

    // Player item dropping
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void dropSelectedItem(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        //Testing purposes - todo - doesn't delete armor, but allows mouse dropping
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ActionResult result = DropItemCallback.EVENT.invoker().onDropItem(player);

        if (result == ActionResult.FAIL) {
            // Canceling the item drop, as well as giving the items back to player (and updating inv with packet)
            player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(
                            -2,
                            player.inventory.selectedSlot,
                            player.inventory.getInvStack(player.inventory.selectedSlot))
            );
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-1, -1, player.inventory.getCursorStack()));
            cir.setReturnValue(false);
        }
    }

    // Player movement
    @Inject(method = "travel(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3d movementInput, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        ActionResult result = PlayerMoveCallback.EVENT.invoker().onPlayerMove(player);

        if (result == ActionResult.FAIL) {
            // A bit ugly, I know. (we need to update player position)
            player.teleport(player.getX(), player.getY(), player.getZ());
            ci.cancel();
        }
    }
}