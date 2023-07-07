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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.interfaces.TabRegistry;
import net.theelm.sewingmachine.objects.TabContext;
import net.theelm.sewingmachine.objects.widgets.CycleWidget;
import net.theelm.sewingmachine.objects.widgets.ToggleWidget;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public abstract class SettingScreen extends Screen {
    public static final @NotNull Identifier BACKGROUND_TEXTURE = Sew.modIdentifier("textures/gui/settings.png");
    
    protected final int backgroundWidth = 176;
    protected final int backgroundHeight = 166;
    
    protected int x;
    protected int y;
    
    private float mouseX;
    private float mouseY;
    
    public @Nullable Screen parent;
    private @NotNull TabContext tabContext;
    private @NotNull SettingScreenListWidget listWidget;
    
    protected SettingScreen(Text title) {
        super(title);
    }
    
    @Override
    protected final void init() {
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
        
        final int pad = 4;
        
        this.tabContext = ((TabRegistry) this.client).getTabs(this.textRenderer, this.x, this.y, this.backgroundHeight);
        this.listWidget = new SettingScreenListWidget(
            this.client, // The minecraft client
            this,
            this.y + pad, // Position against top of screen
            this.height - this.y - pad, // Postition against bottom of screen
            24 // Total vertical height given to each component (Height + padding)
        );
        
        this.addDrawableChild(this.listWidget);
        this.addButtons(this.listWidget);
    }
    
    protected void addButtons(@NotNull SettingScreenListWidget list) {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw the tinted background
        this.renderBackground(context);
        
        int i = this.x;
        int j = this.y;
        
        // Draw background tabs
        this.tabContext.draw(context, delta, mouseX, mouseY, false);
        
        // Draw the background
        this.drawBackground(context, mouseX, mouseY, delta);
        
        super.render(context, mouseX, mouseY, delta);
        
        // Draw foreground tabs
        this.tabContext.draw(context, delta, mouseX, mouseY, true);
        
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
    
    private void drawBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        int i = this.x;
        int j = this.y;
        context.drawTexture(BACKGROUND_TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
    
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        return (button == 0 && this.tabContext.checkScreenTabs(mouseX, mouseY, false))
            || super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return (button == 0 && this.tabContext.checkScreenTabs(mouseX, mouseY, true))
            || super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public final boolean shouldPause() {
        return false;
    }
    
    @Override
    public final void close() {
        this.client.setScreen(this.parent);
    }
}
