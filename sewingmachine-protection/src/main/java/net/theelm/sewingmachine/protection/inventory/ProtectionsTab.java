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

package net.theelm.sewingmachine.protection.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.objects.ScreenTab;
import net.theelm.sewingmachine.objects.Tab;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.screen.ProtectionScreen;
import net.theelm.sewingmachine.screens.SettingScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Jul 03 2023 at 4:00 PM.
 * By greg in sewingmachine
 */
public class ProtectionsTab extends ScreenTab {
    private final @NotNull Text text;
    private final @NotNull ItemStack icon;
    
    public ProtectionsTab(@NotNull MinecraftClient client) {
        super(client);
        this.text = Text.literal("Protections");
        this.icon = new ItemStack(Items.CARTOGRAPHY_TABLE);
    }
    
    @Override
    public @NotNull ItemStack getIcon() {
        return this.icon;
    }
    
    @Override
    public @NotNull Text getText() {
        return this.text;
    }
    
    @Override
    public boolean isVisible() {
        return ((PlayerClaimData) this.client).hasClaim();
    }
    
    @Override
    public boolean isActive() {
        return this.client.currentScreen instanceof SettingScreen;
    }
    
    @Override
    public boolean canClick() {
        return super.canClick()
            || !(this.client.currentScreen instanceof ProtectionScreen);
    }
    
    @Override
    protected @Nullable Screen getScreen() {
        if (this.client.currentScreen instanceof SettingScreen screen) {
            Screen parent = screen.parent;
            if (parent != null)
                return parent;
        }
        return new ProtectionScreen(this.text);
    }
}
