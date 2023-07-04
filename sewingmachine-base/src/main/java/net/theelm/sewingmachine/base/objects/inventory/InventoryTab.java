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

package net.theelm.sewingmachine.base.objects.inventory;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.objects.Tab;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class InventoryTab extends Tab {
    private final @NotNull MinecraftClient client;
    private final @NotNull Text text;
    private final @NotNull ItemStack icon;
    
    public InventoryTab(@NotNull MinecraftClient client) {
        this.client = client;
        this.text = Text.translatable("container.inventory");
        
        GameProfile profile = client.getSession()
            .getProfile();
        this.icon = new ItemStack(Items.PLAYER_HEAD);
        NbtCompound nbt = this.icon.getOrCreateNbt();
        nbt.putString("SkullOwner", profile.getName());
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
    public boolean isEnabled() {
        return this.client.player != null;
    }
    
    @Override
    public boolean isActive() {
        return this.client.currentScreen instanceof InventoryScreen;
    }
    
    @Override
    public void setActive() {
        ClientPlayerEntity player = this.client.player;
        if (player != null)
            this.client.setScreen(new InventoryScreen(player));
        else this.client.setScreen(null);
    }
}
