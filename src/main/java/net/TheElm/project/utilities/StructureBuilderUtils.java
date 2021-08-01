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
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class StructureBuilderUtils {
    public static final StructureBuilderMaterial DEFAULT_OVERWORLD = new StructureBuilderMaterial(Blocks.CHISELED_STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.REDSTONE_LAMP, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final StructureBuilderMaterial DEFAULT_NETHER = new StructureBuilderMaterial(Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.SHROOMLIGHT);
    public static final StructureBuilderMaterial DESERT = new StructureBuilderMaterial(Blocks.SMOOTH_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.REDSTONE_LAMP, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final StructureBuilderMaterial BEACH = new StructureBuilderMaterial(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final StructureBuilderMaterial END = new StructureBuilderMaterial(Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.PURPUR_PILLAR, Blocks.STONE_PRESSURE_PLATE, Blocks.END_ROD);
    
    private final String name;
    private final World world;
    private final long delay = 50L;
    
    private HashMap<BlockPos, BlockState> structureBlocks = new LinkedHashMap<>();
    private HashMap<BlockPos, Supplier<BlockEntity>> structureEntity = new LinkedHashMap<>();
    
    public StructureBuilderUtils(@NotNull World world, @NotNull String structureName) {
        this.world = world;
        this.name = structureName;
        CoreMod.logInfo("Building new " + structureName);
    }
    
    public String getName() {
        return this.name;
    }
    
    public void addBlock(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        this.structureBlocks.put(blockPos, blockState);
    }
    public void addEntity(@NotNull BlockPos blockPos, @NotNull Supplier<BlockEntity> blockEntity) {
        this.structureEntity.put( blockPos, blockEntity );
    }
    
    public void destroy(boolean dropBlocks) throws InterruptedException {
        for (Map.Entry<BlockPos, BlockState> pair : this.structureBlocks.entrySet()) {
            BlockPos blockPos = pair.getKey();
            BlockState newBlockState = pair.getValue();
            BlockState oldBlockState = this.world.getBlockState(blockPos);
            
            BlockEntity blockEntity = dropBlocks && oldBlockState.getBlock().hasBlockEntity() ? this.world.getBlockEntity(blockPos) : null;
            
            // Change the block state
            if (newBlockState.getBlock() == Blocks.AIR)
                this.world.breakBlock(blockPos, dropBlocks);
            else {
                if (!(oldBlockState.getBlock() instanceof AbstractFireBlock))
                    this.world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(oldBlockState));
                
                this.world.setBlockState(blockPos, Blocks.BARRIER.getDefaultState());
                
                // If the old BlockEntity has any drops
                if (blockEntity != null)
                    Block.dropStacks(oldBlockState, this.world, blockPos, blockEntity, null, ItemStack.EMPTY);
            }
            
            Thread.sleep(this.delay);
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
    public <T extends ParticleEffect> void particlesSounds(T particle, SoundEvent sound, double deltaX, double deltaY, double deltaZ, double speed, int count, @NotNull BlockPos... blockPositions) throws InterruptedException {
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
            world.playSound(null, blockPos, sound, SoundCategory.MASTER, 1.0f, 1.0f);
            
            // Sleep
            Thread.sleep(this.delay * 10);
        }
    }
    
    public @NotNull StructureBuilderMaterial forBiome(@NotNull RegistryKey<World> dimension, @NotNull RegistryKey<Biome> biome) {
        Identifier identifier = biome.getValue();
        String path = identifier.getPath();
        
        if (dimension.equals(World.OVERWORLD)) {
            if (path.contains("desert"))
                return DESERT;
            /*if (path.contains("snow"))
                return COLD;*/
            if (path.contains("ocean") || path.contains("beach"))
                return BEACH;
        }
        else if (dimension.equals(World.NETHER)) {
            
            return DEFAULT_NETHER;
        }
        else if (dimension.equals(World.END)) {
            return END;
        }
        return DEFAULT_OVERWORLD;
    }
    
    public static class StructureBuilderMaterial {
        private final @NotNull Block mainBlock;
        private final @NotNull Block decoratingBlock;
        private final @NotNull Block pressurePlateBlock;
        
        private final @NotNull Block lightingBlock;
        private final @Nullable Block coveringBlock;
        private final @Nullable Block supportBlock;
        
        private final @NotNull Block structureBlock;
        
        private StructureBuilderMaterial(@NotNull Block main, @NotNull Block decorating, @NotNull Block lightSource) {
            this(main, decorating, lightSource, null);
        }
        private StructureBuilderMaterial(@NotNull Block main, @NotNull Block decorating, @NotNull Block lightSource, @Nullable Block plate) {
            this(main, decorating, lightSource, plate, null);
        }
        private StructureBuilderMaterial(@NotNull Block main, @NotNull Block decorating, @NotNull Block lightSource, @Nullable Block plate, @Nullable Block coveringBlock) {
            this(main, decorating, lightSource, plate, coveringBlock, lightSource.equals(Blocks.REDSTONE_LAMP) ? Blocks.REDSTONE_BLOCK : null);
        }
        private StructureBuilderMaterial(@NotNull Block main, @NotNull Block decorating, @NotNull Block lightSource, @Nullable Block plate, @Nullable Block coveringBlock, @Nullable Block supportingBlock) {
            this.mainBlock = main;
            this.decoratingBlock = decorating;
            this.lightingBlock = lightSource;
            this.pressurePlateBlock = (plate instanceof PressurePlateBlock ? plate : Blocks.STONE_PRESSURE_PLATE);
            this.coveringBlock = coveringBlock;
            this.supportBlock = supportingBlock;
            this.structureBlock = Blocks.BEDROCK;
        }
        
        public @NotNull BlockState getMainBlock() {
            return this.mainBlock.getDefaultState();
        }
        public @NotNull BlockState getDecoratingBlock() {
            return this.decoratingBlock.getDefaultState();
        }
        public @NotNull BlockState getPressurePlateBlock() {
            return this.pressurePlateBlock.getDefaultState();
        }
        public @NotNull BlockState getLightSourceBlock() {
            return this.lightingBlock.getDefaultState();
        }
        public @Nullable BlockState getCoveringBlock() {
            return this.coveringBlock == null ? null : this.coveringBlock.getDefaultState();
        }
        public @Nullable BlockState getSupportingBlock() {
            return this.supportBlock == null ? null : this.supportBlock.getDefaultState();
        }
        public @NotNull BlockState getStructureBlock() {
            return this.structureBlock.getDefaultState();
        }
        public @NotNull BlockState getAirBlock(@NotNull RegistryKey<World> world) {
            if (world.equals(World.END))
                return Blocks.VOID_AIR.getDefaultState();
            if (world.equals(World.NETHER))
                return Blocks.CAVE_AIR.getDefaultState();
            return Blocks.AIR.getDefaultState();
        }
    }
}
