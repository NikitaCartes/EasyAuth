package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.util.Uuids;
import net.minecraft.world.WorldSaveHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

@Mixin(WorldSaveHandler.class)
public class WorldSaveHandlerMixin {
    @Final
    @Shadow
    private File playerDataDir;

    @Unique
    private boolean fileExists;

    /**
     * Saves whether player save file exists.
     *
     * @param playerEntity
     * @param cir
     * @param compoundTag
     * @param file
     */
    @Inject(
            method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/nbt/NbtCompound;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fileExists(PlayerEntity playerEntity, CallbackInfoReturnable<NbtCompound> cir, NbtCompound compoundTag, File file) {
        // @ModifyVariable cannot capture locals
        this.fileExists = file.exists();
    }

    /**
     * Loads offline-uuid player data to compoundTag in order to migrate from offline to online.
     *
     * @param compoundTag null compound tag.
     * @param player      player who might need migration of data.
     * @return compoundTag containing migrated data.
     */
    @ModifyVariable(
            method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/nbt/NbtCompound;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            )
    )
    private NbtCompound migratePlayerData(NbtCompound compoundTag, PlayerEntity player) {
        // Checking for offline player data only if online doesn't exist yet
        String playername = player.getGameProfile().getName().toLowerCase();
        if (Boolean.parseBoolean(serverProp.getProperty("online-mode")) && mojangAccountNamesCache.contains(playername) && !this.fileExists) {
            LogDebug(String.format("Migrating data for %s", playername));
            File file = new File(this.playerDataDir, Uuids.getOfflinePlayerUuid(player.getGameProfile().getName()) + ".dat");
            if (file.exists() && file.isFile())
                try {
                    compoundTag = NbtIo.readCompressed(new FileInputStream(file), NbtTagSizeTracker.ofUnlimitedBytes());
                } catch (IOException e) {
                    LogWarn(String.format("Failed to load player data for: %s", playername));
                }
        } else {
            LogDebug(
                    String.format("Not migrating %s, as premium status is '%s' and data file is %s present.",
                            playername, mojangAccountNamesCache.contains(playername), this.fileExists ? "" : "not")
            );
        }
        return compoundTag;
    }
}
