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

import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockUtils {
    
    private BlockUtils() {}
    
    public static BlockHitResult getLookingBlock(BlockView world, Entity entity) {
        return BlockUtils.getLookingBlock( world, entity, 8 );
    }
    public static BlockHitResult getLookingBlock(BlockView world, Entity entity, int distance) {
        // Get the direction the entity is facing
        Vec3d posVec = entity.getCameraPosVec(1.0F); // Get camera pos
        Vec3d lookVec = entity.getRotationVec(1.0F); // Get looking dir
        
        // Trace up to MAX_BLOCK_DISTANCE away
        Vec3d traceVec = posVec.add(
            lookVec.x * distance,
            lookVec.y * distance,
            lookVec.z * distance
        );
        
        return world.rayTrace(new RayTraceContext( posVec, traceVec, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.ANY, entity));
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
    
}
