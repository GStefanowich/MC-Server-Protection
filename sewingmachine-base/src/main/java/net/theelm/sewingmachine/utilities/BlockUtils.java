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

import com.mojang.datafixers.util.Either;
import net.minecraft.block.BedBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.protections.BlockRange;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class BlockUtils {
    
    private BlockUtils() {}
    
    /**
     * Get the block that an entity is looking at
     * @param world The world
     * @param entity The entity
     * @return The hit result
     */
    public static @NotNull BlockHitResult getLookingBlock(@NotNull BlockView world, @NotNull Entity entity) {
        return BlockUtils.getLookingBlock(world, entity, 8);
    }
    
    /**
     * Get the block that an entity is looking at, up to a max distance
     * @param world The world
     * @param entity The entity
     * @param distance How far to check
     * @return The hit result
     */
    public static @NotNull BlockHitResult getLookingBlock(@NotNull BlockView world, @NotNull Entity entity, int distance) {
        return BlockUtils.getLookingBlock(world, entity, distance, RaycastContext.FluidHandling.ANY);
    }
    
    /**
     * Get the block that an entity is looking at, up to a max distance
     * @param world The world
     * @param entity The entity
     * @param distance How far to check
     * @param fluids Whether to collide with fluids or not
     * @return The hit result
     */
    public static @NotNull BlockHitResult getLookingBlock(@NotNull BlockView world, @NotNull Entity entity, int distance, RaycastContext.FluidHandling fluids) {
        // Get the direction the entity is facing
        Vec3d posVec = entity.getCameraPosVec(1.0F); // Get camera pos
        Vec3d lookVec = entity.getRotationVec(1.0F); // Get looking dir
        
        // Trace up to MAX_BLOCK_DISTANCE away
        Vec3d traceVec = posVec.add(
            lookVec.x * distance,
            lookVec.y * distance,
            lookVec.z * distance
        );
        
        return world.raycast(new RaycastContext(posVec, traceVec, RaycastContext.ShapeType.OUTLINE, fluids, entity));
    }
    
    /**
     * Find and light nearby campfires
     * @param world The world to search in
     * @param center The position in which to search
     */
    public static void igniteNearbyLightSources(@NotNull final ServerWorld world, @NotNull final BlockPos center) {
        // Get the nearby range
        BlockRange range = BlockRange.radius(center, 10, 4);
        
        // For each matching campfire block
        range.getBlocks(world, BlockTags.CAMPFIRES).forEach(pos -> {
            // Get the current campfire state
            BlockState state = world.getBlockState(pos);
            
            // Only if the campfire isn't currently lit
            if (!state.get(CampfireBlock.LIT)) {
                world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1F, 1F);
                world.setBlockState(pos, state.with(CampfireBlock.LIT, true));
            }
        });
    }
    
    /**
     * Find and extinguish nearby campfires
     * @param world The world to search in
     * @param center The position in which to search
     */
    public static void extinguishNearbyLightSources(@NotNull final ServerWorld world, @NotNull final BlockPos center) {
        // Get the nearby range
        BlockRange range = BlockRange.radius(center, 10, 4);
        
        // For each matching campfire block
        range.getBlocks(world, BlockTags.CAMPFIRES).forEach(pos -> {
            // Get the current campfire state
            BlockState state = world.getBlockState(pos);
            
            // Only if the campfire isn't already lit
            if (state.get(CampfireBlock.LIT)) {
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1F, 1F);
                world.setBlockState(pos, state.with(CampfireBlock.LIT, false));
            }
        });
    }
    
    /**
     * Test if the block is a thin/not-full (mostly air) block
     * @param state The BlockState to test
     * @return If the Block is a thin block
     */
    public static boolean isBlockCarpet(@NotNull BlockState state) {
        Block block = state.getBlock();
        return block instanceof CarpetBlock
            || block == Blocks.MOSS_CARPET
            || block == Blocks.SNOW;
    }
    
    /**
     * Check if the BlockState is mostly air (or hollow)
     * @param state The BlockState to test
     * @return If the Block consists mostly of air
     */
    public static boolean isHollowBlock(@NotNull BlockState state) {
        return BlockUtils.isBlockCarpet(state) || state.isAir();
    }

    /**
     * Attempt to get the LecternBlock that an entity is looking at
     * @param world The world the Entity is in
     * @param entity The entity that is looking in a direction
     * @param klass
     * @param supplier
     * @param <T>
     * @return
     */
    public static @NotNull <T extends BlockEntity> Either<T, String> getLecternBlockEntity(@NotNull World world, @NotNull Entity entity, Class<T> klass, BiFunction<BlockPos, BlockState, T> supplier) {
        // Get the targeted block
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS)
            return Either.right("Could not find targeted block.");
        BlockPos lecternPos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(lecternPos);
        BlockEntity blockEntity = world.getBlockEntity(lecternPos);
        
        // If the BlockEntity is already of our new type
        if (klass.isInstance(blockEntity)) {
            return Either.left((T) blockEntity);
        } else if (state.getBlock() == Blocks.LECTERN) { // If the Block is a Lectern
            // Get the existing LecternBlock BlockEntity
            if (blockEntity instanceof LecternBlockEntity old) {
                // Drop the book on the lectern
                if (old.hasBook()) {
                    ItemStack book = old.getBook();
                    
                    // Drop the book that is in the stand
                    entity.dropStack(book);
                    
                    // Set the book as empty now
                    old.setBook(ItemStack.EMPTY);
                }
            }
            
            // Update the BlockEntity to the new one
            T newBlockEntity = supplier.apply(lecternPos, state);
            world.addBlockEntity(newBlockEntity);
            return Either.left(newBlockEntity);
        } else {
            return Either.right("Can only set at lecterns.");
        }
    }
    
    public static double angleBetween(@NotNull BlockPos pos1, @NotNull BlockPos pos2) {
        return BlockUtils.angleBetween(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
    }
    
    public static double angleBetween(int x1, int z1, int x2, int z2) {
        double angle = Math.toDegrees(Math.atan2(x2 - x1, z2 - z1));
        return angle + Math.ceil( -angle / 360 ) * 360;
    }
    
    public static @Nullable Direction getDirection(@NotNull BlockPos pos1, @NotNull BlockPos pos2) {
        int x = MathHelper.clamp(pos1.getX() - pos2.getX(), -1, 1);
        int z = MathHelper.clamp(pos1.getZ() - pos2.getZ(), -1, 1);
        
        if (x != 0 && z != 0)
            return null;
        
        for (Direction direction : Direction.values())
            if (direction.getOffsetY() != 0 && direction.getOffsetX() == x && direction.getOffsetZ() == z)
                return direction;
        return null;
    }
    
    public static void updateNeighboringBlockStates(@NotNull ServerPlayerEntity player, @NotNull World world, @NotNull BlockPos blockPos) {
        final BlockState blockState = world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        BlockPos part = null;
        
        if ( block instanceof BedBlock) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            BedPart bedPart = blockState.get(BedBlock.PART);
            part = blockPos.offset(bedPart == BedPart.HEAD ? facing.getOpposite() : facing);
        } else if ( block instanceof HorizontalFacingBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            part = blockPos.offset(facing.getOpposite());
        } else if ( block instanceof TallPlantBlock) {
            DoubleBlockHalf half = blockState.get(TallPlantBlock.HALF);
            part = blockPos.offset(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        } else if ( block instanceof DoorBlock) {
            DoubleBlockHalf half = blockState.get(DoorBlock.HALF);
            part = half == DoubleBlockHalf.LOWER ? blockPos.up() : blockPos.down();
        }
        
        if (part != null)
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, part));
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, blockState));
    }
    
    /**
     * Mark a Block in a ServerWorld (Does nothing if run on the Client) as Dirty
     * @param world The World the block is in
     * @param pos The position of the block
     */
    public static void markDirty(@NotNull World world, @NotNull BlockPos pos) {
        if (world instanceof ServerWorld serverWorld)
            BlockUtils.markDirty(serverWorld, pos);
    }
    
    /**
     * Mark a Block in a ServerWorld as Dirty
     * @param world The World the block is in
     * @param pos The position of the block
     */
    public static void markDirty(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        world.getChunkManager()
            .markForUpdate(pos);
    }
    
    /**
     * Get the chunk pos of the coordinate
     * @param pos
     * @return
     */
    public static int chunkPos(int pos) {
        return (pos - (pos % 16)) / 16;
    }
}
