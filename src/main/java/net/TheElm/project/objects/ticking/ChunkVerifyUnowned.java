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

package net.TheElm.project.objects.ticking;

import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.TickableContext;
import net.TheElm.project.interfaces.TickingAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created on Aug 25 2021 at 11:43 PM.
 * By greg in SewingMachineMod
 */
public class ChunkVerifyUnowned implements TickingAction {
    private boolean success = true;
    private final @NotNull Queue<BlockPos> checks = new ArrayDeque<>();
    
    public ChunkVerifyUnowned(@NotNull final BlockPos blockPos, final int radius) {
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        // For the X axis
        for ( int x = chunkX - radius; x <= chunkX + radius; x++ )
            // For the Z axis
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++)
                this.checks.add(new BlockPos(x << 4, 0, z << 4));
    }
    
    public boolean isSuccess() {
        return this.success;
    }
    
    @Override
    public boolean isCompleted(@NotNull TickableContext tickable) {
        BlockPos check;
        if (tickable.isRemoved())
            return true;
        if (tickable.getTicks() % 2 != 0)
            return false;
        if ((check = this.checks.poll()) == null)
            return true;
        World world = tickable.getWorld();
        
        // Create the chunk position
        WorldChunk worldChunk = world.getWorldChunk(check);
        
        // If the chunk is claimed
        this.success = ((IClaimedChunk) worldChunk).getOwner() == null;
        return !this.success || this.checks.isEmpty();
    }
}
