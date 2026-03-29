package xyz.nikitacartes.easyauth.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.PrepareSpawnTask;
import org.spongepowered.asm.mixin.*;
import xyz.nikitacartes.easyauth.interfaces.PrepareSpawnTaskInterface;
import xyz.nikitacartes.easyauth.utils.LastLocation;

@Mixin(PrepareSpawnTask.class)
public abstract class PrepareSpawnTaskMixin implements PrepareSpawnTaskInterface {

    @Unique
    private LastLocation easyAuth$spawnData = null;

    @Unique
    private boolean easyAuth$authenticated = false;

    @Final
    @Shadow
    PlayerConfigEntry player;

    @Final
    @Shadow
    MinecraftServer server;

    @Override
    public void easyAuth$setSpawnData(LastLocation spawnData) {
        this.easyAuth$spawnData = spawnData;
    }

    @Override
    public LastLocation easyAuth$getSpawnData() {
        return this.easyAuth$spawnData;
    }

    @Override
    public void easyAuth$setAuthenticated(boolean authenticated) {
        this.easyAuth$authenticated = authenticated;
    }

    @Override
    public boolean easyAuth$getAuthenticated() {
        return this.easyAuth$authenticated;
    }

    @Override
    public PlayerConfigEntry easyAuth$getPlayer() {
        return this.player;
    }

    @Override
    public MinecraftServer easyAuth$getServer() {
        return this.server;
    }
}
