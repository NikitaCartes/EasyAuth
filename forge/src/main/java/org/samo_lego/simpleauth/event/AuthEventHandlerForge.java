package org.samo_lego.simpleauth.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.samo_lego.simpleauth.utils.PlayerAuth;

import static net.minecraftforge.eventbus.api.EventPriority.HIGHEST;
import static org.samo_lego.simpleauth.SimpleAuth.MOD_ID;
import static org.samo_lego.simpleauth.SimpleAuth.config;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AuthEventHandlerForge {
    @SubscribeEvent(priority = HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onUseBlock(player).equals(ActionResult.FAIL)) {
            event.setCanceled(true);
        }
    }

    // Using a block (right-click function)
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onUseBlock(player).equals(ActionResult.FAIL)) {
            event.setCanceled(true);
        }
    }

    // Punching a block
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackBlock(PlayerInteractEvent.LeftClickBlock event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onBreakBlock(player)) {
            event.setCanceled(true);
        }
    }

    // Using an item
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onUseItem(player).getResult().equals(ActionResult.FAIL)) {
            event.setCanceled(true);
        }
    }

    // Attacking an entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onAttackEntity(player).equals(ActionResult.FAIL)) {
            event.setCanceled(true);
        }
    }

    // Interacting with entity
    @SubscribeEvent(priority = HIGHEST)
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        PlayerEntity player = event.getPlayer();
        if(AuthEventHandler.onUseEntity(player).equals(ActionResult.FAIL)) {
            event.setCanceled(true);
        }
    }
}