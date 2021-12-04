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

package net.TheElm.project.protections.claiming;

import com.flowpowered.math.vector.Vector2d;
import net.TheElm.project.objects.ClaimTag;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Created on Aug 29 2021 at 3:22 PM.
 * By greg in SewingMachineMod
 */
public class LinkedPointChunk {
    private final int x;
    private final int z;
    
    private @Nullable LinkedPointChunk linkedNorth;
    private @Nullable LinkedPointChunk linkedEast;
    private @Nullable LinkedPointChunk linkedSouth;
    private @Nullable LinkedPointChunk linkedWest;
    
    private LinkedPointChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }
    private LinkedPointChunk(@NotNull Vec3i pos) {
        this.x = pos.getX() >> 4;
        this.z = pos.getZ() >> 4;
    }
    
    public int getX() {
        return this.x;
    }
    public int getZ() {
        return this.z;
    }
    
    public int getStartX() {
        return this.getX() << 4;
    }
    
    public int getStartZ() {
        return this.getZ() << 4;
    }
    
    public int getEndX() {
        return this.getStartX() + 15;
    }
    
    public int getEndZ() {
        return this.getStartZ() + 15;
    }
    
    public boolean hasPointNorthEast() {
        return this.hasPointDirection(Direction.NORTH, Direction.EAST);
    }
    public boolean hasPointNorthWest() {
        return this.hasPointDirection(Direction.NORTH, Direction.WEST);
    }
    public boolean hasPointSouthEast() {
        return this.hasPointDirection(Direction.SOUTH, Direction.EAST);
    }
    public boolean hasPointSouthWest() {
        return this.hasPointDirection(Direction.SOUTH, Direction.WEST);
    }
    
    public boolean hasPointDirection(@NotNull Direction a, @NotNull Direction b) {
        LinkedPointChunk linkA = this.getLinked(a);
        LinkedPointChunk linkB = this.getLinked(b);
        if (linkA == null && linkB == null)
            return true;
        if (linkA != null && linkB != null)
            return linkA.getLinked(b) == null;
        return !Objects.equals(linkA == null ? null : linkA.getLinked(b), linkB == null ? null : linkB.getLinked(a));
    }
    
    public @Nullable LinkedPointChunk getLinked(@NotNull Direction direction) {
        switch (direction) {
            case NORTH: return this.linkedNorth;
            case SOUTH: return this.linkedSouth;
            case EAST: return this.linkedEast;
            case WEST: return this.linkedWest;
        }
        return null;
    }
    public @Nullable LinkedPointChunk getLinked(@NotNull Direction a, @NotNull Direction b) {
        LinkedPointChunk pos = this.getLinked(a);
        return pos == null ? null : pos.getLinked(b);
    }
    public boolean setLinked(@NotNull LinkedPointChunk chunk) {
        Direction direction = this.getOffset(chunk);
        boolean set;
        if (set = (direction == Direction.NORTH)) {
            this.linkedNorth = chunk;
            chunk.linkedSouth = this;
            chunk.linkedEast = this.getLinked(Direction.EAST, Direction.NORTH);
            chunk.linkedWest = this.getLinked(Direction.WEST, Direction.NORTH);
        } else if (set = (direction == Direction.EAST)) {
            this.linkedEast = chunk;
            chunk.linkedWest = this;
            chunk.linkedNorth = this.getLinked(Direction.NORTH, Direction.EAST);
            chunk.linkedSouth = this.getLinked(Direction.SOUTH, Direction.EAST);
        } else if (set = (direction == Direction.SOUTH)) {
            this.linkedSouth = chunk;
            chunk.linkedNorth = this;
            chunk.linkedEast = this.getLinked(Direction.EAST, Direction.SOUTH);
            chunk.linkedWest = this.getLinked(Direction.WEST, Direction.SOUTH);
        } else if (set = (direction == Direction.WEST)) {
            this.linkedWest = chunk;
            chunk.linkedEast = this;
            chunk.linkedNorth = this.getLinked(Direction.NORTH, Direction.WEST);
            chunk.linkedSouth = this.getLinked(Direction.SOUTH, Direction.WEST);
        }
        return set;
    }
    
    public @Contract("-> new") LinkedPointChunk north() {
        return this.offset(Direction.NORTH);
    }
    
    public @Contract("-> new") LinkedPointChunk east() {
        return this.offset(Direction.EAST);
    }
    
    public @Contract("-> new") LinkedPointChunk south() {
        return this.offset(Direction.SOUTH);
    }
    
    public @Contract("-> new") LinkedPointChunk west() {
        return this.offset(Direction.WEST);
    }
    
    public @Contract("_ -> new") LinkedPointChunk offset(@NotNull Direction direction) {
        return new LinkedPointChunk(this.getX() + direction.getOffsetX(), this.getZ() + direction.getOffsetZ());
    }
    
    public @NotNull LinkedPointChunk offset(@NotNull Direction direction, int count) {
        return count == 0 ? this : new LinkedPointChunk(this.getX() + (direction.getOffsetX() * 2), this.getZ() + (direction.getOffsetZ() * count));
    }
    
    public @Nullable Direction getOffset(@NotNull LinkedPointChunk chunk) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == Direction.Axis.Y)
                continue;
            if (chunk.getX() == (this.getX() + direction.getOffsetX()) && chunk.getZ() == (this.getZ() + direction.getOffsetZ()))
                return direction;
        }
        return null;
    }
    
    public Collection<Vector2d> gatherPoints() {
        List<Vector2d> list = new ArrayList<>();
        if (this.hasPointNorthEast())
            list.add(new Vector2d(this.getEndX(), this.getStartZ()));
        if (this.hasPointNorthWest())
            list.add(new Vector2d(this.getStartX(), this.getStartZ()));
        if (this.hasPointSouthEast())
            list.add(new Vector2d(this.getEndX(), this.getEndZ()));
        if (this.hasPointSouthWest())
            list.add(new Vector2d(this.getStartX(), this.getEndZ()));
        return list;
    }
    
    public static @Contract("_, _ -> new") LinkedPointChunk of(int x, int z) {
        return new LinkedPointChunk(x, z);
    }
    public static @Contract("null -> null; !null -> new") LinkedPointChunk of(WorldChunk chunk) {
        return chunk == null ? null : LinkedPointChunk.of(chunk.getPos());
    }
    public static @Contract("null -> null; !null -> new") LinkedPointChunk of(ChunkPos chunk) {
        return chunk == null ? null : new LinkedPointChunk(chunk.x, chunk.z);
    }
    public static @Contract("null -> null; !null -> new") LinkedPointChunk of(ClaimTag claim) {
        return claim == null ? null : new LinkedPointChunk(claim.getX(), claim.getZ());
    }
    public static @Contract("null -> null; !null -> new") LinkedPointChunk of(Vec3i pos) {
        return pos == null ? null : new LinkedPointChunk(pos);
    }
    
    @Override
    public boolean equals(Object unknown) {
        if (!(unknown instanceof LinkedPointChunk other))
            return false;
        return this.x == other.x
            && this.z == other.z;
    }
}
