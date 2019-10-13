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

import net.TheElm.project.enums.CompassDirections;
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
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ViewableWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ItemUse {
    
    private ItemUse() {}
    
    /**
     * Initialize our callback listener for Item Usage
     */
    public static void init() {
        ItemUseCallback.EVENT.register(ItemUse::blockInteract);
    }
    
    private static ActionResult blockInteract(ServerPlayerEntity player, World world, Hand hand, ItemStack itemStack) {
        if (itemStack.getItem() == Items.COMPASS) {
            CompassDirections newDirection = ((PlayerData) player).cycleCompass();
            TitleUtils.showPlayerAlert( player, Formatting.YELLOW, new LiteralText("Compass now pointing towards "), newDirection.text() );
            
            return ActionResult.SUCCESS;
        } else if (itemStack.getItem() == Items.STICK) {
            /*
             * If item is stick and player has build permission
             */
            BlockHitResult blockHitResult = BlockUtils.getLookingBlock( world, player );
            BlockPos blockPos = blockHitResult.getBlockPos();
            
            if ((blockHitResult.getType() == HitResult.Type.MISS) || (!ChunkUtils.canPlayerBreakInChunk( player, blockPos )))
                return ActionResult.PASS;
    
            Direction rotation = null;
            BlockState blockState = world.getBlockState( blockPos );
            Block block = blockState.getBlock();
            
            // If a HorizontalFacingBlock can be rotated
            boolean canBeRotated = blockState.contains(HorizontalFacingBlock.FACING)
                && (!((block instanceof ChestBlock && blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) || (block instanceof BedBlock)))
                && ((rotation = findNewRotation(world, blockPos, block, blockState)) != null);
            
            if ((!player.isSneaking()) && canBeRotated) {
                /*
                 * Rotate blocks
                 */
                world.setBlockState(blockPos, blockState.with(HorizontalFacingBlock.FACING, rotation));
                
                return ActionResult.SUCCESS;
            } else if (player.isSneaking() && blockState.contains(PillarBlock.AXIS)) {
                /*
                 * Change block axis
                 */
                world.setBlockState(blockPos, ((PillarBlock) block).rotate( blockState, BlockRotation.CLOCKWISE_90 ));
                
                return ActionResult.SUCCESS;
            } else if (block instanceof StairsBlock) {
                /* 
                 * If block is a stairs block
                 */
                StairShape shape = blockState.get(StairsBlock.SHAPE);
                
                world.setBlockState( blockPos, blockState.with(StairsBlock.SHAPE, rotateStairShape(shape)));
                
                return ActionResult.SUCCESS;
            } else if (block instanceof DoorBlock) {
                /* 
                 * Switch door hinges
                 */
                DoubleBlockHalf doorHalf = blockState.get(DoorBlock.HALF);
                
                BlockPos otherHalf = blockPos.offset(doorHalf == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
                
                // Change the doors hinge
                DoorHinge hinge = blockState.get(DoorBlock.HINGE) == DoorHinge.LEFT ? DoorHinge.RIGHT : DoorHinge.LEFT;
                
                world.setBlockState( blockPos, blockState.with(DoorBlock.HINGE, hinge));
                world.setBlockState( otherHalf, world.getBlockState(otherHalf).with(DoorBlock.HINGE, hinge));
                
                return ActionResult.SUCCESS;
            } else if (player.isSneaking() && canBeRotated) {
                /*
                 * Catch for block rotating
                 */
                world.setBlockState(blockPos, blockState.with(HorizontalFacingBlock.FACING, rotation));
                
                return ActionResult.SUCCESS;
            }
        }
        
        return ActionResult.PASS;
    }
    @Nullable
    private static Direction findNewRotation(ViewableWorld world, BlockPos blockPos, Block block, BlockState blockState) {
        Direction starting = blockState.get(HorizontalFacingBlock.FACING), rotation = starting.rotateYClockwise();
        do {
            if (block.canPlaceAt(blockState.with(HorizontalFacingBlock.FACING, rotation), world, blockPos))
                return rotation;
            
            rotation = rotation.rotateYClockwise();
        } while (rotation != starting);
        return null;
    }
    private static StairShape rotateStairShape(StairShape shape) {
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
}
