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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class FrameData {
    private final float scale = 1.5f;
    private final int padding;
    
    private final int width;
    private final int height;
    
    public int x = 0;
    public int y = 0;
    
    public FrameData(int width, int height) {
        this(width, height, 4);
    }
    public FrameData(int width, int height, int padding) {
        this.padding = padding;
        this.width = width - this.fullPadding();
        this.height = height - this.fullPadding();
    }
    
    public int width() {
        return this.width;
    }
    public int height() {
        return this.height;
    }
    
    public float scale() {
        return this.scale;
    }
    public int scaleX(int i) {
        return Math.round((i - this.x) / this.scale);
    }
    public int scaleX(double i) {
        return Math.round((float)(i - this.x) / this.scale);
    }
    public int scaleY(int i) {
        return Math.round((i - this.y) / this.scale);
    }
    public int scaleY(double i) {
        return Math.round((float)(i - this.y) / this.scale);
    }
    
    public int padding() {
        return this.padding;
    }
    public int fullPadding() {
        return this.padding * 2;
    }
    
    public void fillDot(@NotNull DrawContext context, int x, int y, int color) {
        this.fillDot(context, x, y, color, false);
    }
    public void fillDot(@NotNull DrawContext context, int x, int y, int color, boolean outline) {
        if (outline && !this.isOutline(x, y))
            return;
        
        x *= this.scale();
        y *= this.scale();
        if (x - this.padding < 0 || y - this.padding < 0 || x + this.padding >= this.width() || y + this.padding >= this.height())
            return;
        
        x += this.x + this.padding();
        y += this.y + this.padding();
        
        context.fill(
            RenderLayer.getGuiOverlay(),
            x,
            y,
            MathHelper.clamp(Math.round(x + this.scale()), this.x, this.x + this.width() - this.padding()),
            MathHelper.clamp(Math.round(y + this.scale()), this.y, this.y + this.height() - this.padding()),
            color
        );
    }
    public void fillDot(@NotNull DrawContext context, int x1, int y1, int x2, int y2, int color) {
        x1 *= this.scale();
        x1 += this.x + this.padding();
        y1 *= this.scale();
        y1 += this.y + this.padding();
        x2 *= this.scale();
        x2 += this.x + this.padding();
        y2 *= this.scale();
        y2 += this.y + this.padding();
        
        // Fill in
        context.fill(RenderLayer.getGuiOverlay(), x1, y1, x2, y2, color);
    }
    
    private boolean isOutline(int x, int y) {
        int modX = x % MapChunk.WIDTH;
        int modY = y % MapChunk.WIDTH;
        return modX == 0 || modX == 15
            || modY == 0 || modY == 15;
    }
}
