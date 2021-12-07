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

import net.TheElm.project.utilities.FormattingUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created on Aug 22 2021 at 6:24 PM.
 * By greg in SewingMachineMod
 */
public class WeightedRewardMoney extends WeightedReward {
    private final long money;
    
    protected WeightedRewardMoney(int weight, long money) {
        super(weight);
        this.money = money;
    }
    
    public long getMoney() {
        return this.money;
    }
    
    @Override
    public @Nullable RewardContext create(@Nullable PlayerEntity player) {
        return new RewardContext(new LiteralText("$" + FormattingUtils.format(this.money)), true);
    }
    
    @Override
    public @NotNull Text asText() {
        return new LiteralText("Money (")
            .append(new LiteralText(String.valueOf(this.money)).formatted(Formatting.AQUA))
            .append(")");
    }
    
    @Override
    public boolean isRewardEquals(@NotNull WeightedReward reward) {
        return reward instanceof WeightedRewardMoney rewardMoney
            && rewardMoney.getMoney() == this.getMoney();
    }
    
    @Override
    public int getRewardHash() {
        return Objects.hash(this.money);
    }
}
