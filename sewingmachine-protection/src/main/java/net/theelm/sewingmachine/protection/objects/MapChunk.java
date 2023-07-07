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

package net.theelm.sewingmachine.protection.objects;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.fluid.FluidState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public final class MapChunk {
    // The number of blocks square that a chunk is
    public static final int WIDTH = 16;
    public static final int HOVER_HIGHLIGHT;
    public static final int CLAIM_HIGHLIGHT;
    static {
        Color color;
        
        color = Color.YELLOW;
        HOVER_HIGHLIGHT = ColorHelper.Argb.getArgb(100, color.getRed(), color.getGreen(), color.getBlue());
        
        color = Color.CYAN;
        CLAIM_HIGHLIGHT = ColorHelper.Argb.getArgb(40, color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private final @NotNull World world;
    private final @NotNull WorldChunk chunk;
    
    private final int[][] colors = new int[WIDTH][WIDTH];
    private final boolean ceiling;
    
    private @Nullable Text owner = Text.literal("TheElm");
    
    public MapChunk(@NotNull WorldChunk chunk) {
        this.chunk = chunk;
        this.world = chunk.getWorld();
        this.ceiling = world.getDimension()
            .hasCeiling();
    }
    
    public void update() {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos.Mutable mutable2 = new BlockPos.Mutable();
        if (this.chunk.isEmpty())
            return;
        
        for(int x = 0; x < MapChunk.WIDTH; x++) {
            double d = 0.0;
            
            int[] colors = this.colors[x];
            for(int z = 0; z < MapChunk.WIDTH; z++) {
                Multiset<MapColor> multiset = LinkedHashMultiset.create();
                int t = 0;
                double e = 0.0;
                if (this.ceiling) {
                    int u = x + z * 231871;
                    u = u * u * 31287121 + u * 11;
                    
                    if ((u >> 20 & 1) == 0) {
                        multiset.add(Blocks.DIRT.getDefaultState().getMapColor(this.world, BlockPos.ORIGIN), 10);
                    } else {
                        multiset.add(Blocks.STONE.getDefaultState().getMapColor(this.world, BlockPos.ORIGIN), 100);
                    }
                    
                    e = 100.0;
                } else {
                    mutable.set(x, 0, z);
                    int y = this.chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                    BlockState blockState;
                    if (y <= world.getBottomY() + 1) {
                        blockState = Blocks.BEDROCK.getDefaultState();
                    } else {
                        do {
                            --y;
                            mutable.setY(y);
                            blockState = this.chunk.getBlockState(mutable);
                        } while (blockState.getMapColor(this.world, mutable) == MapColor.CLEAR && y > this.world.getBottomY());
                        
                        if (y > this.world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                            int y2 = y - 1;
                            mutable2.set(mutable);
                            
                            BlockState blockState2;
                            do {
                                mutable2.setY(y2--);
                                blockState2 = this.chunk.getBlockState(mutable2);
                                ++t;
                            } while(y2 > this.world.getBottomY() && !blockState2.getFluidState().isEmpty());
                            
                            blockState = this.getFluidStateIfVisible(this.world, blockState, mutable);
                        }
                    }
                    
                    e += y;
                    multiset.add(blockState.getMapColor(this.world, mutable));
                }
                
                MapColor mapColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
                MapColor.Brightness brightness;
                double f;
                if (mapColor == MapColor.WATER_BLUE) {
                    f = (double)t * 0.1 + (double)(x + z & 1) * 0.2;
                    if (f < 0.5) {
                        brightness = MapColor.Brightness.HIGH;
                    } else if (f > 0.9) {
                        brightness = MapColor.Brightness.LOW;
                    } else {
                        brightness = MapColor.Brightness.NORMAL;
                    }
                } else {
                    f = (e - d) * 4.0 / (double)(4) + ((double)(x + z & 1) - 0.5) * 0.4;
                    if (f > 0.6) {
                        brightness = MapColor.Brightness.HIGH;
                    } else if (f < -0.6) {
                        brightness = MapColor.Brightness.LOW;
                    } else {
                        brightness = MapColor.Brightness.NORMAL;
                    }
                }
                
                d = e;
                colors[z] = this.getRenderColor(mapColor, brightness);
            }
        }
    }
    
    public void render(@NotNull DrawContext context, @NotNull FrameData frame, boolean hover, int offsetX, int offsetZ) {
        for (int x = 0; x < WIDTH; x++) {
            int[] colors = this.colors[x];
            for (int z = 0; z < WIDTH; z++) {
                frame.fillDot(context, x + offsetX, z + offsetZ, colors[z]);
                
                if (this.hasOwner())
                    frame.fillDot(context, x + offsetX, z + offsetZ, CLAIM_HIGHLIGHT);
                if (hover)
                    frame.fillDot(context, x + offsetX, z + offsetZ, HOVER_HIGHLIGHT);
            }
        }
    }
    
    /**
     * Convert the MapColor to an ARGB color used for the GUI
     * @param color The MapColor
     * @param brightness The brightness used at a position
     * @return
     */
    private int getRenderColor(@NotNull MapColor color, MapColor.@NotNull Brightness brightness) {
        if (color == MapColor.CLEAR) {
            return 0;
        } else {
            int i = brightness.brightness;
            return ColorHelper.Argb.getArgb(
                255,
                (color.color >> 16 & 255) * i / 255,
                (color.color >> 8 & 255) * i / 255,
                (color.color & 255) * i / 255
            );
        }
    }
    
    public boolean hasOwner() {
        return this.owner != null;
    }
    
    public @NotNull Text getOwner() {
        if (this.owner == null)
            return Text.literal("Wilderness");
        return this.owner;
    }
    
    private @NotNull BlockState getFluidStateIfVisible(@NotNull World world, @NotNull BlockState state, @NotNull BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
    }
}
