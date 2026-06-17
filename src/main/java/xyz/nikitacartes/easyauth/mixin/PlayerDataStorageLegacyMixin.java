//? if <1.21 {
/*package xyz.nikitacartes.easyauth.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
//? if >= 1.20.3 {
import net.minecraft.nbt.NbtAccounter;
//?}
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogDebug;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogWarn;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageLegacyMixin {
    @Final
    @Shadow
    private File playerDir;

    /^*
     * Loads offline-uuid player data to compoundTag in order to migrate from offline to online.
     *
     * @param cir
     * @param mixinFile
     ^/
    @Inject(
            //? if >= 1.20.5 {
            method = "load(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;)Ljava/util/Optional;",
            //?} else {
            /^method = "load(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/nbt/CompoundTag;",
            ^///?}
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;exists()Z"
            ),
            cancellable = true
    )
    //? if >= 1.20.5 {
    private void fileExists(Player player, String extension, CallbackInfoReturnable<Optional<CompoundTag>> cir, @Local File mixinFile) {
    //?} else {
    /^private void fileExists(Player player, CallbackInfoReturnable<CompoundTag> cir, @Local File mixinFile) {
    ^///?}
        if (!(mixinFile.exists() && mixinFile.isFile())) {
            String playername = player.getGameProfile().getName().toLowerCase(Locale.ENGLISH);
            PlayerAuth playerAuth = (PlayerAuth) player;
            if (Boolean.parseBoolean(serverProp.getProperty("online-mode")) && playerAuth.easyAuth$isUsingMojangAccount()) {
                LogDebug(String.format("Migrating data for %s", playername));
                //? if >= 1.20.5 {
                File file = new File(this.playerDir, UUIDUtil.createOfflinePlayerUUID(player.getGameProfile().getName()) + extension);
                if (file.exists() && file.isFile()) try {
                    cir.setReturnValue(Optional.of(NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap())));
                } catch (IOException e) {
                    LogWarn(String.format("Failed to load player data for: %s", playername));
                }
                //?} else {
                /^File file = new File(this.playerDir, UUIDUtil.createOfflinePlayerUUID(player.getGameProfile().getName()) + ".dat");
                if (file.exists() && file.isFile()) {
                    try {
                        //? if >= 1.20.3 {
                        cir.setReturnValue(NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap()));
                        //?} else {
                        /^¹cir.setReturnValue(NbtIo.readCompressed(file));
                        ¹^///?}
                    } catch (IOException e) {
                        LogWarn(String.format("Failed to load player data for: %s", playername));
                    }
                }
                ^///?}
            } else {
                LogDebug(
                        String.format("Not migrating %s, as premium status is '%s' and data file is %s present.",
                                playername, playerAuth.easyAuth$isUsingMojangAccount(), mixinFile.exists() && mixinFile.isFile() ? "" : "not")
                );
            }
        }
    }
}
*///?}
