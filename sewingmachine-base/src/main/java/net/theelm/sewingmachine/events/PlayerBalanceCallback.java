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

package net.theelm.sewingmachine.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Event for changing the balance of a player
 */
@FunctionalInterface
public interface PlayerBalanceCallback {
    Event<PlayerBalanceCallback> EVENT = EventFactory.createArrayBacked(PlayerBalanceCallback.class, (listeners) -> (player, amount, consume) -> {
        if (listeners.length > 0) {
            // Skip listeners if the amount is ZERO (No meaningful action to take)
            if (amount == 0)
                return Boolean.TRUE;
            
            for (PlayerBalanceCallback callback : listeners) {
                Boolean result = callback.update(player, amount, consume);
                
                // Return if a result was declarative
                if (result != null)
                    return result;
            }
        }
        
        // Default is TRUE only if the amount is ZERO,
        //   otherwise FALSE as no implementation handled the transaction
        return amount == 0;
    });
    
    /**
     * Update (determinant by {@code consume}) a players balance by an amount
     * @param player The player that holds the balance
     * @param amount The amount to change the balance by
     * @param consume Whether to actually update the balance or not
     * @return When {@code consume} is TRUE, if the balance has been updated. If {@code consume} is FALSE, if the player has the balance necessary
     */
    Boolean update(@NotNull PlayerEntity player, int amount, boolean consume);
    
    /**
     * Test if the player has a balance without taking from it
     * @param player The player balance to check
     * @param amount The amount to check if the player has
     * @return If the player balance is at least that amount
     */
    default boolean hasBalance(@NotNull PlayerEntity player, int amount) {
        return this.update(player, amount, false) == Boolean.TRUE;
    }
    
    /**
     * Give money to a player
     * @param player The player balance to check
     * @param amount The amount to give to the player
     */
    default void give(@NotNull PlayerEntity player, int amount) {
        this.update(player, -amount, true);
    }
    
    /**
     * Take money from a player
     * @param player The player balance to check
     * @param amount The amount to take from the player
     */
    default void take(@NotNull PlayerEntity player, int amount) {
        this.update(player, amount, true);
    }
}
