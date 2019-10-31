package org.samo_lego.simpleauth.event.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;

public interface EntitySpawnCallback {

    Event<EntitySpawnCallback> EVENT = EventFactory.createArrayBacked(EntitySpawnCallback.class, listeners -> entity -> {
        for(EntitySpawnCallback callback : listeners) {
            if(callback.spawnEntity(entity)) {
                return true;
            }
        }
        return false;
    });

    /**
     * fired when an entity is spawned
     *
     * @return {@code true} to stop the entity from spawning
     */
    boolean spawnEntity(Entity entity);
}
