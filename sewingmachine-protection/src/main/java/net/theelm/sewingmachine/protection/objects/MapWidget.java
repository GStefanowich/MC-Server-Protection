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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.packets.ClaimChunkPacket;
import net.theelm.sewingmachine.protection.packets.ClaimQueryPacket;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class MapWidget {
    private final @NotNull MinecraftClient client;
    private final @NotNull FrameData frame;
    
    private final int chunkWidth;
    private final int chunkHeight;
    
    private final int chunkX;
    private final int chunkZ;
    
    private final MapChunk[][] chunks;
    
    private int mouseDrag = 0;
    private boolean mouseOne = false;
    private boolean mouseTwo = false;
    
    public MapWidget(@NotNull MinecraftClient client, @NotNull ChunkPos chunkPos, @NotNull FrameData frame, int chunkWidth, int chunkHeight) {
        this.client = client;
        this.frame = frame;
        
        this.chunks = new MapChunk[chunkWidth][chunkHeight];
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        
        this.chunkX = chunkPos.x - Math.round((float) chunkWidth / 2) + 1;
        this.chunkZ = chunkPos.z - Math.round((float) chunkHeight / 2) + 1;
    }
    
    public void update() {
        ClientWorld world = this.getWorld();
        if (world == null)
            return;
        for (int x = 0; x < this.chunkWidth; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunkHeight; z++) {
                MapChunk data = inner[z];
                if (data != null)
                    data.update();
                else {
                    Chunk chunk = world.getChunk(this.chunkX + x, this.chunkZ + z, ChunkStatus.EMPTY, false);
                    if (chunk instanceof WorldChunk worldChunk) {
                        MapChunk mapChunk = new MapChunk(worldChunk);
                        
                        inner[z] = mapChunk;
                        
                        // Update the chunk
                        mapChunk.update();
                        
                        // Query the server
                        NetworkingUtils.send(this.client, new ClaimQueryPacket(mapChunk.getPos()));
                    }
                }
            }
        }
    }
    
    public @Nullable ClientPlayerEntity getPlayer() {
        return this.client.player;
    }
    
    public @Nullable ClientWorld getWorld() {
        return this.client.world;
    }
    
    public @Nullable MapChunk render(@NotNull DrawContext context, int mouseX, int mouseY) {
        MapChunk hovered = null;
        
        for (int x = 0; x < this.chunkWidth; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunkHeight; z++) {
                int offsetX = x * MapChunk.WIDTH;
                int offsetZ = z * MapChunk.WIDTH;
                
                boolean isHovered = false;
                if (this.isMouseOver(mouseX, mouseY)) {
                    int relativeX = this.frame.scaleX(mouseX - this.frame.padding());
                    int relativeY = this.frame.scaleY(mouseY - this.frame.padding());
                    
                    isHovered = relativeX >= offsetX
                        && relativeX < offsetX + MapChunk.WIDTH
                        && relativeY >= offsetZ
                        && relativeY < offsetZ + MapChunk.WIDTH;
                }
                
                MapChunk chunk = inner[z];
                if (chunk != null) {
                    if (isHovered)
                        hovered = chunk;
                    chunk.render(context, this.frame, isHovered, offsetX, offsetZ);
                }
            }
        }
        
        PlayerEntity player = this.getPlayer();
        if (player != null) {
            BlockPos pos = player.getBlockPos();
            int x = pos.getX() - (this.chunkX * MapChunk.WIDTH);
            int z = pos.getZ() - (this.chunkZ * MapChunk.WIDTH);
            
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
        if (x >= this.chunkWidth || z >= this.chunkHeight)
            return null;
        return this.chunks[x][z];
    }
}
