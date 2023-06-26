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

package net.theelm.sewingmachine.objects.ticking;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.interfaces.TickableContext;
import net.theelm.sewingmachine.interfaces.TickingAction;
import net.theelm.sewingmachine.utilities.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on May 08 2022 at 12:41 PM.
 * By greg in SewingMachineMod
 */
public final class Carver implements TickingAction {
    private static final int PLANE = 16 * 16;
    
    private final @NotNull ServerWorld world;
    private final @Nullable WorldChunk chunk;
    private final int destroyPerTick;
    
    private @NotNull BlockPos position;
    private boolean found = false;
    
    public Carver(@NotNull ServerWorld world, @NotNull WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        
        this.world = world;
        this.chunk = chunk;
        this.destroyPerTick = 18;
        
        this.position = new BlockPos(pos.getStartX(), world.getTopY() - 1, pos.getStartZ());
    }
    public Carver(@NotNull ServerWorld world, @NotNull BlockPos starting) {
        this.world = world;
        this.chunk = null;
        this.destroyPerTick = 1;
        
        this.position = starting;
    }
    public Carver(@NotNull ServerWorld world, @NotNull Vec3d starting) {
        this(world, BlockPos.ofFloored(starting));
    }
    
    @Override
    public boolean isCompleted(@NotNull TickableContext tickable) {
        if (tickable.isRemoved())
            return true;
        
        int maxPer = Carver.PLANE * 4;
        for (int i = 0; i < maxPer; i++) {
            if (this.found)
                maxPer = this.destroyPerTick;
            
            if (this.world.isOutOfHeightLimit(this.position))
                return true;
            
            BlockState blockState = this.world.getBlockState(this.position);
            if (!blockState.isAir()) {
                this.world.breakBlock(this.position, false, null);
                
                this.found = true;
            }
            
            this.moveNext();
        }
        
        return false;
    }
    
    private void moveNext() {
        if (this.chunk == null)
            this.position = this.position.down();
        else {
            int pos = ChunkUtils.getPositionWithinChunk(this.position) + 1;
            int x, y, z;
            if (pos >= Carver.PLANE) {
                x = 0;
                y = -1;
                z = 0;
            } else {
                x = pos % 16;
                y = 0;
                z = (pos - x) / 16;
            }
            
            // Move to the next position
            ChunkPos chunk = this.chunk.getPos();
            this.position = new BlockPos(chunk.getStartX() + x, this.position.getY() + y, chunk.getStartZ() + z);
        }
    }
}
