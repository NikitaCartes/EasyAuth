package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.samo_lego.simpleauth.event.block.BreakBlockCallback;
import org.samo_lego.simpleauth.event.block.InteractBlockCallback;
import org.samo_lego.simpleauth.event.entity.player.InteractItemCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {

    @Shadow public ServerWorld world;

    @Shadow public ServerPlayerEntity player;

    // We inject the following code to tryBreakBlock method, by TextileLib
    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        // Triggering the event
        if(BreakBlockCallback.EVENT.invoker().onBlockBroken(world, pos, world.getBlockState(pos), player)) {
            info.setReturnValue(false);
        }
    }

    // Interacting with blocks
    @Inject(method="interactBlock", at = @At("HEAD"), cancellable = true)
    private void interactBlock(PlayerEntity playerEntity_1, World world_1, ItemStack itemStack_1, Hand hand_1, BlockHitResult blockHitResult_1, CallbackInfoReturnable<ActionResult> info){
        // Callback
        ActionResult result = InteractBlockCallback.EVENT.invoker().onInteractBlock((ServerPlayerEntity) playerEntity_1);
        if(result == ActionResult.FAIL) {
            info.cancel();
        }
    }

    // Interacting with items
    @Inject(method="interactItem", at = @At("HEAD"), cancellable = true)
    private void interactItem(PlayerEntity playerEntity_1, World world_1, ItemStack itemStack_1, Hand hand_1, CallbackInfoReturnable<ActionResult> info){
        // Callback
        ActionResult result = InteractItemCallback.EVENT.invoker().onInteractItem((ServerPlayerEntity) playerEntity_1);
        if(result == ActionResult.FAIL) {
            info.cancel();
        }
    }
}
