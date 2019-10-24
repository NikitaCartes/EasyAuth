package org.samo_lego.simpleauth.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class SimpleAuthMixin {
	@Inject(at = @At("HEAD"), method = "start()V")
	private void init(CallbackInfo info) {
		System.out.println("This line is printed by an example mod mixin!");
	}
}
