package org.samo_lego.simpleauth.event.entity.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface BreakBlockCallback {

    Event<BreakBlockCallback> EVENT = EventFactory.createArrayBacked(BreakBlockCallback.class, listeners -> (world, pos, state, player) -> {
        for(BreakBlockCallback callback : listeners) {
            if(callback.onBlockBroken(world, pos, state, player)) {
                return true;
            }
        }
        return false;
    });

    /**
     * fired when a block is broken by a player
     *
     * @return {@code true} to cancel the event
     */
    boolean onBlockBroken(World world, BlockPos pos, BlockState state, @Nullable PlayerEntity player);
}
