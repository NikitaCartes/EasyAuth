package org.samo_lego.simpleauth;

import com.mojang.brigadier.CommandDispatcher;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.samo_lego.simpleauth.commands.*;

@Mod(SimpleAuth.MOD_ID)
public class SimpleAuthForge {
    public SimpleAuthForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(SimpleAuth.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        SimpleAuth.init(FMLPaths.GAMEDIR.get());
    }

    // Registering the commands
    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<ServerCommandSource> dispatcher = event.getDispatcher();

        RegisterCommand.registerCommand(dispatcher);
        LoginCommand.registerCommand(dispatcher);
        LogoutCommand.registerCommand(dispatcher);
        AccountCommand.registerCommand(dispatcher);
        AuthCommand.registerCommand(dispatcher);
    }

    /**
     * Called on server stop.
     */
    @SubscribeEvent
    public void onStopServer(FMLServerStoppedEvent event) {
        SimpleAuth.stop();
    }
}
