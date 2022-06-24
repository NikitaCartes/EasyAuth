package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) {
        ActionResult result = AuthEventHandler.onPlayerCommand(source.getPlayer(), command);
        if (result == ActionResult.FAIL) {
            cir.setReturnValue(1);
        }
        return;
    }
}