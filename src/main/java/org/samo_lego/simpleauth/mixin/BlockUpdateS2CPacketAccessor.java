package org.samo_lego.simpleauth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockUpdateS2CPacket.class)
public interface BlockUpdateS2CPacketAccessor {
    @Accessor("state")
    void setState(BlockState state);

    @Accessor("pos")
    void setBlockPos(BlockPos pos);
}
