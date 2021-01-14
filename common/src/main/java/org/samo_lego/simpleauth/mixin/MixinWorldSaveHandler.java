package org.samo_lego.simpleauth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.WorldSaveHandler;
import org.apache.logging.log4j.Logger;
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

import static org.samo_lego.simpleauth.SimpleAuth.config;
import static org.samo_lego.simpleauth.SimpleAuth.mojangAccountNamesCache;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

@Mixin(WorldSaveHandler.class)
public class MixinWorldSaveHandler {

    @Final
    @Shadow
    private File playerDataDir;

    @Unique
    private boolean fileExists;

    @Final
    @Shadow
    private static Logger LOGGER;

    /**
     * Saves whether player save file exists.
     *
     * @param playerEntity
     * @param cir
     * @param compoundTag
     * @param file
     */
    @Inject(
            method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fileExists(PlayerEntity playerEntity, CallbackInfoReturnable<CompoundTag> cir, CompoundTag compoundTag, File file) {
        // @ModifyVariable cannot capture locals
        this.fileExists = file.exists();
    }

    /**
     * Loads offline-uuid player data to compoundTag in order to migrate from offline to online.
     *
     * @param compoundTag null compound tag.
     * @param player player who might need migration of datd.
     * @return compoundTag containing migrated data.
     */
    @ModifyVariable(
            method = "loadPlayerData(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            )
    )
    private CompoundTag migratePlayerData(CompoundTag compoundTag, PlayerEntity player) {
        // Checking for offline player data only if online doesn't exist yet
        String playername = player.getGameProfile().getName().toLowerCase();
        if(config.main.premiumAutologin && mojangAccountNamesCache.contains(playername) && !this.fileExists) {
            if(config.experimental.debugMode)
                    logInfo("Migrating data for " + playername);
                File file = new File(this.playerDataDir, PlayerEntity.getOfflinePlayerUuid(player.getGameProfile().getName()) + ".dat");
            if (file.exists() && file.isFile())
                try {
                    compoundTag = NbtIo.readCompressed(new FileInputStream(file));
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to load player data for {}", playername);
                }
        }
        else if(config.experimental.debugMode)
            logInfo("Not migrating " +
                    playername +
                    ", as premium status is: " +
                    mojangAccountNamesCache.contains(playername) +
                    " and data file is " + (this.fileExists ? "" : "not") +
                    " present."
            );
        return compoundTag;
    }
}
