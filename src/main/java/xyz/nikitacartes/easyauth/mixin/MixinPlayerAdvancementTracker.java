package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nikitacartes.easyauth.EasyAuth;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(PlayerAdvancementTracker.class)
public class MixinPlayerAdvancementTracker {

    @Mutable
    @Shadow
    @Final
    private File advancementFile;

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "load(Lnet/minecraft/server/ServerAdvancementLoader;)V",  at = @At("HEAD"))
    private void startMigratingOfflineAdvancements(ServerAdvancementLoader advancementLoader, CallbackInfo ci) {
        if(EasyAuth.config.main.premiumAutologin && !EasyAuth.config.experimental.forceoOfflineUuids && ((PlayerAuth) this.owner).isUsingMojangAccount() && !this.advancementFile.isFile()) {
            // Migrate
            String playername = owner.getGameProfile().getName();
            this.advancementFile = new File(this.advancementFile.getParent(), PlayerEntity.getOfflinePlayerUuid(playername).toString() + ".json");
        }
    }

    @Inject(method = "load(Lnet/minecraft/server/ServerAdvancementLoader;)V",  at = @At("TAIL"))
    private void endMigratingOfflineAdvancements(ServerAdvancementLoader advancementLoader, CallbackInfo ci) {
        if(EasyAuth.config.main.premiumAutologin && !EasyAuth.config.experimental.forceoOfflineUuids && ((PlayerAuth) this.owner).isUsingMojangAccount()) {
            // Changes the file name to use online UUID
            this.advancementFile = new File(this.advancementFile.getParent(), owner.getUuid() + ".json");
        }
    }
}
