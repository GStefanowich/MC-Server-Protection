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

package net.theelm.sewingmachine.protections;

import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public final class BlockRange implements Iterable<BlockPos> {
    
    final BlockPos firstPos;
    final BlockPos secondPos;
    
    final int distEastWest;
    final int distNorthSouth;
    final int distUpDown;
    
    final int volume;
    
    private BlockRange(@NotNull final BlockPos firstPos, @NotNull final BlockPos secondPos) {
        this.firstPos = firstPos;
        this.secondPos = secondPos;
        
        int volume = 1;
        
        int ew = Math.abs(firstPos.getX() - secondPos.getX()); // East-West
        if (ew != 0) volume *= (++ew);
        
        int ns = Math.abs(firstPos.getZ() - secondPos.getZ()); // North-South
        if (ns != 0) volume *= (++ns);
        
        int ud = Math.abs(firstPos.getY() - secondPos.getY()); // Up-Down
        if (ud != 0) volume *= (++ud);
        
        this.distEastWest = ew;
        this.distNorthSouth = ns;
        this.distUpDown = ud;
        this.volume = volume;
    }
    
    // Get the dimensions
    public MutableText displayDimensions() {
        return MessageUtils.dimensionToTextComponent("x", Math.max( this.distEastWest, this.distNorthSouth ), Math.min( this.distEastWest, this.distNorthSouth ), this.distUpDown);
    }
    
    // Volume
    public @NotNull MutableText formattedVolume() {
        return MessageUtils.formatNumber(this.getVolume());
    }
    public int getVolume() {
        return this.volume;
    }
    public boolean hasDistinctVolume() {
        if (this.volume == 1)
            return false;
        return (this.volume != (this.distEastWest + this.distNorthSouth + this.distEastWest));
    }
    
    public boolean isWithin(@NotNull BlockPos search) {
        return (this.withinX(search.getX()) && this.withinY(search.getY()) && this.withinZ(search.getZ()));
    }
    
    // East-West
    public int getEastWest() {
        return distEastWest;
    }
    public Direction isEastOrWest() {
        return (this.firstPos.getX() > this.secondPos.getX() ? Direction.WEST : Direction.EAST);
    }
    
    // North-South
    public int getNorthSouth() {
        return this.distNorthSouth;
    }
    public Direction isNorthOrSouth() {
        return (this.firstPos.getZ() > this.secondPos.getZ() ? Direction.NORTH : Direction.SOUTH);
    }
    
    // Up-Down
    public int getUpDown() {
        return this.distUpDown;
    }
    public Direction isUpOrDown() {
        return (this.firstPos.getY() > this.secondPos.getY() ? Direction.DOWN : Direction.UP);
    }
    
    // Coordinates
    public int getUpperX() {
        return Math.max(
            this.firstPos.getX(),
            this.secondPos.getX()
        );
    }
    public int getLowerX() {
        return Math.min(
            this.firstPos.getX(),
            this.secondPos.getX()
        );
    }
    private boolean withinX(int x) {
        return ((x <= this.getUpperX()) && (x >= this.getLowerX()));
    }
    
    public int getUpperY() {
        return Math.max(
            this.firstPos.getY(),
            this.secondPos.getY()
        );
    }
    public int getLowerY() {
        return Math.min(
            this.firstPos.getY(),
            this.secondPos.getY()
        );
    }
    private boolean withinY(int y) {
        return ((y <= this.getUpperY()) && (y >= this.getLowerY()));
    }
    
    public int getUpperZ() {
        return Math.max(
            this.firstPos.getZ(),
            this.secondPos.getZ()
        );
    }
    public int getLowerZ() {
        return Math.min(
            this.firstPos.getZ(),
            this.secondPos.getZ()
        );
    }
    private boolean withinZ(int z) {
        return ((z <= this.getUpperZ()) && (z >= this.getLowerZ()));
    }
    
    public @NotNull BlockPos getUpper() {
        return new BlockPos(
            this.getUpperX(),
            this.getUpperY(),
            this.getUpperZ()
        );
    }
    public @NotNull BlockPos getLower() {
        return new BlockPos(
            this.getLowerX(),
            this.getLowerY(),
            this.getLowerZ()
        );
    }
    
    // World interaction
    public @NotNull Collection<BlockPos> getBlocks(@NotNull WorldView world, @NotNull Block block) {
        return this.getBlocks(world, (state) -> state.getBlock().equals(block));
    }
    public @NotNull Collection<BlockPos> getBlocks(@NotNull WorldView world, @NotNull TagKey<Block> tag) {
        return this.getBlocks(world, (state) -> state.isIn(tag));
    }
    public @NotNull Collection<BlockPos> getBlocks(@NotNull WorldView world, @NotNull Predicate<BlockState> predicate) {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos pos : this) {
            if (predicate.test(world.getBlockState(pos)))
                list.add(pos);
        }
        return list;
    }
    
    public static @NotNull BlockRange radius(@NotNull BlockPos center, int radius) {
        return BlockRange.radius(center, radius, radius);
    }
    public static @NotNull BlockRange radius(@NotNull BlockPos center, int radius, int height) {
        return BlockRange.between(
        new BlockPos(center.getX() - radius, center.getY() - height, center.getZ() - radius),
        new BlockPos(center.getX() + radius, center.getY() + height, center.getZ() + radius)
        );
    }
    public static @NotNull BlockRange between(@NotNull BlockPos firstPos, @NotNull BlockPos secondPos) {
        return new BlockRange(firstPos, secondPos);
    }
    public static @NotNull BlockRange of(@NotNull BlockPos pos) {
        return new BlockRange(pos, pos);
    }
    
    @Override
    public @NotNull Iterator<BlockPos> iterator() {
        return new BlockRangeIterator(this.getLower(), this.getUpper());
    }
    
    public static class BlockRangeIterator implements Iterator<BlockPos> {
        private final @NotNull BlockPos starts;
        private final @NotNull BlockPos ends;
        
        private int x;
        private int y;
        private int z;
        
        private BlockRangeIterator(@NotNull BlockPos starting, @NotNull BlockPos ending) {
            this.starts = starting;
            this.ends = ending;
            
            this.x = starting.getX();
            this.y = starting.getY();
            this.z = starting.getZ();
        }
        
        @Override
        public boolean hasNext() {
            return this.y <= this.ends.getY();
        }
        
        @Override
        public BlockPos next() {
            BlockPos next = new BlockPos(this.x, this.y, this.z);
            if (this.isAtEndRow()) {
                this.y++;
                this.x = this.starts.getX();
                this.z = this.starts.getZ();
            } else if (this.isAtEndX()) {
                this.z++;
                this.x = this.starts.getX();
            } else {
                this.x++;
            }
            return next;
        }
        
        private boolean isAtEndRow() {
            return this.isAtEndX() && this.isAtEndZ();
        }
        private boolean isAtEndX() {
            return this.x == this.ends.getX();
        }
        private boolean isAtEndZ() {
            return this.z == this.ends.getZ();
        }
        private boolean isAtEndY() {
            return this.y == this.ends.getY();
        }
    }
}
