package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.Uuids;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

@Mixin(PlayerSaveHandler.class)
public class PlayerSaveHandlerMixin {
    @Final
    @Shadow
    private File playerDataDir;

    /**
     * Loads offline-uuid player data to compoundTag in order to migrate from offline to online.
     *
     * @param cir
     * @param mixinFile
     */
    @Inject(
            method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;Ljava/lang/String;)Ljava/util/Optional;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void fileExists(PlayerEntity player, String extension, CallbackInfoReturnable<Optional<NbtCompound>> cir, File mixinFile) {
        if (!(mixinFile.exists() && mixinFile.isFile())) {
            String playername = player.getGameProfile().getName().toLowerCase(Locale.ENGLISH);
            if (Boolean.parseBoolean(serverProp.getProperty("online-mode")) && mojangAccountNamesCache.contains(playername)) {
                LogDebug(String.format("Migrating data for %s", playername));
                File file = new File(this.playerDataDir, Uuids.getOfflinePlayerUuid(player.getGameProfile().getName()) + extension);
                if (file.exists() && file.isFile()) try {
                    cir.setReturnValue(Optional.of(NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes())));
                } catch (IOException e) {
                    LogWarn(String.format("Failed to load player data for: %s", playername));
                }
            } else {
                LogDebug(
                        String.format("Not migrating %s, as premium status is '%s' and data file is %s present.",
                                playername, mojangAccountNamesCache.contains(playername), mixinFile.exists() && mixinFile.isFile() ? "" : "not")
                );
            }
        }
    }
}
