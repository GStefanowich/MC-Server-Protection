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

package net.TheElm.project.utilities;

import net.TheElm.project.CoreMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructureBuilderUtils {
    
    private final World world;
    private final long delay = 50L;
    
    private HashMap<BlockPos, BlockState> structureBlocks = new LinkedHashMap<>();
    private HashMap<BlockPos, BlockEntity> structureEntity = new LinkedHashMap<>();
    
    public StructureBuilderUtils(World world, String structureName) {
        this.world = world;
        CoreMod.logMessage( "Building new " + structureName );
    }
    
    public void addBlock(BlockPos blockPos, BlockState blockState) {
        this.structureBlocks.put( blockPos, blockState );
    }
    public void addEntity(BlockPos blockPos, BlockEntity blockEntity) {
        this.structureEntity.put( blockPos, blockEntity );
    }
    
    public void destroy(boolean dropBlocks) throws InterruptedException {
        for (BlockPos blockPos : this.structureBlocks.keySet()) {
            this.world.breakBlock(blockPos, dropBlocks);
            Thread.sleep( this.delay );
        }
    }
    public void build() throws InterruptedException {
        // Place all of the blocks
        for (Map.Entry<BlockPos, BlockState> blockEntry : this.structureBlocks.entrySet()) {
            BlockPos blockPos = blockEntry.getKey();
            BlockState block = blockEntry.getValue();
            
            if (block.getBlock() == Blocks.AIR)
                continue;
            
            this.world.setBlockState( blockPos, block );
            Thread.sleep( this.delay );
        }
        
        // Update the block entities
        for (Map.Entry<BlockPos, BlockEntity> blockEntry : this.structureEntity.entrySet()) {
            this.world.setBlockEntity(
                blockEntry.getKey(),
                blockEntry.getValue()
            );
        }
    }
    
}
