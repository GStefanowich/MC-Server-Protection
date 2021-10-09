/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.protections.events;

import net.TheElm.project.ServerCore;
import net.TheElm.project.interfaces.ItemUseCallback;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.BlockUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemUse {
    
    private static final int BLOCK_UPDATE_ROTATION_FLAG = 16;
    private static final int BLOCK_UPDATE_MAX_DEPTH = 512;
    
    private ItemUse() {}
    
    /**
     * Initialize our callback listener for Item Usage
     */
    public static void init() {
        ItemUseCallback.EVENT.register(ItemUse::blockInteract);
    }
    
    private static @NotNull ActionResult blockInteract(@NotNull ServerPlayerEntity player, @NotNull World world, @NotNull Hand hand, @NotNull ItemStack itemStack) {
        if (itemStack.getItem() == Items.COMPASS) {
            Pair<Text, BlockPos> newDirection = ((PlayerData) player).cycleCompass();
            TitleUtils.showPlayerAlert(player, Formatting.YELLOW, new LiteralText("Compass now pointing towards "), newDirection.getLeft());
            
            return ActionResult.SUCCESS;
        } else if (itemStack.getItem() == Items.STICK) {
            /*
             * If item is stick and player has build permission
             */
            BlockHitResult hitResult = BlockUtils.getLookingBlock(world, player, 10, RaycastContext.FluidHandling.NONE);
            if ((hitResult.getType() != HitResult.Type.MISS) && ChunkUtils.canPlayerBreakInChunk(player, hitResult.getBlockPos()) && ItemUse.stickBlockInteraction(player, world, hitResult))
                return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }
    private static boolean stickBlockInteraction(@NotNull ServerPlayerEntity player, @NotNull World world, @NotNull BlockHitResult hitResult) {
        Direction rotation = null;
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        
        // If a HorizontalFacingBlock can be rotated
        boolean canBeRotated = state.contains(HorizontalFacingBlock.FACING)
            && (!((block instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) || (block instanceof BedBlock)))
            && ((rotation = findNewRotation(world, pos, block, state)) != null);
        
        if ((!player.isSneaking()) && canBeRotated) {
            /*
             * Rotate blocks
             */
            ItemUse.syncedBlockStateChange(world, pos, state.with(HorizontalFacingBlock.FACING, rotation));
            
            return true;
        } else if (player.isSneaking() && state.contains(PillarBlock.AXIS)) {
            /*
             * Change block axis
             */
            ItemUse.syncedBlockStateChange(world, pos, ((PillarBlock) block).rotate(state, BlockRotation.CLOCKWISE_90));
            
            return true;
        } else if (block instanceof StairsBlock) {
            /*
             * If block is a stairs block
             */
            StairShape shape = state.get(StairsBlock.SHAPE);
            ItemUse.syncedBlockStateChange(world, pos, state.with(StairsBlock.SHAPE, ItemUse.rotateStairShape(shape)));
            
            return true;
        } else if (block instanceof DoorBlock) {
            /*
             * Switch door hinges
             */
            DoubleBlockHalf doorHalf = state.get(DoorBlock.HALF);
            
            BlockPos otherHalf = pos.offset(doorHalf == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
            
            // Change the doors hinge
            DoorHinge hinge = state.get(DoorBlock.HINGE) == DoorHinge.LEFT ? DoorHinge.RIGHT : DoorHinge.LEFT;
            
            ItemUse.syncedBlockStateChange(world, pos, state.with(DoorBlock.HINGE, hinge));
            ItemUse.syncedBlockStateChange(world, otherHalf, world.getBlockState(otherHalf).with(DoorBlock.HINGE, hinge));
            
            return true;
        } else if (player.isSneaking() && block instanceof HorizontalConnectingBlock) {
            /*
             * Change which sides of a connecting block are connected
             */
            if (hitResult.getSide() == Direction.UP || hitResult.getSide() == Direction.DOWN)
                return false;
            
            Vec3d vec3d = hitResult.getPos();
            Vec3d face = new Vec3d(vec3d.getX() - pos.getX(), vec3d.getY() - pos.getY(), vec3d.getZ() - pos.getZ());
            Direction dir = ItemUse.getConnectingFace(hitResult.getSide(), face);
            
            // Get the connecting property to toggle
            BooleanProperty property = ItemUse.getConnecting(dir);
            boolean isConnected = state.get(property);
            ItemUse.syncedBlockStateChange(world, pos, state.with(property, !isConnected));
            
            // Check the connected block to see if we need to de-couple it
            BlockPos otherPos = pos.offset(dir);
            BlockState other = world.getBlockState(otherPos);
            dir = dir.getOpposite();
            property = ItemUse.getConnecting(dir);
            if (other.getBlock() == block && other.get(property) == isConnected)
                ItemUse.syncedBlockStateChange(world, otherPos, other.with(property, !isConnected));
            
            return true;
        } else if (player.isSneaking() && canBeRotated) {
            /*
             * Catch for block rotating
             */
            ItemUse.syncedBlockStateChange(world, pos, state.with(HorizontalFacingBlock.FACING, rotation));
            
            return true;
        } else {
            /*BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof SignBlockEntity) {
                SignBlockEntity sign = (SignBlockEntity) entity;
                if (!player.isSneaking()) {
                    System.out.println("Hello!");
                } else if (block.isIn(BlockTags.STANDING_SIGNS)) {
                    System.out.println("Standing sign!");
                }
                
                sign.setEditor(player);
                player.networkHandler.sendPacket(new SignEditorOpenS2CPacket(pos));
            }*/
        }
        return false;
    }
    private static boolean syncedBlockStateChange(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state) {
        boolean rotated = world.setBlockState(pos, state, ItemUse.BLOCK_UPDATE_ROTATION_FLAG, ItemUse.BLOCK_UPDATE_MAX_DEPTH);
        if (rotated)
            ServerCore.markDirty(world, pos);
        return rotated;
    }
    
    private static @Nullable Direction findNewRotation(@NotNull WorldView world, @NotNull BlockPos blockPos, @NotNull Block block, @NotNull BlockState blockState) {
        Direction starting = blockState.get(HorizontalFacingBlock.FACING), rotation = starting.rotateYClockwise();
        do {
            if (block.canPlaceAt(blockState.with(HorizontalFacingBlock.FACING, rotation), world, blockPos))
                return rotation;
            
            rotation = rotation.rotateYClockwise();
        } while (rotation != starting);
        return null;
    }
    private static @NotNull StairShape rotateStairShape(@NotNull StairShape shape) {
        switch (shape) {
            case STRAIGHT:
                return StairShape.INNER_LEFT;
            case INNER_LEFT:
                return StairShape.INNER_RIGHT;
            case INNER_RIGHT:
                return StairShape.OUTER_LEFT;
            case OUTER_LEFT:
                return StairShape.OUTER_RIGHT;
            default:
                return StairShape.STRAIGHT;
        }
    }
    
    private static @NotNull Direction getConnectingFace(@NotNull Direction face, @NotNull Vec3d pos) {
        double faceSide = ItemUse.faceSide(face, pos);
        boolean left = faceSide <= 0.38;
        boolean right = faceSide >= 0.62;
        if (!left && !right)
            return face;
        return left && !right ? face.rotateYCounterclockwise() : face.rotateYClockwise();
    }
    private static @NotNull BooleanProperty getConnecting(@NotNull Direction face) {
        switch (face) {
            case UP: return Properties.UP;
            case DOWN: return Properties.DOWN;
            case NORTH: return Properties.NORTH;
            case SOUTH: return Properties.SOUTH;
            case WEST: return Properties.WEST;
            case EAST: return Properties.EAST;
        }
        return Properties.UP;
    }
    private static double faceSide(@NotNull Direction face, @NotNull Vec3d pos) {
        if (face == Direction.NORTH)
            return pos.getX();
        if (face == Direction.SOUTH)
            return 1 - pos.getX();
        if (face == Direction.EAST)
            return pos.getZ();
        if (face == Direction.WEST)
            return 1 - pos.getZ();
        return 0.5d;
    }
}
