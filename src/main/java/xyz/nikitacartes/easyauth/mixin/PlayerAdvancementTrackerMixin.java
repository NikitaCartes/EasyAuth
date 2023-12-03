package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import xyz.nikitacartes.easyauth.utils.PlayerAuth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.serverProp;

import java.nio.file.Path;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {

    @Mutable
    @Shadow
    @Final
    private Path filePath;

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "load(Lnet/minecraft/server/ServerAdvancementLoader;)V", at = @At("HEAD"))
    private void startMigratingOfflineAdvancements(ServerAdvancementLoader advancementLoader, CallbackInfo ci) {
        if (Boolean.parseBoolean(serverProp.getProperty("online-mode")) && !config.experimental.forcedOfflineUuids && ((PlayerAuth) this.owner).easyAuth$isUsingMojangAccount() && !this.filePath.toFile().isFile()) {
            // Migrate
            String playername = owner.getGameProfile().getName();
            this.filePath = this.filePath.getParent().resolve(Uuids.getOfflinePlayerUuid(playername) + ".json");
        }
    }

    @Inject(method = "load(Lnet/minecraft/server/ServerAdvancementLoader;)V", at = @At("TAIL"))
    private void endMigratingOfflineAdvancements(ServerAdvancementLoader advancementLoader, CallbackInfo ci) {
        if (Boolean.parseBoolean(serverProp.getProperty("online-mode")) && !config.experimental.forcedOfflineUuids && ((PlayerAuth) this.owner).easyAuth$isUsingMojangAccount()) {
            // Changes the file name to use online UUID
            this.filePath = this.filePath.getParent().resolve(owner.getUuid() + ".json");
        }
    }
}
