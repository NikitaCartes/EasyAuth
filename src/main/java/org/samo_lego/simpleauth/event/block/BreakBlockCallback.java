package org.samo_lego.simpleauth.event.block;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface BreakBlockCallback {

    Event<BreakBlockCallback> EVENT = EventFactory.createArrayBacked(BreakBlockCallback.class, listeners -> (world, pos, state, player) -> {
        for(BreakBlockCallback callback : listeners) {
            if(callback.onBlockBroken(world, pos, state, player)) {
                return true;
            }
        }
        return false;
    });
    boolean onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player);
}
