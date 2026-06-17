//? if >=1.21 {
package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
//? if >= 1.21.9 {
import net.minecraft.server.players.NameAndId;
//?}
//? if < 1.21.9 {
/*import net.minecraft.world.entity.player.Player;
*///?}
import net.minecraft.world.level.storage.PlayerDataStorage;
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
import static xyz.nikitacartes.easyauth.utils.StoneCutterUtils.getName;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {
    @Final
    @Shadow
    public File playerDir;

    /**
     * Loads offline-uuid player data to compoundTag in order to migrate from offline to online.
     *
     * @param cir
     * @param mixinFile
     */
    //? if >= 1.21.9 {
    @Inject(
            method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            cancellable = true
    )
    private void fileExists(NameAndId player, String extension, CallbackInfoReturnable<Optional<CompoundTag>> cir, @Local File mixinFile) {
    //?} else {
    /*@Inject(
            method = "load(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)Ljava/util/Optional;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            cancellable = true
    )
        private void fileExists(Player player, String extension, CallbackInfoReturnable<Optional<CompoundTag>> cir, @Local File mixinFile) {
    *///?}
        if (!(mixinFile.exists() && mixinFile.isFile())) {
            String playerName = getName(player);
            if (Boolean.parseBoolean(serverProp.getProperty("online-mode"))) {
                LogDebug(String.format("Migrating data for %s", playerName));
                File file = new File(this.playerDir, UUIDUtil.createOfflinePlayerUUID(playerName) + extension);
                if (file.exists() && file.isFile()) try {
                    cir.setReturnValue(Optional.of(NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap())));
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
//?}