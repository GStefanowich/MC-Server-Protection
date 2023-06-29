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

package net.theelm.sewingmachine.base.objects.rewards;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Aug 22 2021 at 8:06 PM.
 * By greg in SewingMachineMod
 */
public final class RewardContext {
    private final @NotNull Text text;
    private final @Nullable ItemStack stack;
    private final boolean success;
    
    public RewardContext(@NotNull Text text, boolean success) {
        this.text = text;
        this.success = success;
        this.stack = null;
    }
    public RewardContext(@NotNull Text text, @NotNull ItemStack stack) {
        this.text = text;
        this.stack = stack;
        this.success = true;
    }
    
    public @NotNull Text asText() {
        return this.text;
    }
    
    public boolean hasStack() {
        return this.getStack() != null;
    }
    public @Nullable ItemStack getStack() {
        return this.stack;
    }
    public @Nullable Item getItem() {
        ItemStack stack = this.getStack();
        return stack == null ? null : stack.getItem();
    }
    
    public boolean wasSuccess() {
        return this.success;
    }
}
