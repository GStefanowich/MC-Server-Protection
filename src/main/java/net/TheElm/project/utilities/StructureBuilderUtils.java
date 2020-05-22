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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class StructureBuilderUtils {
    
    private final String name;
    private final World world;
    private final long delay = 50L;
    
    private HashMap<BlockPos, BlockState> structureBlocks = new LinkedHashMap<>();
    private HashMap<BlockPos, Supplier<BlockEntity>> structureEntity = new LinkedHashMap<>();
    
    public StructureBuilderUtils(World world, String structureName) {
        this.world = world;
        this.name = structureName;
        CoreMod.logInfo( "Building new " + structureName );
    }
    
    public String getName() {
        return this.name;
    }
    
    public void addBlock(BlockPos blockPos, BlockState blockState) {
        this.structureBlocks.put( blockPos, blockState );
    }
    public void addEntity(BlockPos blockPos, Supplier<BlockEntity> blockEntity) {
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
        for (Map.Entry<BlockPos, BlockState> iterator : this.structureBlocks.entrySet()) {
            BlockPos blockPos = iterator.getKey();
            BlockState block = iterator.getValue();
            
            // If block is already the same
            if (block.getBlock() == this.world.getBlockState(blockPos).getBlock())
                continue;
            
            this.world.setBlockState( blockPos, block );
            this.world.playSound(null, blockPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            Thread.sleep( this.delay );
        }
        
        // Update the block entities
        for (Map.Entry<BlockPos, Supplier<BlockEntity>> iterator : this.structureEntity.entrySet()) {
            BlockPos blockPos = iterator.getKey();
            BlockEntity entity = iterator.getValue().get();
            
            // Get the chunk directly (Won't update properly otherwise)
            this.world.getWorldChunk( blockPos ) // Update the block entity
                .setBlockEntity( blockPos, entity );
        }
    }
    public <T extends ParticleEffect> void particlesSounds(T particle, SoundEvent sound, double deltaX, double deltaY, double deltaZ, double speed, int count, BlockPos... blockPositions) throws InterruptedException {
        for (BlockPos blockPos : blockPositions) {
            // Spawn the particles
            ((ServerWorld) this.world).spawnParticles(
                particle,
                blockPos.getX() + 0.5,
                blockPos.getY(),
                blockPos.getZ() + 0.5,
                count,
                deltaX,
                deltaY,
                deltaZ,
                speed
            );
            
            // Play the sound effect
            world.playSound( null, blockPos, sound, SoundCategory.MASTER, 1.0f, 1.0f );
            
            // Sleep
            Thread.sleep(this.delay * 10);
        }
    }
    
}
