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

package net.theelm.sewingmachine.utilities;

import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Function;

public final class StructureBuilderUtils {
    public static final @NotNull StructureBuilderMaterial DEFAULT_OVERWORLD = new StructureBuilderMaterial(Blocks.CHISELED_STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.REDSTONE_LAMP, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final @NotNull StructureBuilderMaterial DEFAULT_NETHER = new StructureBuilderMaterial(Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.SHROOMLIGHT);
    public static final @NotNull StructureBuilderMaterial DEFAULT_END = new StructureBuilderMaterial(Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.PURPUR_PILLAR, Blocks.STONE_PRESSURE_PLATE, Blocks.END_ROD);
    public static final @NotNull StructureBuilderMaterial DESERT = new StructureBuilderMaterial(Blocks.SMOOTH_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.REDSTONE_LAMP, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final @NotNull StructureBuilderMaterial BEACH = new StructureBuilderMaterial(Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
    public static final @NotNull StructureBuilderMaterial COLD = new StructureBuilderMaterial(Blocks.SMOOTH_QUARTZ, Blocks.CALCITE, Blocks.REDSTONE_LAMP, Blocks.STONE_PRESSURE_PLATE);
    public static final @NotNull StructureBuilderMaterial DEEP_DARK = new StructureBuilderMaterial(Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.AMETHYST_BLOCK, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.LIGHT);
    
    private final @NotNull String name;
    public final @NotNull World world;
    public final @NotNull RegistryKey<Biome> biome;
    public final @NotNull StructureBuilderMaterial material;
    private final int delay = 1;
    
    private @NotNull final Queue<BlockPos> locations = new ArrayDeque<>();
    private @NotNull final Queue<Pair<BlockPos, BlockState>> destroyBlocks = new ArrayDeque<>();
    private @NotNull final Queue<Pair<BlockPos, BlockState>> structureBlocks = new ArrayDeque<>();
    private @NotNull final Queue<Pair<BlockPos, Function<BlockEntity, BlockEntity>>> structureEntity = new ArrayDeque<>();
    private @NotNull final Queue<Runnable> runnables = new ArrayDeque<>();
    
    public StructureBuilderUtils(@NotNull World world, @NotNull BlockPos pos, @NotNull RegistryKey<Biome> biome, @NotNull String structureName) {
        this.world = world;
        this.biome = biome;
        this.name = structureName;
        this.material = this.forBiome(biome, pos);
        CoreMod.logInfo("Building new '" + structureName + "' in '" + DimensionUtils.dimensionIdentifier(world) + "'");
    }
    
    public String getName() {
        return this.name;
    }
    public int getDelay() {
        return this.delay;
    }
    
    public void addBlock(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        this.locations.add(blockPos);
        Pair<BlockPos, BlockState> pair = new Pair<>(blockPos, blockState);
        this.destroyBlocks.add(pair);
        this.structureBlocks.add(pair);
    }
    public void addEntity(@NotNull BlockPos blockPos, @NotNull Function<BlockEntity, BlockEntity> function) {
        this.locations.add(blockPos);
        this.structureEntity.add(new Pair<>(blockPos, function));
    }
    public void add(@NotNull Runnable runnable) {
        this.runnables.add(runnable);
    }
    
    public boolean hasDestroy() {
        return this.destroyBlocks.peek() != null;
    }
    public boolean hasBuild() {
        return this.structureBlocks.peek() != null || this.structureEntity.peek() != null;
    }
    public boolean hasRunnable() {
        return this.runnables.peek() != null;
    }
    
    public boolean generating() {
        // If there are still locations to peek at
        if (!this.locations.isEmpty()) {
            // Peek at the first block position
            BlockPos pos = this.locations.peek();
            
            // Get the chunk at the location
            Chunk chunk = this.world.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.EMPTY, true);
            
            // Chunks must be fully generated
            if (chunk.getStatus() == ChunkStatus.FULL)
                this.locations.remove(pos);
            return true;
        }
        return false;
    }
    
    public boolean destroy(boolean dropBlocks) {
        Pair<BlockPos, BlockState> pair = this.destroyBlocks.poll();
        if (pair == null)
            return false;
        
        BlockPos blockPos = pair.getLeft();
        BlockState newBlockState = pair.getRight();
        BlockState oldBlockState = this.world.getBlockState(blockPos);
        
        BlockEntity blockEntity = dropBlocks && oldBlockState.hasBlockEntity() ? this.world.getBlockEntity(blockPos) : null;
        
        // Change the block state
        if (newBlockState.getBlock() == this.material.getAirBlock(this.world.getRegistryKey()).getBlock())
            this.world.breakBlock(blockPos, dropBlocks);
        else {
            if (!(oldBlockState.getBlock() instanceof AbstractFireBlock))
                this.world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(oldBlockState));
            
            this.world.setBlockState(blockPos, Blocks.BARRIER.getDefaultState());
            
            // If the old BlockEntity has any drops
            if (blockEntity != null)
                Block.dropStacks(oldBlockState, this.world, blockPos, blockEntity, null, ItemStack.EMPTY);
        }
        
        return true;
    }
    public boolean build() {
        Pair<BlockPos, BlockState> blocks = this.structureBlocks.poll();
        
        // Place all the blocks
        if (blocks != null) {
            BlockPos blockPos = blocks.getLeft();
            BlockState state = blocks.getRight();
            
            // If block is already the same
            if (state.getBlock() != this.world.getBlockState(blockPos).getBlock()) {
                BlockSoundGroup soundGroup = state.getSoundGroup();
                
                this.world.setBlockState(blockPos, state);
                this.world.playSound(null, blockPos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            
            return true;
        }
        
        // Update the block entities
        Pair<BlockPos, Function<BlockEntity, BlockEntity>> entities = this.structureEntity.poll();
        if (entities != null) {
            BlockPos blockPos = entities.getLeft();
            Function<BlockEntity, BlockEntity> function = entities.getRight();
            
            // Get the chunk directly (Won't update properly otherwise)
            WorldChunk worldChunk = this.world.getWorldChunk(blockPos);// Update the block entity
            BlockEntity entity = worldChunk.getBlockEntity(blockPos, WorldChunk.CreationType.IMMEDIATE);
            worldChunk.setBlockEntity(function.apply(entity));
            
            return true;
        }
        
        return false;
    }
    public boolean after() {
        Runnable runnable = this.runnables.poll();
        if (runnable == null)
            return false;
        runnable.run();
        return true;
    }
    
    public <T extends ParticleEffect> void particlesSound(T particle, SoundEvent sound, double deltaX, double deltaY, double deltaZ, double speed, int count, @NotNull BlockPos blockPos) {
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
        this.world.playSound(null, blockPos, sound, SoundCategory.MASTER, 1.0f, 1.0f);
    }
    
    public @NotNull StructureBuilderMaterial forBiome(@NotNull RegistryKey<Biome> biome, @NotNull BlockPos pos) {
        RegistryKey<World> dimension = this.world.getRegistryKey();
        Identifier identifier = biome.getValue();
        String path = identifier.getPath();
        
        if (dimension.equals(World.OVERWORLD)) {
            if (path.contains("desert"))
                return StructureBuilderUtils.DESERT;
            if (path.contains("snow"))
                return StructureBuilderUtils.COLD;
            if (path.contains("ocean") || path.contains("beach"))
                return StructureBuilderUtils.BEACH;
            if (pos.getY() < 0)
                return StructureBuilderUtils.DEEP_DARK;
        }
        else if (dimension.equals(World.NETHER)) {
            return StructureBuilderUtils.DEFAULT_NETHER;
        }
        else if (dimension.equals(World.END)) {
            return StructureBuilderUtils.DEFAULT_END;
        }
        return StructureBuilderUtils.DEFAULT_OVERWORLD;
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
