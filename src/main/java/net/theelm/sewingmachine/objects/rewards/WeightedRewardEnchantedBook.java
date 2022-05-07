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

package net.theelm.sewingmachine.objects.rewards;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created on Aug 22 2021 at 6:23 PM.
 * By greg in SewingMachineMod
 */
public class WeightedRewardEnchantedBook extends WeightedReward {
    private final @NotNull Enchantment enchantment;
    private final int level;
    
    public WeightedRewardEnchantedBook(int weight, @NotNull Enchantment enchantment, int level) {
        super(weight);
        this.enchantment = enchantment;
        this.level = level;
    }
    
    public @NotNull Enchantment getEnchantment() {
        return this.enchantment;
    }
    
    public int getLevel() {
        return this.level;
    }
    
    @Override
    public @NotNull RewardContext create(@Nullable PlayerEntity player) {
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(enchantedBook, new EnchantmentLevelEntry(this.enchantment, this.level));
        return new RewardContext(this.asText(), enchantedBook);
    }
    
    @Override
    public @NotNull Text asText() {
        return new TranslatableText(Items.ENCHANTED_BOOK.getTranslationKey())
            .append(" (")
            .append(this.enchantment.getName(this.level))
            .append(")");
    }
    
    @Override
    public boolean isRewardEquals(@NotNull WeightedReward reward) {
        return (reward instanceof WeightedRewardEnchantedBook book)
            && book.getEnchantment() == this.getEnchantment()
            && book.getLevel() == this.getLevel();
    }
    
    @Override
    public int getRewardHash() {
        return Objects.hash(
            this.getEnchantment(),
            this.getLevel()
        );
    }
}
