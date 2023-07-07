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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class MapWidget {
    private final @NotNull World world;
    private final @NotNull FrameData frame;
    
    private final int chunkWidth;
    private final int chunkHeight;
    
    private final int chunkX;
    private final int chunkZ;
    
    private final MapChunk[][] chunks;
    
    private boolean mouseOne = false;
    private boolean mouseTwo = false;
    
    public MapWidget(@NotNull World world, @NotNull ChunkPos chunkPos, @NotNull FrameData frame, int chunkWidth, int chunkHeight) {
        this.world = world;
        this.frame = frame;
        
        this.chunks = new MapChunk[chunkWidth][chunkHeight];
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        
        this.chunkX = chunkPos.x - Math.round((float)chunkWidth / 2);
        this.chunkZ = chunkPos.z - Math.round((float)chunkHeight / 2);
    }
    
    public void update() {
        for (int x = 0; x < this.chunkWidth; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunkHeight; z++) {
                MapChunk data = inner[z];
                if (data != null)
                    data.update();
                else {
                    Chunk chunk = this.world.getChunk(this.chunkX + x, this.chunkZ + z, ChunkStatus.EMPTY, false);
                    if (chunk instanceof WorldChunk worldChunk) {
                        MapChunk mapChunk = new MapChunk(worldChunk);
                        
                        inner[z] = mapChunk;
                        
                        mapChunk.update();
                    }
                }
            }
        }
    }
    
    public @Nullable MapChunk render(@NotNull DrawContext context, int mouseX, int mouseY) {
        MapChunk hovered = null;
        
        for (int x = 0; x < this.chunkWidth; x++) {
            MapChunk[] inner = this.chunks[x];
            for (int z = 0; z < this.chunkHeight; z++) {
                int offsetX = x * MapChunk.WIDTH;
                int offsetZ = z * MapChunk.WIDTH;
                
                int relativeX = this.frame.scaleX(mouseX);
                int relativeY = this.frame.scaleY(mouseY);
                
                boolean isHovered = relativeX > offsetX
                    && relativeX < offsetX + MapChunk.WIDTH
                    && relativeY > offsetZ
                    && relativeY < offsetZ + MapChunk.WIDTH;
                
                MapChunk chunk = inner[z];
                if (chunk != null) {
                    if (isHovered)
                        hovered = chunk;
                    chunk.render(context, this.frame, isHovered, offsetX, offsetZ);
                }
            }
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

        System.out.println("Pressed button " + button);
        
        return true;
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.mouseOne)
            this.mouseOne = false;
        else if (button == 1 && this.mouseTwo)
            this.mouseTwo = false;
        if (this.isMouseOver(mouseX, mouseY)) {
            System.out.println("Released button " + button);
            return true;
        }
        
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
            !this.mouseOne
            || !this.mouseTwo
            || this.mouseOne == this.mouseTwo
        ) return;
        
        MapChunk chunk = this.getAtMouse(mouseX, mouseY);
        if (chunk == null)
            return;
        
        if (this.mouseOne) {
            
        }
        
        if (this.mouseTwo) {
            
        }
    }
    
    public @Nullable MapChunk getAtMouse(double mouseX, double mouseY) {
        int relativeX = this.frame.scaleX(mouseX);
        int relativeY = this.frame.scaleY(mouseY);
        
        return null;
    }
}
