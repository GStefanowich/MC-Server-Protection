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

package net.theelm.sewingmachine.protections.regions;

import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created on Apr 15 2022 at 2:18 AM.
 * By greg in SewingMachineMod
 */
public final class BlockPosChainLink implements Iterable<BlockPosChainLink> {
    public final BlockPos pos;
    
    private boolean isDirty;
    private boolean needsCleanup;
    
    public @NotNull BlockPosChainLink next;
    public @NotNull BlockPosChainLink previous;
    
    private @NotNull BlockPosChainLink first;
    
    public BlockPosChainLink(@NotNull BlockPos pos) {
        this.next = this.previous = this.first = this;
        this.pos = pos;
    }
    public BlockPosChainLink(@NotNull Chunk chunk) {
        this(chunk.getPos());
    }
    public BlockPosChainLink(@NotNull ChunkPos pos) {
        this(pos.getStartPos());
        this.addPoint(pos.getBlockPos(16, 0, 0))
            .addPoint(pos.getBlockPos(0, 0, 16))
            .addPoint(pos.getBlockPos(16, 0, 16));
    }
    
    public @NotNull BlockPosChainLink addPoint(@NotNull BlockPos pos) {
        BlockPosChainLink next = this.first;
        do {
            // If equal to
            if (next.equals(pos))
                return next;
            
            // Move to next
            next = next.next;
        } while (!next.isFirst());
        
        next = this.first;
        do {
            // Insert the point
            if (next.isParallel(pos))
                return this.insert(pos);
            
            // Move to next
            next = next.next;
        } while (!next.isFirst());
        
        return this.insert(pos);
    }
    public @NotNull BlockPosChainLink addChunk(@NotNull ChunkPos pos) {
        return this.addPoint(pos.getStartPos())
            .addPoint(pos.getBlockPos(16, 0, 0))
            .addPoint(pos.getBlockPos(0, 0, 16))
            .addPoint(pos.getBlockPos(16, 0, 16));
    }
    public @NotNull BlockPosChainLink addChunk(@NotNull Chunk chunk) {
        return this.addChunk(chunk.getPos());
    }
    public @Contract("_ -> this") BlockPosChainLink addChain(@NotNull BlockPosChainLink chain) {
        BlockPosChainLink next = chain.first;
        BlockPosChainLink start = null;
        
        // Get the linkup positions
        do {
            if (this.contains(next.pos)) {
                start = next;
                break;
            }
            
            // Move to next
            next = next.next;
        } while (!next.isFirst());
        
        // If a shared position was found
        if (start != null) {
            start.updateToFirst();
            start.forEach(link -> this.addPoint(link.pos));
        }
        
        return this;
    }
    
    public @NotNull BlockPosChainLink insert(@NotNull BlockPos pos) {
        BlockPosChainLink chain = new BlockPosChainLink(pos);
        
        // Set the FIRST
        chain.first = this.first;
        
        this.next.previous = chain;
        chain.previous = this;
        chain.next = this.next;
        this.next = chain;
        
        this.markForCleanup();
        
        return chain;
    }
    private void setToNext(@NotNull BlockPosChainLink next) {
        this.next = next;
        next.previous = this;
    }
    public @NotNull BlockPosChainLink append(@NotNull BlockPos pos) {
        return this.first.previous.insert(pos);
    }
    public boolean remove() {
        if (this.isFirst() || !this.canBeIgnored())
            return false;
        
        this.previous.setToNext(this.next);
        return true;
    }
    
    public int size() {
        int size = 0;
        
        BlockPosChainLink next = this.first;
        do {
            size++;
            
            // Move to next
            next = next.next;
        } while (!next.isFirst());
        
        return size;
    }
    
    private void updateToFirst() {
        BlockPosChainLink next = this;
        do {
            next.first = this;
            
            // Move to next
            next = next.next;
        } while (!next.equals(this));
    }
    
    public void markDirty() {
        this.isDirty = true;
    }
    public void markForCleanup() {
        this.needsCleanup = true;
    }
    private void cleanup() {
        if (this.needsCleanup) {
            this.needsCleanup = false;
            Iterable.super.forEach(BlockPosChainLink::remove);
        }
    }
    
    public boolean isFirst() {
        return this.equals(this.first);
    }
    public boolean contains(@Nullable BlockPos pos) {
        for (BlockPosChainLink link : this) {
            if (link.equals(pos))
                return true;
        }
        
        return false;
    }
    public boolean isParallel(@NotNull BlockPos pos) {
        return this.pos.getX() == pos.getX()
            || this.pos.getZ() == pos.getZ();
    }
    public boolean isParallel(@NotNull BlockPosChainLink pos) {
        return this.isParallel(pos.pos);
    }
    
    public boolean canBeIgnored() {
        Direction direction = this.getDirection(this.previous);
        return direction != null && direction.getOpposite() == this.getDirection(this.next);
    }
    public @Nullable Direction getDirection(@NotNull BlockPosChainLink link) {
        return this.getDirection(link.pos);
    }
    public @Nullable Direction getDirection(@NotNull BlockPos pos) {
        Direction direction = BlockUtils.getDirection(this.pos, pos);
        System.out.println(direction);
        return direction;
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof BlockPosChainLink other)
            return this.equals(other.pos);
        if (obj instanceof BlockPos pos)
            return this.equals(pos);
        return false;
    }
    public boolean equals(@Nullable BlockPos pos) {
        return Objects.equals(pos, this.pos);
    }
    public boolean equals(@Nullable BlockPosChainLink other) {
        return other != null && Objects.equals(other.pos, this.pos);
    }
    
    @Override
    public void forEach(Consumer<? super BlockPosChainLink> action) {
        this.cleanup();
        Iterable.super.forEach(action);
    }
    
    public boolean intersects(@Nullable BlockPosChainLink other) {
        if (other == null || Objects.equals(this.first, other.first))
            return false;
        for (BlockPosChainLink a : this) {
            for (BlockPosChainLink b : other) {
                if (Objects.equals(a, b))
                    return true;
            }
        }
        return false;
    }
    
    public @NotNull NbtList toNbt() {
        NbtList list = new NbtList();
        // Add all chain links to the Nbt
        for (BlockPosChainLink link : this)
            list.add(NbtUtils.blockPosToTag(link.pos));
        return list;
    }
    public static BlockPosChainLink fromNbt(@NotNull NbtList list) {
        BlockPosChainLink chain = null;
        
        for (int i = 0; i < list.size(); i++) {
            BlockPos link = NbtUtils.tagToBlockPos(list.getCompound(i));
            if (link == null)
                continue;
            
            if (chain == null)
                chain = new BlockPosChainLink(link);
            else chain.append(link);
        }
        
        return chain;
    }
    
    @Override
    public @NotNull Iterator<BlockPosChainLink> iterator() {
        this.cleanup();
        return new BlockPosChain(this.first);
    }
    
    private static class BlockPosChain implements Iterator<BlockPosChainLink> {
        private final BlockPosChainLink first;
        private BlockPosChainLink link;
        
        public BlockPosChain(@NotNull BlockPosChainLink first) {
            if (!first.isFirst())
                throw new IllegalArgumentException("");
            this.first = first;
        }
        
        @Override
        public boolean hasNext() {
            return this.link == null || !this.link.next.isFirst();
        }
        
        @Override
        public BlockPosChainLink next() {
            return (this.link = (this.link == null ? this.first : this.link.next));
        }
    }
}
