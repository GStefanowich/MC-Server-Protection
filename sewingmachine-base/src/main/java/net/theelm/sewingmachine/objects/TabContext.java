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

package net.theelm.sewingmachine.objects;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created on Jul 04 2023 at 12:34 PM.
 * By greg in sewingmachine
 */
public record TabContext(List<Tab> tabs, TextRenderer textRenderer, int x, int y, int backgroundHeight, boolean show) {
    public TabContext(List<Tab> tabs, TextRenderer textRenderer, int x, int y, int backgroundHeight) {
        this(tabs, textRenderer, x, y, backgroundHeight, show(tabs));
    }
    
    public void draw(DrawContext context, float delta, int mouseX, int mouseY, boolean front) {
        if (!this.show)
            return;
        
        for (int col = 0; col < this.tabs.size(); col++) {
            Tab tab = this.tabs.get(col);
            
            boolean selected = tab.isActive();
            if (selected != front)
                continue;
            boolean topRow = true;
            
            int top = 0;
            int left = col * 26;
            
            int x = this.x + (col * 27);
            int y = this.y;
            
            if (selected) {
                top += 32;
            }
            if (topRow) {
                y -= 28;
            } else {
                top += 64;
                y += this.backgroundHeight - 4;
            }
            
            context.drawTexture(Tab.TEXTURE, x, y, left, top, 26, 32);
            context.getMatrices().push();
            context.getMatrices().translate(0.0f, 0.0f, 100.0f);
            
            int n2 = topRow ? 1 : -1;
            ItemStack itemStack = tab.getIcon();
            context.drawItem(itemStack, x += 5, y += 8 + n2);
            context.drawItemInSlot(this.textRenderer, itemStack, x, y);
            context.getMatrices().pop();
        }
        
        if (front) {
            for (int col = 0; col < this.tabs.size(); col++) {
                Tab tab = this.tabs.get(col);
                
                // If hovering the tab
                if (this.isPointWithinBounds((col * 27) + 3, -27, 21, 27, mouseX, mouseY))
                    context.drawTooltip(this.textRenderer, tab.getText(), mouseX, mouseY);
            }
        }
    }
    
    public boolean checkScreenTabs(double mouseX, double mouseY, boolean activate) {
        if (!this.show) // Tabs hidden, so don't try checking for clicks
            return false;
        for (int col = 0; col < this.tabs.size(); col++) {
            Tab tab = this.tabs.get(col);
            
            // Check if the pointer is currently within the bounds of the tab
            if (!this.isPointWithinBounds((col * 27) + 3, -27, 21, 27, mouseX, mouseY))
                continue;
            
            // Call the tab activate
            if (activate && !tab.isActive() && tab.isEnabled())
                tab.setActive();
            
            return true;
        }
        
        return false;
    }
    
    private boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        return (pointX -= this.x) >= (double)(x - 1)
            && pointX < (double)(x + width + 1)
            && (pointY -= this.y) >= (double)(y - 1)
            && pointY < (double)(y + height + 1);
    }
    
    /**
     * If any of the tabs should be shown at all
     * Mainly here so servers that don't have any of the mods won't show these tabs
     * @param tabs A list of all registered Tabs
     * @return If the tab bar should be visible
     */
    private static boolean show(@NotNull List<Tab> tabs) {
        for (Tab tab : tabs) {
            if (tab.isActive())
                continue;
            if (tab.isEnabled())
                return true;
        }
        return false;
    }
}
