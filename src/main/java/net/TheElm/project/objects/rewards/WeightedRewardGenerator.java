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

package net.TheElm.project.objects.rewards;

import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Created on Aug 22 2021 at 6:23 PM.
 * By greg in SewingMachineMod
 */
public class WeightedRewardGenerator extends WeightedReward {
    private @NotNull Text text;
    private @NotNull Function<PlayerEntity, ItemStack> function;

    public WeightedRewardGenerator(int weight, @NotNull String name, @NotNull Function<PlayerEntity, ItemStack> function) {
        this(weight, new LiteralText(name), function);
    }
    public WeightedRewardGenerator(int weight, @NotNull Text name, @NotNull Function<PlayerEntity, ItemStack> function) {
        super(weight);
        this.text = name;
        this.function = function;
    }
    
    public @NotNull Function<PlayerEntity, ItemStack> getFunction() {
        return this.function;
    }
    
    @Override
    public @Nullable RewardContext create(@Nullable PlayerEntity player) {
        ItemStack stack = this.function.apply(player);
        return new RewardContext(MessageUtils.detailedItem(stack), stack);
    }
    
    @Override
    public @NotNull Text asText() {
        return this.text;
    }
    
    @Override
    public boolean isRewardEquals(@NotNull WeightedReward reward) {
        return (reward instanceof WeightedRewardGenerator rewardGenerator)
            && rewardGenerator.getFunction() == this.getFunction();
    }
    
    @Override
    public int getRewardHash() {
        return Objects.hash(this.function);
    }
}
