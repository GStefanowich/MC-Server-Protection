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
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import net.TheElm.project.CoreMod;
import net.TheElm.project.objects.ClaimTag;
import net.TheElm.project.utilities.MapUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created on Jul 30 2021 at 11:02 PM.
 * By greg in SewingMachineMod
 */
public class ClaimRegion {
    
    public final @NotNull OutlineMaintainer outline;
    private final @NotNull Set<ClaimTag> chunks = new LinkedHashSet<>();
    
    public final @NotNull Claimant claimant;
    
    public final @NotNull RegistryKey<World> world;
    
    public final @NotNull String mapId;
    public final @NotNull String label;
    public final @NotNull String description;
    
    public ClaimRegion(@NotNull ClaimTag first, @NotNull Claimant owner) {
        System.out.println("Created a new claim region");
        this.world = first.getDimension();
        this.outline = new OutlineMaintainer(first);
        this.chunks.add(first);
        
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
    public ClaimRegion(@NotNull RegistryKey<World> world, @NotNull ListTag list, @NotNull Claimant owner) {
        this.world = world;
        this.outline = new OutlineMaintainer(list);
        
        this.mapId = "";
        this.label = "";
        this.description = "";
        
        this.claimant = owner;
    }
    
    public boolean tryConsume(@NotNull Claimant claimant,@NotNull WorldChunk chunk) {
        return this.tryConsume(claimant, ClaimTag.of(chunk));
    }
    public boolean tryConsume(@NotNull Claimant claimant, @NotNull ClaimTag tag) {
        if (!Objects.equals(tag.getDimension(), this.world) || !Objects.equals(this.claimant.getId(), claimant.getId()))
            return false;
        boolean success = this.outline.push(tag);
        if (success)
            this.chunks.add(tag);
        return success;
    }
    
    public int size() {
        return this.chunks.size();
    }
    
    public static @NotNull Set<ClaimRegion> generateFromExistingClaims(@NotNull final Collection<ClaimTag> collection, @NotNull Claimant claimant) {
        Set<ClaimRegion> regions = new HashSet<>();
        Set<ClaimTag> regionChunks = new HashSet<>(collection);
        while (!regionChunks.isEmpty()) {
            ClaimTag regionTag = null;
            boolean consumed = !regions.isEmpty();

            Iterator<ClaimTag> regionIterator = regionChunks.iterator();
            outer:
            while (regionIterator.hasNext()) {
                regionTag = regionIterator.next();
                // If no region exists, skip trying to iterate forever and just make one
                if (regions.isEmpty())
                    break;
                for (ClaimRegion region : regions) {
                    if (consumed = region.tryConsume(claimant, regionTag)) {
                        regionIterator.remove();
                        break outer;
                    }
                }
            }

            if (!consumed && regionTag != null) {
                // Create a new region for the unconsumed chunk
                if (regionTag.getDimension() != null)
                    regions.add(new ClaimRegion(regionTag, claimant));
                regionIterator.remove();
            }
        }
        
        MapUtils.RUN.add((api) -> {
            for (ClaimRegion region : regions) {
                System.out.println("Region Size -> " + region.size() + " (" + region.outline.size() + " corners)");
                MapUtils.withMarker(region.world, "claims", (map, set) -> {
                    if (!set.getMarker(region.mapId).isPresent()) {
                        ShapeMarker shapeMarker = set.createShapeMarker(region.mapId, map, new Shape(region.outline.toArray()), 64);
                        shapeMarker.setLabel(region.label);
                        shapeMarker.setDetail(region.description);

                        return true;
                    }
                    return false;
                });
            }
        });
        
        return regions;
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
        private OutlineMaintainer(@NotNull ListTag points) {
            
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
