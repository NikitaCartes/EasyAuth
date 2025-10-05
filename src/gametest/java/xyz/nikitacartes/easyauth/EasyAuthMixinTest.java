package xyz.nikitacartes.easyauth;

//? if >= 1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.test.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
*///?}
import net.minecraft.test.TestContext;

import org.spongepowered.asm.mixin.MixinEnvironment;

//? if >= 1.21.5 {
public class EasyAuthMixinTest {
@GameTest
//?} else {
/*public class EasyAuthMixinTest implements FabricGameTest {
@GameTest(templateName = EMPTY_STRUCTURE)
*///?}
    public void test(TestContext context) {
        MixinEnvironment.getCurrentEnvironment().audit();
        context.complete();
    }
}