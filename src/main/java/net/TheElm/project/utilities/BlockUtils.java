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

import com.mojang.datafixers.util.Either;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.BlockRange;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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
     * Simple check of permissions based on the owners of two positions
     * @param world The world to test the permissions in
     * @param protectedPos The position that is being interacted with ()
     * @param sourcePos The position doing the interacting (A piston, player, etc)
     * @param permission The permission to test
     * @return Whether sourcePos is allowed to do something to protectedPos
     */
    public static boolean canBlockModifyBlock(@NotNull World world, @NotNull BlockPos protectedPos, @NotNull BlockPos sourcePos, @Nullable ClaimPermissions permission) {
        // Get chunks
        WorldChunk protectedChunk = world.getWorldChunk(protectedPos);
        WorldChunk sourceChunk = world.getWorldChunk(sourcePos);
        
        // Check that first chunk owner can modify the next chunk
        return ((IClaimedChunk) protectedChunk).canPlayerDo(protectedPos, ((IClaimedChunk) sourceChunk).getOwner(sourcePos), permission);
    }
    
    public static @NotNull <T extends BlockEntity> Either<T, String> getLecternBlockEntity(@NotNull World world, @NotNull Entity entity, Class<T> klass, Supplier<T> supplier) {
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
            if (blockEntity instanceof LecternBlockEntity) {
                // Drop the book on the lectern
                LecternBlockEntity old = ((LecternBlockEntity)blockEntity);
                if (old.hasBook()) {
                    ItemStack book = old.getBook();
                    
                    // Drop the book that is in the stand
                    entity.dropStack(book);
                    
                    // Set the book as empty now
                    old.setBook(ItemStack.EMPTY);
                }
            }
            
            // Update the BlockEntity to the new one
            T newBlockEntity = supplier.get();
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
    
}
