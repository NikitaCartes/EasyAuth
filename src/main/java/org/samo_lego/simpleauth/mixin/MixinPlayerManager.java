package org.samo_lego.simpleauth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.samo_lego.simpleauth.SimpleAuth;
import org.samo_lego.simpleauth.event.entity.player.PlayerJoinServerCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.NetherPortalBlock.AXIS;
import static net.minecraft.util.math.Direction.Axis.Z;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void onPlayerConnect(ClientConnection clientConnection, ServerPlayerEntity serverPlayerEntity, CallbackInfo ci) {
        PlayerJoinServerCallback.EVENT.invoker().onPlayerJoin(serverPlayerEntity);
        if(SimpleAuth.config.main.tryPortalRescue && serverPlayerEntity.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL)) { //((MixinEntity) serverPlayerEntity).inNetherPortal()
            // Tries to rescue player from nether portal
            // Teleport - serverPlayerEntity.getBlockState().isOpaque();
            BlockState portalState = serverPlayerEntity.getBlockState();
            if(portalState.get(AXIS) == Z) {
                // Player should be put to northern or southern block
                System.out.println("(N/S is ok) Position %s" + serverPlayerEntity.getBlockPos().toString());
            }
            else {
                // Player should be put to eastern or western block
                System.out.println("(W/E is ok) Position %s" + serverPlayerEntity.getBlockPos().toString());
            }

        }
    }
}
