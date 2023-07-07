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

package net.theelm.sewingmachine.protection.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.theelm.sewingmachine.protection.objects.FrameData;
import net.theelm.sewingmachine.protection.objects.MapChunk;
import net.theelm.sewingmachine.protection.objects.MapWidget;
import net.theelm.sewingmachine.screens.SettingScreen;
import net.theelm.sewingmachine.screens.SettingScreenListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ProtectionClaimScreen extends SettingScreen {
    private final float scale = 1.5f;
    private final int padding = 4;
    
    private final int chunkX;
    private final int chunkY;
    
    private final FrameData frame;
    private @Nullable MapWidget widget = null;
    private int ticks = 0;
    
    protected ProtectionClaimScreen() {
        super(Text.literal("Claim chunks"));
        
        this.frame = new FrameData(
            this.backgroundWidth,
            this.backgroundHeight
        );
        
        this.chunkX = Math.round(this.frame.width() / (MapChunk.WIDTH * this.scale));
        this.chunkY = Math.round(this.frame.height() / (MapChunk.WIDTH * this.scale));
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        World world = this.client.world;
        PlayerEntity player = this.client.player;
        
        this.frame.x = this.x;
        this.frame.y = this.y;
        this.widget = (world != null && player != null)
            ? new MapWidget(world, player.getChunkPos(), this.frame, this.chunkX, this.chunkY) : null;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Run the parent render
        super.render(context, mouseX, mouseY, delta);
        
        if (this.widget != null) {
            if (this.ticks-- <= 0) {
                this.ticks = 60;
                this.widget.update();
            }
            
            MapChunk hovered = this.widget.render(context, mouseX, mouseY);
            if (hovered != null)
                context.drawTooltip(this.textRenderer, hovered.getOwner(), mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (this.widget != null && this.widget.mouseClicked(mouseX, mouseY, button))
            || super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return (this.widget != null && this.widget.mouseReleased(mouseX, mouseY, button))
            || super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.widget != null && this.widget.isMouseOver(mouseX, mouseY);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.widget != null)
            this.widget.mouseMoved(mouseX, mouseY);
    }
}
