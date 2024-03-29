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

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Created on Jun 26 2023 at 1:23 AM.
 * By greg in sewingmachine
 */
public final class ChunkUtils {
    private ChunkUtils() {}
    
    public static int getPositionWithinChunk(@NotNull BlockPos blockPos) {
        int chunkIndex = blockPos.getX() & 0xF;
        return (chunkIndex |= (blockPos.getZ() & 0xF) << 4);
    }
    
    public static boolean isPositionWithinSquareRange(@NotNull ChunkPos sourcePos, int range, @NotNull ChunkPos queryPos) {
        int diffX = sourcePos.x - queryPos.x;
        int diffZ = sourcePos.z - queryPos.z;
        
        // Prevent querying chunks that are too far away from the player
        return diffX <= range
            && diffX >= -range
            && diffZ <= range
            && diffZ >= -range;
    }
    
    public static @NotNull Collection<ServerPlayerEntity> getPlayersMonitoring(@NotNull World world, @NotNull ChunkPos chunkPos) {
        if (world instanceof ServerWorld serverWorld)
            return ChunkUtils.getPlayersMonitoring(serverWorld, chunkPos);
        return Collections.emptyList();
    }
    public static @NotNull Collection<ServerPlayerEntity> getPlayersMonitoring(@NotNull ServerWorld world, @NotNull ChunkPos chunkPos) {
        ServerChunkManager chunkManager = world.getChunkManager();
        return chunkManager.threadedAnvilChunkStorage.getPlayersWatchingChunk(chunkPos, false);
    }
}
