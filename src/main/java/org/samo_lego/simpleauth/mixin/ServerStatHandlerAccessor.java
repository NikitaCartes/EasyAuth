package org.samo_lego.simpleauth.mixin;

import net.minecraft.stat.ServerStatHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(ServerStatHandler.class)
public interface ServerStatHandlerAccessor {

    @Accessor("file")
    File getFile();

    @Accessor("file")
    void setFile(File file);
}
