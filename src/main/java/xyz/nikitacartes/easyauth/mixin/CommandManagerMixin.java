package xyz.nikitacartes.easyauth.mixin;

import com.mojang.brigadier.ParseResults;
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
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
        ActionResult result = AuthEventHandler.onPlayerCommand(parseResults.getContext().getSource().getPlayer(), command);
        if (result == ActionResult.FAIL) {
            cir.setReturnValue(1);
        }
    }
}