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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.packets.ClaimChunkPacket;
import net.theelm.sewingmachine.protection.packets.ClaimQueryPacket;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import net.theelm.sewingmachine.utilities.mod.Sew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class MapWidget {
    public static final @NotNull Identifier IDENTIFIER = Sew.modIdentifier("map_widget");
    
    private final @NotNull MinecraftClient client;
    private final @NotNull NativeImageBackedTexture texture;
    private final @NotNull FrameData frame;

    /**
     * The number of chunks to have in the X (Width) direction
     */
    private final int chunksX;

    /**
     * The number of chunks to have in the Z (Height) direction
     */
    private final int chunksZ;
    
    /**
     * The pixel width of a single chunk
     */
    private final int chunkWidth;
    
    /**
     * The pixel height of a single chunk
     */
    private final int chunkHeight;
    
    private final @NotNull ChunkPos startChunk;
    private final MapChunk[][] chunks;
    
    private int mouseDrag = 0;
    private boolean mouseOne = false;
    private boolean mouseTwo = false;
    
    public MapWidget(@NotNull MinecraftClient client, @NotNull ChunkPos chunkPos, @NotNull FrameData frame, int chunksX, int chunksZ) {
        this.client = client;
        this.frame = frame;
        
        this.chunks = new MapChunk[chunksX][chunksZ];
        this.chunksX = chunksX;
        this.chunksZ = chunksZ;
        
        this.chunkWidth = Math.round((float)this.frame.width() / this.chunksX);
        this.chunkHeight = Math.round((float)this.frame.height() / this.chunksZ);
        
        System.out.println("Each chunk is " + this.chunkWidth + "x" + this.chunkHeight + " pixels");
        
        this.startChunk = new ChunkPos(
            chunkPos.x - Math.round((float) chunksX / 2) + 1,
            chunkPos.z - Math.round((float) chunksZ / 2) + 1
        );
        this.texture = new NativeImageBackedTexture(this.chunksX * MapChunk.WIDTH, this.chunksZ * MapChunk.WIDTH, true);
    }
    
    public void update() {
        ClientWorld world = this.getWorld();
        if (world == null)
            return;
        for (int x = 0; x < this.chunksX; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunksZ; z++) {
                MapChunk data = inner[z];
                if (data != null)
                    data.update(x * MapChunk.WIDTH, z * MapChunk.WIDTH);
                else {
                    Chunk chunk = world.getChunk(this.startChunk.x + x, this.startChunk.z + z, ChunkStatus.EMPTY, false);
                    if (chunk instanceof WorldChunk worldChunk) {
                        MapChunk mapChunk = new MapChunk(worldChunk);
                        
                        inner[z] = mapChunk;
                        
                        // Update the chunk
                        mapChunk.update(x * MapChunk.WIDTH, z * MapChunk.WIDTH);
                        
                        // Query the server
                        NetworkingUtils.send(this.client, new ClaimQueryPacket(mapChunk.getPos()));
                    }
                }
            }
        }
    }
    
    public @NotNull NativeImageBackedTexture getTexture() {
        return this.texture;
    }
    
    public @Nullable ClientPlayerEntity getPlayer() {
        return this.client.player;
    }
    
    public @Nullable ClientWorld getWorld() {
        return this.client.world;
    }
    
    public @Nullable MapChunk render(@NotNull DrawContext context, int mouseX, int mouseY) {
        MapChunk hovered = null;
        
        // Draw the map
        this.frame.plot(context);
        
        // Draw the hovered chunk and return it
        for (int x = 0; x < this.chunksX; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunksZ; z++) {
                int offsetX = x * this.chunkWidth;
                int offsetZ = z * this.chunkHeight;
                
                boolean isHovered = false;
                if (this.isMouseOver(mouseX, mouseY)) {
                    int relativeX = this.frame.scaleX(mouseX - this.frame.padding());
                    int relativeY = this.frame.scaleY(mouseY - this.frame.padding());
                    
                    isHovered = relativeX >= offsetX
                        && relativeX < offsetX + this.chunkWidth
                        && relativeY >= offsetZ
                        && relativeY < offsetZ + this.chunkHeight;
                }
                
                MapChunk chunk = inner[z];
                if (chunk != null) {
                    if (isHovered)
                        hovered = chunk;
                    chunk.render(context, isHovered, offsetX, offsetZ);
                }
            }
        }
        
        // Draw the player dot
        PlayerEntity player = this.getPlayer();
        if (player != null) {
            BlockPos pos = player.getBlockPos();
            int x = pos.getX() - (this.startChunk.x * MapChunk.WIDTH);
            int z = pos.getZ() - (this.startChunk.z * MapChunk.WIDTH);
            
            // Draw a dot for where the player is standing
            this.frame.fillDot(context, x - 1, z - 1, x + 1, z + 1, ColorUtils.Argb.RED);
        }
        
        return hovered;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY))
            return false;
        if (button == 0 && !this.mouseOne)
            this.mouseOne = true;
        if (button == 1 && !this.mouseTwo)
            this.mouseTwo = true;
        return this.mouseOne || this.mouseTwo;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.mouseOne)
            this.mouseOne = false;
        else if (button == 1 && this.mouseTwo)
            this.mouseTwo = false;
        else return false;
        
        if (this.isMouseOver(mouseX, mouseY)) {
            MapChunk chunk = this.getAtMouse(mouseX, mouseY);
            if (chunk != null) {
                if (button == 0) {
                    this.claimChunk(chunk);
                    return true;
                }
                if (button == 1) {
                    this.unclaimChunk(chunk);
                    return true;
                }
            }
        }
        
        if (!this.mouseOne && !this.mouseTwo)
            this.mouseDrag = 0;
        
        return false;
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX > this.frame.x
            && mouseX < this.frame.x + this.frame.width()
            && mouseY > this.frame.y
            && mouseY < this.frame.y + this.frame.height();
    }
    
    public void mouseMoved(double mouseX, double mouseY) {
        if (
            !(this.mouseOne || this.mouseTwo)
            || this.mouseOne == this.mouseTwo
            || this.mouseDrag++ < 20
            || !this.isMouseOver(mouseX, mouseY)
        ) return;
        this.mouseDrag = 0;
        
        MapChunk chunk = this.getAtMouse(mouseX, mouseY);
        if (chunk == null)
            return;
        
        if (this.mouseOne)
            this.claimChunk(chunk);
        
        if (this.mouseTwo)
            this.unclaimChunk(chunk);
    }
    
    /**
     * Send a request to the server to claim the chunk
     * @param chunk
     */
    public void claimChunk(@NotNull MapChunk chunk) {
        if (chunk.hasOwner())
            return;
        NetworkingUtils.send(this.client, new ClaimChunkPacket(chunk.getPos(), true));
    }
    
    /**
     * Send a request to the server to unclaim the chunk
     * @param chunk
     */
    public void unclaimChunk(@NotNull MapChunk chunk) {
        ClaimantPlayer owner = chunk.getOwner();
        if (owner == null)
            return;
        UUID self = this.client.getSession()
            .getUuidOrNull();
        if (!Objects.equals(self, owner.getId()))
            return;
        NetworkingUtils.send(this.client, new ClaimChunkPacket(chunk.getPos(), false));
    }
    
    public @Nullable MapChunk getAtMouse(double mouseX, double mouseY) {
        int relativeX = this.frame.scaleX(mouseX);
        int relativeY = this.frame.scaleY(mouseY);
        int x = BlockUtils.chunkPos(relativeX);
        int z = BlockUtils.chunkPos(relativeY);
        if (x >= this.chunksX || z >= this.chunksZ)
            return null;
        return this.chunks[x][z];
    }
    
    public final class MapChunk {
        // The number of blocks square that a chunk is
        public static final int WIDTH = 16;
        public static final int HOVER_HIGHLIGHT;
        public static final int CLAIM_HIGHLIGHT;
        static {
            Color color;
            
            color = Color.BLACK;
            HOVER_HIGHLIGHT = ColorHelper.Argb.getArgb(/*65*/ 255, color.getRed(), color.getGreen(), color.getBlue());
            
            color = Color.CYAN;
            CLAIM_HIGHLIGHT = ColorHelper.Abgr.getAbgr(80, color.getBlue(), color.getGreen(), color.getRed());
        }
        
        private final @NotNull World world;
        private final @NotNull WorldChunk chunk;
        
        private final boolean hasCeiling;
        
        public MapChunk(@NotNull WorldChunk chunk) {
            this.chunk = chunk;
            this.world = chunk.getWorld();
            this.hasCeiling = world.getDimension()
                .hasCeiling();
        }
        
        public void update(int offsetX, int offsetZ) {
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            BlockPos.Mutable mutable2 = new BlockPos.Mutable();
            if (this.chunk.isEmpty())
                return;
            
            boolean isOwned = this.hasOwner();
            NativeImage image = MapWidget.this.texture.getImage();
            
            for (int x = MapChunk.WIDTH - 1; x >= 0; x--) {
                int posX = offsetX + x;
                double d = 0.0;
                
                //int[] colors = this.colors[x];
                for (int z = MapChunk.WIDTH - 1; z >= 0; z--) {
                    int posZ = offsetZ + z;
                    
                    Multiset<MapColor> multiset = LinkedHashMultiset.create();
                    int t = 0;
                    double e = 0.0;
                    if (this.hasCeiling) {
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
                        
                        // If no block was found above void
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
                        if (f < 0.5) brightness = MapColor.Brightness.HIGH;
                        else if (f > 0.9) brightness = MapColor.Brightness.LOW;
                        else brightness = MapColor.Brightness.NORMAL;
                    } else {
                        f = (e - d) * 4.0 / (double)(4) /*+ ((double)(x + z & 1) - 0.5) * 0.4*/;
                        if (f > 0.6) brightness = MapColor.Brightness.HIGH;
                        else if (f < -0.6) brightness = MapColor.Brightness.LOW;
                        else brightness = MapColor.Brightness.NORMAL;
                    }
                    
                    d = e;
                    image.setColor(posX, posZ, this.getRenderColor(mapColor, brightness));
                    if (isOwned)
                        image.blend(posX, posZ, CLAIM_HIGHLIGHT);
                }
            }
            
            MapWidget.this.texture.upload();
        }
        
        public void render(@NotNull DrawContext context, boolean hover, int offsetX, int offsetZ) {
            if (hover)
                MapWidget.this.frame.fillDot(context, offsetX, offsetZ, offsetX + WIDTH, offsetZ + WIDTH, HOVER_HIGHLIGHT);
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
                return ColorHelper.Abgr.getAbgr(
                    255,
                    (color.color & 255) * i / 255,
                    (color.color >> 8 & 255) * i / 255,
                    (color.color >> 16 & 255) * i / 255
                );
            }
        }
        
        public boolean hasOwner() {
            return this.getOwner() != null;
        }
        
        public @Nullable ClaimantPlayer getOwner() {
            return ((IClaimedChunk) this.chunk).getOwner();
        }
        
        public @NotNull Text getName() {
            ClaimantPlayer owner = this.getOwner();
            if (owner == null)
                return Text.literal("Wilderness");
            return owner.getName();
        }
        
        public @NotNull ChunkPos getPos() {
            return this.chunk.getPos();
        }
        
        private @NotNull BlockState getFluidStateIfVisible(@NotNull World world, @NotNull BlockState state, @NotNull BlockPos pos) {
            FluidState fluidState = state.getFluidState();
            return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
        }
    }
}
