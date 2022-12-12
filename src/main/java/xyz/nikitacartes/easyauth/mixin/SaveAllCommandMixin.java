package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.SaveAllCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static xyz.nikitacartes.easyauth.EasyAuth.*;

@Mixin(SaveAllCommand.class)
public class SaveAllCommandMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAllCommandMixin.class);

    @Inject(method = "saveAll(Lnet/minecraft/server/command/ServerCommandSource;Z)I", at = @At("HEAD"))
    private static void saveDB(ServerCommandSource source, boolean flush, CallbackInfoReturnable<Integer> cir) {
        THREADPOOL.submit(() -> {
            LOGGER.info("Saving database");
            DB.saveAll(playerCacheMap);
        });
    }
}