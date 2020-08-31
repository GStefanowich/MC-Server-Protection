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

import net.TheElm.project.CoreMod;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.utilities.BlockUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.OreBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BlockEvents {
    private BlockEvents() {}
    
    /*
     * Tree Capacitator
     */
    public static ActionResult eventTreeCapacitator(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace) {
        if (!(ChunkUtils.isSetting(ClaimSettings.TREE_CAPACITATE, world, blockPos) && BlockUtils.isTreeBlock(world.getBlockState(blockPos)) && player.isSneaking()))
            return ActionResult.PASS;
        
        CoreMod.logInfo("Broke a tree block.");
        
        return ActionResult.SUCCESS;
    }
    
    /*
     * Vein Miner
     */
    public static ActionResult eventVeinMiner(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace) {
        BlockState blockState = world.getBlockState( blockPos );
        Block block = blockState.getBlock();
        
        if (!(ChunkUtils.isSetting(ClaimSettings.VEIN_MINER, world, blockPos) && block instanceof OreBlock && player.isSneaking()))
            return ActionResult.PASS;
        
        Set<BlockPos> ores = BlockEvents.gatherOreVein(block, world, blockPos);
        CoreMod.logInfo("Broke an ore block, got " + ores.size() + " others");
        for (BlockPos pos : ores) {
            if (BlockBreak.canBlockBreak(player, world, hand, pos, blockFace, null ) != ActionResult.FAIL)
                world.breakBlock( pos, true, player );
        }
        
        return ActionResult.SUCCESS;
    }
    private static Set<BlockPos> gatherOreVein(@NotNull final Block block, @NotNull final ServerWorld world, @NotNull final BlockPos originPos) {
        Set<BlockPos> set = new HashSet<>();
        
        // Get all of the adjacent ores
        BlockEvents.gatherOreVein(block, world, originPos, set);
        
        return set;
    }
    private static void gatherOreVein(@NotNull final Block block, @NotNull final ServerWorld world, @NotNull final BlockPos originPos, @NotNull Set<BlockPos> set, Direction... directions) {
        // For all possible directions
        for (Direction direction : Direction.values()) {
            Set<Direction> directionSet = new HashSet<>(Arrays.asList( directions ));
            if (directionSet.contains( direction ))
                continue;
            
            BlockPos searchPos = originPos.offset( direction );
            BlockState state = world.getBlockState( searchPos );
            
            // If ore type is equal
            if (state.getBlock().equals( block )) {
                boolean added = true;
                
                // Add the currently location to the list
                added = set.add( searchPos );
                
                // Get more adjacent ore
                directionSet.add( direction.getOpposite() );
                
                // If position was added to the set (Not already in the set)
                if ( added ) BlockEvents.gatherOreVein(
                    block,
                    world,
                    searchPos,
                    set,
                    directionSet.toArray(new Direction[0])
                );
            }
        }
    }
}
