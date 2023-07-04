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

package net.theelm.sewingmachine.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.interfaces.TabRegistry;
import net.theelm.sewingmachine.objects.Tab;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created on Jul 04 2023 at 12:13 AM.
 * By greg in sewingmachine
 */
public abstract class SettingScreen extends Screen {
    public static final Identifier BACKGROUND_TEXTURE = Sew.modIdentifier("textures/gui/settings.png");
    
    protected int backgroundWidth = 176;
    protected int backgroundHeight = 166;
    
    protected int x;
    protected int y;
    
    private float mouseX;
    private float mouseY;
    
    protected SettingScreen(Text title) {
        super(title);
    }
    
    @Override
    protected void init() {
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Draw the tinted background
        this.renderBackground(context);
        
        int i = this.x;
        int j = this.y;
        
        // Draw background tabs
        this.drawScreenTabs(context, delta, mouseX, mouseY, false);
        
        // Draw the background
        this.drawBackground(context, mouseX, mouseY, delta);
        
        // Draw foreground tabs
        this.drawScreenTabs(context, delta, mouseX, mouseY, true);
        
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
    
    private void drawBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        int i = this.x;
        int j = this.y;
        context.drawTexture(BACKGROUND_TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private void drawScreenTabs(DrawContext context, float delta, int mouseX, int mouseY, boolean front) {
        Collection<Tab> tabs = ((TabRegistry) this.client).getTabs();
        Iterator<Tab> iterator = tabs.iterator();
        for (int col = 0; iterator.hasNext(); col++) {
            Tab tab = iterator.next();
            
            boolean selected = tab.isActive();
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
            
            if (selected == front) {
                context.drawTexture(Tab.TEXTURE, x, y, left, top, 26, 32);
                context.getMatrices().push();
                context.getMatrices().translate(0.0f, 0.0f, 100.0f);
                
                int n2 = topRow ? 1 : -1;
                ItemStack itemStack = tab.getIcon();
                context.drawItem(itemStack, x += 5, y += 8 + n2);
                context.drawItemInSlot(this.textRenderer, itemStack, x, y);
                context.getMatrices().pop();
            }
            
            // If hovering the tab
            if (front && this.isPointWithinBounds((col * 27) + 3, -27, 21, 27, mouseX, mouseY))
                context.drawTooltip(this.textRenderer, tab.getText(), mouseX, mouseY);
        }
    }
    
    private boolean checkScreenTabs(double mouseX, double mouseY, boolean activate) {
        Collection<Tab> tabs = ((TabRegistry) this.client).getTabs();
        Iterator<Tab> iterator = tabs.iterator();
        
        for (int col = 0; iterator.hasNext(); col++) {
            Tab tab = iterator.next();
            
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
    
    protected boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        int i = this.x;
        int j = this.y;
        return (pointX -= (double)i) >= (double)(x - 1) && pointX < (double)(x + width + 1) && (pointY -= (double)j) >= (double)(y - 1) && pointY < (double)(y + height + 1);
    }
    
    @Override
    public void close() {
        this.client.player.closeHandledScreen();
        super.close();
    }
}
