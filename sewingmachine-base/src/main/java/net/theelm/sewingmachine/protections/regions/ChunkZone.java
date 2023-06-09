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

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.utilities.MapUtils;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created on Jul 30 2021 at 11:02 PM.
 * By greg in SewingMachineMod
 */
public class ChunkZone {
    //public final @NotNull OutlineMaintainer outline;
    private final @NotNull Set<LinkedPointChunk> chunks = new LinkedHashSet<>();
    
    public final @NotNull Claimant claimant;
    
    public final @NotNull RegistryKey<World> world;
    
    public final @NotNull String mapId;
    public final @NotNull String label;
    public final @NotNull String description;
    
    public ChunkZone(@NotNull ClaimTag first, @NotNull Claimant owner) {
        System.out.println("Created a new claim region");
        this.world = first.getDimension();
        //this.outline = new OutlineMaintainer(first);
        this.chunks.add(LinkedPointChunk.of(first));
        
        this.mapId = "region" + first.getX() + "-" + first.getUpperZ();
        
        if (Objects.equals(owner.getId(), CoreMod.SPAWN_ID)) {
            this.label = "Spawn";
            this.description = "Spawn";
        } else {
            this.label = owner.getName().getString();
            this.description = this.label + "'s claimed area";
        }
        this.claimant = owner;
    }
    public ChunkZone(@NotNull RegistryKey<World> world, @NotNull NbtList list, @NotNull Claimant owner) {
        this.world = world;
        //this.outline = new OutlineMaintainer(list);
        
        this.mapId = "";
        this.label = "";
        this.description = "";
        
        this.claimant = owner;
    }
    
    public boolean tryConsume(@NotNull Claimant claimant, @NotNull WorldChunk chunk) {
        return this.tryConsume(claimant, ClaimTag.of(chunk));
    }
    public boolean tryConsume(@NotNull Claimant claimant, @NotNull ClaimTag tag) {
        if (!Objects.equals(tag.getDimension(), this.world) || !Objects.equals(this.claimant.getId(), claimant.getId()))
            return false;
        /*boolean success = this.outline.push(tag);
        if (success)
            this.chunks.add(tag);*/
        LinkedPointChunk convert = LinkedPointChunk.of(tag);
        for (LinkedPointChunk chunk : this.chunks) {
            if (chunk.setLinked(convert)) {
                this.chunks.add(convert);
                return true;
            }
        }
        return false;
    }
    
    public Vector2d[] toArray() {
        List<Vector2d> points = new ArrayList<>();
        for (LinkedPointChunk chunk : this.chunks) {
            points.addAll(chunk.gatherPoints());
        }
        
        return Arrays.stream(points.toArray(new Vector2d[0]))
            .sorted((t1, t2) -> {
                if ((t1.getX() == t2.getX()) && (t1.getY() == t2.getY()))
                    return 0;
                if ((t1.getX() == t2.getX()) || (t1.getY() == t2.getY()))
                    return 1;
                return Double.compare(t1.getX(), t2.getX()) + Double.compare(t1.getY(), t2.getY());
            })
            .toArray(Vector2d[]::new);
        
        /*Collections.sort(points, (t1, t2) -> {
            if ((t1.getX() == t2.getX()) || (t1.getY() == t2.getY()))
                return 0;
            return Double.compare(t1.getX(), t2.getX()) + Double.compare(t1.getY(), t2.getY());
        });
        
        return points.toArray(new Vector2d[0]);*/
    }
    
    public int size() {
        return this.chunks.size();
    }
    
    public static class Builder implements Consumer<BlueMapAPI> {
        private final @NotNull Claimant claimant;
        private final @NotNull Set<ChunkZone> regions = new HashSet<>();
        private final @NotNull Set<ClaimTag> regionChunks = new HashSet<>();
        
        public Builder(@NotNull Claimant claimant) {
            this.claimant = claimant;
        }
        
        public ChunkZone.Builder add(Collection<ClaimTag> collection) {
            this.regionChunks.addAll(collection);
            return this;
        }
        public ChunkZone.Builder add(ClaimTag tag) {
            this.regionChunks.add(tag);
            return this;
        }
        
        public @NotNull Set<ChunkZone> build() {
            while (!this.regionChunks.isEmpty()) {
                ClaimTag regionTag = null;
                boolean consumed = !this.regions.isEmpty();

                Iterator<ClaimTag> regionIterator = this.regionChunks.iterator();
                outer:
                while (regionIterator.hasNext()) {
                    regionTag = regionIterator.next();
                    // If no region exists, skip trying to iterate forever and just make one
                    if (this.regions.isEmpty())
                        break;
                    for (ChunkZone region : this.regions) {
                        if (consumed = region.tryConsume(this.claimant, regionTag)) {
                            regionIterator.remove();
                            break outer;
                        }
                    }
                }
                
                if (!consumed && regionTag != null) {
                    // Create a new region for the unconsumed chunk
                    if (regionTag.getDimension() != null)
                        regions.add(new ChunkZone(regionTag, this.claimant));
                    regionIterator.remove();
                }
            }
            
            // Run the bluemap plotter
            MapUtils.RUN.add(this);
            
            // Return the generated regions
            return this.regions;
        }
        
        private @NotNull ChunkZone addRegion(@NotNull ClaimTag tag) {
            ChunkZone region = new ChunkZone(tag, this.claimant);
            
            this.regions.add(region);
            
            return region;
        }
        
        @Override
        public void accept(BlueMapAPI blueMapAPI) {
            for (ChunkZone region : this.regions) {
                Vector2d[] points = region.toArray();
                System.out.println("Region Size -> " + region.size() + " (" + points.length + " corners)");
            }
        }
        
        public static @NotNull Set<ChunkZone> build(@NotNull final Collection<ClaimTag> collection, @NotNull Claimant claimant) {
            Builder builder = new Builder(claimant);
            
            builder.add(collection);
            
            return builder.build();
        }
    }
    public static class OutlineMaintainer {
        private final @NotNull List<Vector2d> outline = new LinkedList<>();
        
        private OutlineMaintainer(@NotNull ClaimTag tag) {
            int lowerX = tag.getX() << 4,
                lowerZ = tag.getZ() << 4,
                upperX = lowerX + 15,
                upperZ = lowerZ + 15;
            
            this.outline.add(new Vector2d(lowerX, lowerZ));
            this.outline.add(new Vector2d(lowerX, upperZ));
            this.outline.add(new Vector2d(upperX, upperZ));
            this.outline.add(new Vector2d(upperX, lowerZ));
        }
        private OutlineMaintainer(@NotNull NbtList points) {
            
        }
        
        public boolean push(@NotNull ClaimTag tag) {
            int lowerX = tag.getLowerX(),
                lowerZ = tag.getLowerZ(),
                upperX = tag.getUpperX(),
                upperZ = tag.getUpperZ();
            
            int x = 0,
                z = 0;
            int size = this.outline.size();
            for (int i1 = 0; i1 < size; i1++) {
                int i2 = i1 + 1 >= size ? 0 : i1 + 1;
                
                Vector2d fir = this.outline.get(i1),
                    sec = this.outline.get(i2);
                
                LineShift shift = OutlineMaintainer.isLineNeigbor(fir, sec, tag.getX(), tag.getZ());
                if (shift != null) {
                    System.out.println(shift + " " + tag.getX() + ", " + tag.getZ());
                    //System.out.println(lowerX + ", " + lowerZ + " -> " + upperX + ", " + upperZ);
                    
                    switch (shift) {
                        case SHARES_Z_AXIS_UPPER_X:
                            this.outline.set(i1, new Vector2d(fir.getFloorX() - 16, fir.getFloorY()));
                            this.outline.set(i2, new Vector2d(sec.getFloorX() - 16, sec.getFloorY()));
                            break;
                        case SHARES_Z_AXIS_LOWER_X:
                            break;
                        case END_Z_AXIS_UPPER_X:
                            break;
                        case END_Z_AXIS_LOWER_X:
                            break;
                        case SHARES_X_AXIS_UPPER_Z:
                            break;
                        case SHARES_X_AXIS_LOWER_Z:
                            break;
                        case END_X_AXIS_UPPER_Z:
                            
                            break;
                        case END_X_AXIS_LOWER_Z:
                            break;
                        default:
                            return true;
                    }
                    
                    return true;
                }
            }
            
            return false;
        }
        
        public Vector2d[] toArray() {
            return this.outline.toArray(new Vector2d[0]);
        }
        public int size() {
            return this.outline.size();
        }
        
        public static @Nullable LineShift isLineNeigbor(@NotNull Vector2d fir, @NotNull Vector2d sec, int chunkX, int chunkZ) {
            int firX = fir.getFloorX() >> 4,
                firZ = fir.getFloorY() >> 4,
                secX = sec.getFloorX() >> 4,
                secZ = sec.getFloorY() >> 4;
            int minX = Math.min(firX, secX),
                maxX = Math.max(firX, secX),
                minZ = Math.min(firZ, secZ),
                maxZ = Math.max(firZ, secZ);
            
            if (minX == maxX) {
                if ((minZ <= chunkZ) && (maxZ >= chunkZ)) {
                    if (chunkX + 1 == minX)
                        return LineShift.SHARES_Z_AXIS_UPPER_X;
                    if (chunkX - 1 == minX)
                        return LineShift.SHARES_Z_AXIS_LOWER_X;
                }
                if (chunkX == minX) {
                    if (minZ - 1 == chunkZ)
                        return LineShift.END_X_AXIS_LOWER_Z;
                    if (maxZ + 1 == chunkZ)
                        return LineShift.END_X_AXIS_UPPER_Z;
                }
            }
            if (minZ == maxZ) {
                if ((minX <= chunkX) && (maxX >= chunkX)) {
                    if (chunkZ + 1 == minZ)
                        return LineShift.SHARES_X_AXIS_UPPER_Z;
                    if (chunkZ - 1 == minZ)
                        return LineShift.SHARES_X_AXIS_LOWER_Z;
                }
                if (chunkZ == minZ) {
                    if (minX - 1 == chunkX)
                        return LineShift.END_Z_AXIS_LOWER_X;
                    if (maxX + 1 == chunkX)
                        return LineShift.END_Z_AXIS_UPPER_X;
                }
            }
            return null;
        }
    }
    public enum LineShift {
        SHARES_Z_AXIS_UPPER_X,
        SHARES_Z_AXIS_LOWER_X,
        END_Z_AXIS_UPPER_X,
        END_Z_AXIS_LOWER_X,
        SHARES_X_AXIS_UPPER_Z,
        SHARES_X_AXIS_LOWER_Z,
        END_X_AXIS_UPPER_Z,
        END_X_AXIS_LOWER_Z
    }
}
