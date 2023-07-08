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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public abstract class Tab {
    // Texture from CreativeInventoryScreen.TEXTURE
    public static final Identifier TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    
    /**
     * Get the icon for the tab
     * @return The itemstack
     */
    public abstract @NotNull ItemStack getIcon();
    
    /**
     * Get the hovertext for the icon
     * @return Text describing what the button does
     */
    public abstract @NotNull Text getText();
    
    /**
     * If the tab should be visible
     * @return
     */
    public abstract boolean isVisible();
    
    /**
     * If the tab is clickable
     * @return
     */
    public boolean canClick() {
        return !this.isActive();
    }
    
    /**
     * If the tab is currently selected
     * @return
     */
    public abstract boolean isActive();
    
    /**
     * Activate the tab
     */
    public abstract void setActive();
}
