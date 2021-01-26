package org.samo_lego.simpleauth;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.samo_lego.simpleauth.commands.*;

@Mod(SimpleAuth.MOD_ID)
public class SimpleAuthForge {
    public SimpleAuthForge() {
        SimpleAuth.init(FMLPaths.GAMEDIR.get());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<ServerCommandSource> dispatcher = event.getDispatcher();

        RegisterCommand.registerCommand(dispatcher);
        LoginCommand.registerCommand(dispatcher);
        LogoutCommand.registerCommand(dispatcher);
        AccountCommand.registerCommand(dispatcher);
        AuthCommand.registerCommand(dispatcher);
    }


    @SubscribeEvent
    public void onStopServer(FMLServerStoppedEvent event) {
        SimpleAuth.stop();
    }
}
