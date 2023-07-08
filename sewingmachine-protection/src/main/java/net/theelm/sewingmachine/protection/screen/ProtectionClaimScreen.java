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
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.protection.interfaces.ClientClaimData;
import net.theelm.sewingmachine.protection.objects.FrameData;
import net.theelm.sewingmachine.protection.objects.MapChunk;
import net.theelm.sewingmachine.protection.objects.MapWidget;
import net.theelm.sewingmachine.screens.SettingScreen;
import net.theelm.sewingmachine.screens.SettingScreenListWidget;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.mod.Sew;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
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
    private @Nullable Text claimsText = null;
    private int ticks = 0;
    private int claims = -1;
    
    protected ProtectionClaimScreen(@NotNull Text title) {
        super(title, Sew.modIdentifier("textures/gui/settings_map.png"));
        
        this.frame = new FrameData(
            this.backgroundWidth,
            this.backgroundHeight
        );
        
        this.chunkX = Math.round(this.frame.width() / (MapChunk.WIDTH * this.scale));
        this.chunkY = Math.round(this.frame.height() / (MapChunk.WIDTH * this.scale));
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        PlayerEntity player = this.client.player;
        
        this.frame.x = this.x;
        this.frame.y = this.y;
        this.widget = (player != null)
            ? new MapWidget(this.client, player.getChunkPos(), this.frame, this.chunkX, this.chunkY) : null;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Run the parent render
        super.render(context, mouseX, mouseY, delta);
        
        if (this.widget != null) {
            if (this.ticks-- <= 0) {
                this.ticks = 60;
                this.widget.update();
                this.claims = -1;
            }
            
            ClientClaimData claimData = (ClientClaimData) this.client;
            int current = claimData.getClaimedChunks();
            if (this.claims != current) {
                int limit = claimData.getMaxChunks();
                this.claimsText = TextUtils.literal()
                    .append(MessageUtils.formatNumber(current, current == limit ? Formatting.RED : Formatting.GREEN))
                    .append(" / ")
                    .append(MessageUtils.formatNumber(limit, Formatting.WHITE));
                this.claims = current;
            }
            
            // Draw the owner tooltip if hovering over a chunk
            MapChunk hovered = this.widget.render(context, mouseX, mouseY);
            if (hovered != null)
                context.drawTooltip(this.textRenderer, hovered.getName(), mouseX, mouseY);
            
            // Draw the number of claims that a player has available
            if (this.claimsText != null)
                context.drawTextWithShadow(this.textRenderer, this.claimsText, this.x, this.y + this.backgroundHeight, ColorUtils.Argb.WHITE);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (this.widget != null && this.widget.mouseClicked(mouseX - this.frame.padding(), mouseY - this.frame.padding(), button))
            || super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return (this.widget != null && this.widget.mouseReleased(mouseX - this.frame.padding(), mouseY - this.frame.padding(), button))
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
