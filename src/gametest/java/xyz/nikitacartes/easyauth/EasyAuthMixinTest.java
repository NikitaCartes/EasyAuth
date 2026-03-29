package xyz.nikitacartes.easyauth;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import org.spongepowered.asm.mixin.MixinEnvironment;

public class EasyAuthMixinTest {
@GameTest
    public void test(GameTestHelper context) {
        MixinEnvironment.getCurrentEnvironment().audit();
        context.succeed();
    }
}