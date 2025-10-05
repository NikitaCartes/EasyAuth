package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
//? if >= 1.21.9 {
import net.minecraft.server.PlayerConfigEntry;
//?}
import net.minecraft.util.Uuids;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
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
            //? if >= 1.21.9 {
            method = "loadPlayerData(Lnet/minecraft/server/PlayerConfigEntry;Ljava/lang/String;)Ljava/util/Optional;",
            //?} else {
            /*method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;Ljava/lang/String;)Ljava/util/Optional;",
            *///?}
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            cancellable = true
    )
    //? if >= 1.21.9 {
    private void fileExists(PlayerConfigEntry playerConfigEntry, String extension, CallbackInfoReturnable<Optional<NbtCompound>> cir, @Local File mixinFile) {
    //?} else {
    /*private void fileExists(PlayerEntity player, String extension, CallbackInfoReturnable<Optional<NbtCompound>> cir, @Local File mixinFile) {
    *///?}
        if (!(mixinFile.exists() && mixinFile.isFile())) {
            //? if >= 1.21.9 {
            String playerName = playerConfigEntry.name();
            //?} else {
            /*String playerName = player.getGameProfile().getName();
            *///?}
            if (Boolean.parseBoolean(serverProp.getProperty("online-mode"))) {
                LogDebug(String.format("Migrating data for %s", playerName));
                File file = new File(this.playerDataDir, Uuids.getOfflinePlayerUuid(playerName) + extension);
                if (file.exists() && file.isFile()) try {
                    cir.setReturnValue(Optional.of(NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes())));
                } catch (IOException e) {
                    LogWarn(String.format("Failed to load player data for: %s", playerName));
                }
            } else {
                LogDebug(
                        String.format("Not migrating %s, data file is %s present.",
                                playerName, mixinFile.exists() && mixinFile.isFile() ? "" : "not")
                );
            }
        }
    }
}
