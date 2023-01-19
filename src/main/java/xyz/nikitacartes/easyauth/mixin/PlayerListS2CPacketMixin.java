package xyz.nikitacartes.easyauth.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import xyz.nikitacartes.easyauth.storage.PlayerCache;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;

import java.util.*;

import static xyz.nikitacartes.easyauth.EasyAuth.config;

@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin {
    @Mutable
    @Final
    @Shadow
    private List<PlayerListS2CPacket.Entry> entries;

    private static boolean hideFromTabList(ServerPlayerEntity player) {
        return !(PlayerCache.isAuthenticated(((PlayerAuth) player).getFakeUuid()) ||
                (((PlayerAuth) player).isUsingMojangAccount() && config.main.premiumAutologin));
    }
    @ModifyVariable(
            method = "<init>(Ljava/util/EnumSet;Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private static Collection<ServerPlayerEntity> playerListS2CPacket(Collection<ServerPlayerEntity> players) {
        // direct removeIf errors out as this seems to receive ImmutableCollection from time to time (?)
        if (config.main.hideUnauthenticatedPLayersFromPlayerList) {
            ArrayList<ServerPlayerEntity> temp = new ArrayList<>();
            for (ServerPlayerEntity player : players) {
                if (!hideFromTabList(player)) {
                    temp.add(player);
                }
            }
            return temp.stream().toList();
        }
        return players;
    }
    /* Check the other, single player arg constructor - overriding the entries field with empty if not allowed */
    @Redirect(
            method = "<init>(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Action;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;entries:Ljava/util/List;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void checkSetEntries(PlayerListS2CPacket instance, List<PlayerListS2CPacket.Entry> entries, PlayerListS2CPacket.Action _action, ServerPlayerEntity player) {
        assert !entries.isEmpty();
        if (config.main.hideUnauthenticatedPLayersFromPlayerList && hideFromTabList(player)) {
            this.entries = new ArrayList<>();
            return;
        }
        this.entries = entries;
    }
}