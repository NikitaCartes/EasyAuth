package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Uuids;
import net.minecraft.world.WorldSaveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.EasyAuth.mojangAccountNamesCache;

@Mixin(WorldSaveHandler.class)
public class WorldSaveHandlerMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSaveHandlerMixin.class);

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
        if (config.main.premiumAutologin && mojangAccountNamesCache.contains(playername) && !this.fileExists) {
            LOGGER.debug("Migrating data for {}", playername);
            File file = new File(this.playerDataDir, Uuids.getOfflinePlayerUuid(player.getGameProfile().getName()) + ".dat");
            if (file.exists() && file.isFile())
                try {
                    compoundTag = NbtIo.readCompressed(new FileInputStream(file));
                } catch (IOException e) {
                    LOGGER.warn("Failed to load player data for: {}", playername);
                }
        } else {
            LOGGER.debug(
                    "Not migrating {}, as premium status is '{}' and data file is {} present.",
                    playername, mojangAccountNamesCache.contains(playername), this.fileExists ? "" : "not");
        }
        return compoundTag;
    }
}
