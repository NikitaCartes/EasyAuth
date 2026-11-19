package xyz.nikitacartes.easyauth;

//? if >= 1.21.5 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
*///?}
import net.minecraft.gametest.framework.GameTestHelper;
//? if >= 1.21.9 {
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import xyz.nikitacartes.easyauth.interfaces.PlayerAuth;

import java.util.UUID;
//?}

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

    //? if >= 1.21.9 {
    // #292: the client of a dead player shows the death screen only after it gets the health
    @GameTest
    public void deadPlayerJoinSendsHealth(GameTestHelper context) {
        MinecraftServer server = context.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "dead-join"), false);
        ServerPlayer player = new ServerPlayer(server, context.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        player.setHealth(0.0F);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);

        boolean authenticated = ((PlayerAuth) player).easyAuth$isAuthenticated();
        boolean healthSent = channel.outboundMessages().stream()
                .anyMatch(packet -> packet instanceof ClientboundSetHealthPacket health && health.getHealth() == 0.0F);
        server.getPlayerList().remove(player);

        context.assertFalse(authenticated, Component.literal("Player is authenticated at join"));
        context.assertTrue(healthSent, Component.literal("Dead player got no health packet at join"));
        context.succeed();
    }
    //?}
}
