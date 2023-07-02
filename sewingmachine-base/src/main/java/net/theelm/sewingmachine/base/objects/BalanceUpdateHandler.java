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

import net.minecraft.entity.player.PlayerEntity;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.events.PlayerBalanceCallback;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created on Jun 30 2023 at 3:45 PM.
 * By greg in sewingmachine
 */
public final class BalanceUpdateHandler implements PlayerBalanceCallback {
    @Override
    public Boolean update(@NotNull UUID player, int amount, boolean consume) {
        try {
            if (!consume)
                return MoneyUtils.getPlayerMoney(player) - amount > 0;
            if (amount > 0) {
                try {
                    return MoneyUtils.takePlayerMoney(player, amount);
                } catch (NotEnoughMoneyException exception) {
                    return Boolean.FALSE;
                }
            }
            else return MoneyUtils.givePlayerMoney(player, -amount);
        } catch (NbtNotFoundException exception) {
            int adjusted = Math.abs(amount);
            String error;
            if (amount > 0)
                error = "Failed to give " + adjusted + " money to \"" + player + "\"";
            else
                error = "Failed to take " + adjusted + " money from \"" + player + "\"";
            
            CoreMod.logError(error + " (Maybe they haven't joined the server?).");
            return Boolean.FALSE;
        }
    }
    
    @Override
    public Boolean updatePlayer(@NotNull PlayerEntity player, int amount, boolean consume) {
        if (!consume)
            return MoneyUtils.getPlayerMoney(player) - amount > 0;
        if (amount > 0) {
            try {
                return MoneyUtils.takePlayerMoney(player, amount);
            } catch (NotEnoughMoneyException e) {
                return Boolean.FALSE;
            }
        }
        return MoneyUtils.givePlayerMoney(player, -amount);
    }
}
