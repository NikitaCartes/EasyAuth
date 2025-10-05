package xyz.nikitacartes.easyauth;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.test.TestContext;

import org.spongepowered.asm.mixin.MixinEnvironment;

public class EasyAuthMixinTest {
@GameTest
    public void test(TestContext context) {
        MixinEnvironment.getCurrentEnvironment().audit();
        context.complete();
    }
}