package xyz.nikitacartes.easyauth;

//? if >= 1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
*///?}
import net.minecraft.gametest.framework.GameTestHelper;

import org.spongepowered.asm.mixin.MixinEnvironment;

//? if >= 1.21.5 {
public class EasyAuthMixinTest {
@GameTest
//?} else {
/*public class EasyAuthMixinTest implements FabricGameTest {
@GameTest(template = EMPTY_STRUCTURE)
*///?}
    public void test(GameTestHelper context) {
        MixinEnvironment.getCurrentEnvironment().audit();
        context.succeed();
    }
}