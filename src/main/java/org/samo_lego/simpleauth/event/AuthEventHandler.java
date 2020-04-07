package org.samo_lego.simpleauth.event;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.samo_lego.simpleauth.SimpleAuth;

import static net.minecraft.block.NetherPortalBlock.AXIS;
import static net.minecraft.util.math.Direction.Axis.Z;

/**
 * This class will take care of actions players try to do,
 * and cancel them if they aren't authenticated
 */
public class AuthEventHandler {
    private static Text notAuthenticated() {
        if(SimpleAuth.config.main.enableGlobalPassword) {
            return new LiteralText(SimpleAuth.config.lang.loginRequired);
        }
        return new LiteralText(SimpleAuth.config.lang.notAuthenticated);
    }

    private static Text successfulPortalRescue = new LiteralText(SimpleAuth.config.lang.successfulPortalRescue);

    // Player joining the server
    public static void onPlayerJoin(ServerPlayerEntity player) {
        SimpleAuth.deauthenticatePlayer(player);

        // Tries to rescue player from nether portal
        if(SimpleAuth.config.main.tryPortalRescue && player.getBlockState().getBlock().equals(Blocks.NETHER_PORTAL)) {
            boolean wasSuccessful = false;

            BlockState portalState = player.getBlockState();
            World world = player.getEntityWorld();

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            if(portalState.get(AXIS) == Z) {
                // Player should be put to eastern or western block
                if( // Checking towards east
                        world.getBlockState(new BlockPos(x + 1, y, z)).isAir() &&
                        world.getBlockState(new BlockPos(x + 1, y + 1, z)).isAir() &&
                        (
                            world.getBlockState(new BlockPos(x + 1, y - 1, z)).isOpaque() ||
                            world.getBlockState(new BlockPos(x + 1, y - 1, z)).hasSolidTopSurface(world, new BlockPos(x + 1, y - 1, z), player)
                        )
                ) {
                    x++; // Towards east
                    wasSuccessful = true;
                }

                else if( // Checking towards south
                        world.getBlockState(new BlockPos(x - 1, y, z)).isAir() &&
                        world.getBlockState(new BlockPos(x - 1, y + 1, z)).isAir() &&
                        (
                            world.getBlockState(new BlockPos(x - 1, y - 1, z)).isOpaque() ||
                            world.getBlockState(new BlockPos(x - 1, y - 1, z)).hasSolidTopSurface(world, new BlockPos(x - 1, y - 1, z), player)
                        )
                ) {
                    x--; // Towards south
                    wasSuccessful = true;
                }
            }
            else {
                // Player should be put to northern or southern block
                if( // Checking towards south
                        world.getBlockState(new BlockPos(x, y, z + 1)).isAir() &&
                        world.getBlockState(new BlockPos(x, y + 1, z + 1)).isAir() &&
                        (
                            world.getBlockState(new BlockPos(x, y - 1, z + 1)).isOpaque() ||
                            world.getBlockState(new BlockPos(x, y - 1, z + 1)).hasSolidTopSurface(world, new BlockPos(x, y - 1, z + 1), player)
                        )
                ) {
                    z++; // Towards south
                    wasSuccessful = true;
                }

                else if( // Checking towards north
                        world.getBlockState(new BlockPos(x, y, z - 1)).isAir() &&
                        world.getBlockState(new BlockPos(x, y + 1, z - 1)).isAir() &&
                        (
                            world.getBlockState(new BlockPos(x, y - 1, z - 1)).isOpaque() ||
                            world.getBlockState(new BlockPos(x, y - 1, z - 1)).hasSolidTopSurface(world, new BlockPos(x, y - 1, z - 1), player)
                        )
                ) {
                    z--; // Towards north
                    wasSuccessful = true;
                }
            }
            if(wasSuccessful) {
                player.teleport(x, y, z);
                player.sendMessage(successfulPortalRescue);
            }
        }
    }

    // Player chatting
    public static ActionResult onPlayerChat(PlayerEntity player, ChatMessageC2SPacket chatMessageC2SPacket) {
        // Getting the message to then be able to check it
        String msg = chatMessageC2SPacket.getChatMessage();
        if(
            !SimpleAuth.isAuthenticated((ServerPlayerEntity) player) &&
            !msg.startsWith("/login") &&
            !msg.startsWith("/register") &&
            (!SimpleAuth.config.main.allowChat || msg.startsWith("/"))
        ) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Player movement
    public static ActionResult onPlayerMove(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowMovement) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using a block (right-click function)
    public static ActionResult onUseBlock(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowBlockUse) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Punching a block
    public static ActionResult onAttackBlock(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowBlockPunch) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    // Using an item
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowItemUse) {
            player.sendMessage(notAuthenticated());
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        return TypedActionResult.pass(ItemStack.EMPTY);
    }
    // Dropping an item
    public static ActionResult onDropItem(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowItemDrop) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    // Changing inventory (item moving etc.)
    public static ActionResult onTakeItem(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowItemMoving) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Attacking an entity
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowEntityPunch) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
    // Interacting with entity
    public static ActionResult onUseEntity(PlayerEntity player) {
        if(!SimpleAuth.isAuthenticated((ServerPlayerEntity) player) && !SimpleAuth.config.main.allowEntityInteract) {
            player.sendMessage(notAuthenticated());
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}