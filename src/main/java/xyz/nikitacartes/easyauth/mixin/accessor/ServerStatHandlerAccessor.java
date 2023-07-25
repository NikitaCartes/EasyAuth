package xyz.nikitacartes.easyauth.mixin.accessor;

import net.minecraft.stat.ServerStatHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(ServerStatHandler.class)
public interface ServerStatHandlerAccessor {

    @Accessor("file")
    File getFile();

    @Accessor("file")
    @Mutable
    void setFile(File file);
}
