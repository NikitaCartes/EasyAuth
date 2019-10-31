package org.samo_lego.simpleauth.event.block;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface BlockDropsCallback {

    Event<BlockDropsCallback> EVENT = EventFactory.createArrayBacked(BlockDropsCallback.class, listeners -> (world, pos, state, drops, harvester, tool) -> {
        for(BlockDropsCallback callback : listeners) {
            if(callback.onDrop(world, pos, state, drops, harvester, tool)) {
                return true;
            }
        }
        return false;
    });

    /**
     * fired when a block is about to drop it's items
     *
     * @return {@code true} to cancel all drops
     */
    boolean onDrop(World world, BlockPos pos, BlockState state, List<ItemStack> drops, @Nullable PlayerEntity harvester, ItemStack tool);
}
