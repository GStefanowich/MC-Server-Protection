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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class WeightedReward {
    
    private final int weight;
    
    protected WeightedReward(int weight) {
        this.weight = weight;
    }
    
    public int getWeight() {
        return this.weight;
    }
    
    public abstract @NotNull RewardContext create(@Nullable PlayerEntity player);
    public abstract @NotNull Text asText();
    public abstract boolean isRewardEquals(@NotNull WeightedReward reward);
    public abstract int getRewardHash();
    
    @Override
    public final boolean equals(Object object) {
        if (object == this)
            return true;
        return object instanceof WeightedReward reward
            && reward.getWeight() == this.getWeight()
            && reward.isRewardEquals(this);
    }
    @Override
    public int hashCode() {
        return Objects.hash(
            this.getRewardHash(),
            this.getWeight()
        );
    }
    @Override
    public String toString() {
        return this.asText()
            .getString();
    }
}
