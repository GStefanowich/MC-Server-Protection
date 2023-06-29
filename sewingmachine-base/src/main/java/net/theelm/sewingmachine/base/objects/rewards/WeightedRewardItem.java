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

import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Created on Aug 22 2021 at 6:23 PM.
 * By greg in SewingMachineMod
 */
public class WeightedRewardItem extends WeightedReward {
    private final @NotNull Item reward;
    private final @NotNull BiConsumer<PlayerEntity, ItemStack> consumer;
    
    public WeightedRewardItem(int weight, @NotNull Item reward ) {
        this(weight, reward, (p, s) -> {});
    }
    public WeightedRewardItem(int weight, @NotNull Item reward, @NotNull BiConsumer<PlayerEntity, ItemStack> consumer) {
        super(weight);
        this.reward = reward;
        this.consumer = consumer;
    }
    
    public @NotNull Item getReward() {
        return this.reward;
    }
    
    public @NotNull BiConsumer<PlayerEntity, ItemStack> getConsumer() {
        return this.consumer;
    }
    
    @Override
    public @NotNull RewardContext create(@Nullable PlayerEntity player) {
        ItemStack stack = new ItemStack(this.reward);
        this.consumer.accept(player, stack);
        return new RewardContext(MessageUtils.detailedItem(stack), stack);
    }
    
    @Override
    public @NotNull Text asText() {
        RewardContext item = this.create(null);
        if (item.hasStack())
            return MessageUtils.detailedItem(item.getStack());
        return Text.literal("Null");
    }
    
    @Override
    public boolean isRewardEquals(@NotNull WeightedReward reward) {
        return (reward instanceof WeightedRewardItem rewardItem)
            && rewardItem.getReward() == this.getReward()
            && rewardItem.getConsumer() == this.getConsumer();
    }
    
    @Override
    public int getRewardHash() {
        return Objects.hash(this.reward, this.consumer);
    }
}
