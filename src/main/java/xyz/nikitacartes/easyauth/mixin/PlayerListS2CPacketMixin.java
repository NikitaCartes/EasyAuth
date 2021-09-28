package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static xyz.nikitacartes.easyauth.EasyAuth.config;

@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin {

    @ModifyVariable(
            method = "<init>(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Action;[Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private static ServerPlayerEntity[] playerListS2CPacket(ServerPlayerEntity[] players) {
        if (config.main.hideUnauthenticatedPLayersFromPlayerList) {
            List<ServerPlayerEntity> temp = new ArrayList<>();
            Collections.addAll(temp, players);
            temp.removeIf(player -> !PlayerCache.isAuthenticated(((PlayerAuth) player).getFakeUuid()));
            return temp.toArray(new ServerPlayerEntity[0]);
        }
        return players;
    }

    @ModifyVariable(
            method = "<init>(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Action;Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private static Collection<ServerPlayerEntity> playerListS2CPacket(Collection<ServerPlayerEntity> players) {
        if (config.main.hideUnauthenticatedPLayersFromPlayerList) {
            players.removeIf(player -> !PlayerCache.isAuthenticated(((PlayerAuth) player).getFakeUuid()));
        }
        return players;
    }

}