package xyz.nikitacartes.easyauth.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nikitacartes.easyauth.event.AuthEventHandler;

import static xyz.nikitacartes.easyauth.EasyAuth.langConfig;

@Mixin(Commands.class)
public class CommandsMixin {
    //? if >= 1.20.3 {
    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void checkCanUseCommands(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
    *///?}
        InteractionResult result = AuthEventHandler.onPlayerCommand(parseResults.getContext().getSource().getPlayer(), command);
        if (result == InteractionResult.FAIL) {
            langConfig.session.loginRequired.send(parseResults.getContext().getSource());
            //? if >= 1.20.3 {
            ci.cancel();
            //?} else {
            /*cir.setReturnValue(1);
            *///?}
        }
    }
}