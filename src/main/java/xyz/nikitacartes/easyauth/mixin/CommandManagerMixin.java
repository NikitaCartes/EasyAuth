package xyz.nikitacartes.easyauth.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) {
        ServerPlayerEntity player = null;
        try {
            player = source.getPlayer();
        } catch (CommandSyntaxException ignored) {
        }
        ActionResult result = AuthEventHandler.onPlayerCommand(player, command);
        if (result == ActionResult.FAIL) {
            langConfig.loginRequired.send(player);
            cir.setReturnValue(1);
        }
    }
}