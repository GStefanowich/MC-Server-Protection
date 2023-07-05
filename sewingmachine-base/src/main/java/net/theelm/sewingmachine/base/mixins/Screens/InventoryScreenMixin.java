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

package net.theelm.sewingmachine.base.mixins.Screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.interfaces.TabRegistry;
import net.theelm.sewingmachine.objects.TabContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {
    private TabContext tabContext;
    
    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }
    
    @Inject(at = @At("TAIL"), method = "render")
    private void afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        this.tabContext.draw(context, delta, mouseX, mouseY, true);
    }
    
    @Inject(at = @At("TAIL"), method = "init")
    private void afterInit(CallbackInfo callback) {
        this.tabContext = ((TabRegistry) this.client).getTabs(this.textRenderer, this.x, this.y, this.backgroundHeight);
    }
    
    @Inject(at = @At("HEAD"), method = "drawBackground")
    private void beforeDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo callback) {
        // If width has updated, recreate the context
        if (this.tabContext.x() != this.x)
            this.tabContext = ((TabRegistry) this.client).getTabs(this.textRenderer, this.x, this.y, this.backgroundHeight);
        
        this.tabContext.draw(context, delta, mouseX, mouseY, false);
    }
    
    @Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == 0 && this.tabContext.checkScreenTabs(mouseX, mouseY, false))
            callback.setReturnValue(Boolean.TRUE);
    }
    
    @Inject(at = @At("HEAD"), method = "mouseReleased", cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button == 0 && this.tabContext.checkScreenTabs(mouseX, mouseY, this.handler.getCursorStack().isEmpty()))
            callback.setReturnValue(Boolean.TRUE);
    }
}
