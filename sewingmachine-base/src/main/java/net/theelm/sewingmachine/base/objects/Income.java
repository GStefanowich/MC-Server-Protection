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

package net.theelm.sewingmachine.base.objects;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created on Jun 28 2023 at 11:07 PM.
 * By greg in sewingmachine
 */
public final class Income {
    private final @NotNull ServerPlayerEntity player;
    private final int value;
    private final @NotNull List<Tax> taxes = new ArrayList<>();
    
    private int untaxed;
    
    public Income(@NotNull ServerPlayerEntity player, int value) {
        this.player = player;
        this.value = value;
        this.untaxed = value;
    }
    
    public void addTax(Text name, int percentage) {
        int clamp = MathHelper.clamp(percentage, 0, 100);
        float f = (float) clamp / 100;
        int amount = MathHelper.clamp(Math.round(f * this.untaxed), 0, this.untaxed);
        
        // Add the tax to the list
        this.untaxed -= amount;
        this.taxes.add(new Tax(name, amount, clamp));
    }
    public @NotNull Collection<Tax> getTaxes() {
        return this.taxes;
    }
    public @NotNull ServerPlayerEntity getPlayer() {
        return this.player;
    }
    
    public int getValue() {
        return this.value;
    }
    public int getValueAfterTaxes() {
        return this.untaxed;
    }
}
