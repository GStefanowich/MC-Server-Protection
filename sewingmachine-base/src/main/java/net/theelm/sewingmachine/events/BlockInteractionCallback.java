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
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.theelm.sewingmachine.enums.Test;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import org.jetbrains.annotations.NotNull;

public interface BlockInteractionCallback {
    Event<BlockInteractionCallback> EVENT = EventFactory.createArrayBacked( BlockInteractionCallback.class, (listeners) -> (player, world, hand, itemStack, blockHitResult) -> {
        for (BlockInteractionCallback event : listeners) {
            Test result = event.interact(player, world, hand, itemStack, blockHitResult);
            if (result.isConclusive()) {
                if (result == Test.FAIL) {
                    // If the result is a failure resend the player inventory to fix any lost counts
                    InventoryUtils.resendInventory(player);
                    
                    // Resend the information of the neighboring blocks (Fix ghost blocks, multi-block structures [eg; doors], etc)
                    BlockUtils.updateNeighboringBlockStates(player, world, blockHitResult.getBlockPos());
                }
                
                // Return the result of the interaction
                return result;
            }
        }
        
        return Test.CONTINUE;
    });
    
    Test interact(@NotNull ServerPlayerEntity player, World world, Hand hand, ItemStack itemStack, BlockHitResult blockHitResult);
}
