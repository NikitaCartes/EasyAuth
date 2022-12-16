package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.SaveAllCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static xyz.nikitacartes.easyauth.EasyAuth.*;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogInfo;

@Mixin(SaveAllCommand.class)
public class SaveAllCommandMixin {
    @Inject(method = "saveAll(Lnet/minecraft/server/command/ServerCommandSource;Z)I", at = @At("HEAD"))
    private static void saveDB(ServerCommandSource source, boolean flush, CallbackInfoReturnable<Integer> cir) {
        THREADPOOL.submit(() -> {
            LogInfo("Saving database");
            DB.saveAll(playerCacheMap);
        });
    }
}