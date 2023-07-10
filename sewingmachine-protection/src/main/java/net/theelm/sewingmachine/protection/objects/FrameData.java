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
import net.theelm.sewingmachine.protection.objects.MapWidget.MapChunk;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class FrameData {
    private final float scale = 1f;
    private final int padding;
    
    private final int width;
    private final int height;
    
    public int x = 0;
    public int y = 0;
    
    public FrameData(int width, int height) {
        this(width, height, 8);
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
    
    public void plot(@NotNull DrawContext context) {
        this.plot(context, this.x + this.padding(), this.y + this.padding());
    }
    public void plot(@NotNull DrawContext context, int x, int y) {
        context.drawTexture(MapWidget.IDENTIFIER, x, y, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
    }
}
