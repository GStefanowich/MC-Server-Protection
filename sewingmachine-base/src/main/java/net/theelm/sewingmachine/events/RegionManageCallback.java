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
import net.minecraft.server.world.ServerWorld;
import net.theelm.sewingmachine.protections.BlockRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@FunctionalInterface
public interface RegionManageCallback {
    Event<RegionManageCallback> HANDLER = EventFactory.createArrayBacked(
        RegionManageCallback.class,
        (world, player, region, claimed) -> true,
        (listeners) -> (world, player, region, claimed) -> {
            for (RegionManageCallback callback : listeners) {
                boolean result = callback.tryUpdate(world, player, region, claimed);
                if (result)
                    return true;
            }
            
            return false;
        }
    );
    
    boolean tryUpdate(@NotNull ServerWorld world, @Nullable UUID player, @NotNull BlockRange region, boolean claimed);
    
    static boolean tryClaim(@NotNull ServerWorld world, @Nullable UUID player, @NotNull BlockRange region) {
        return RegionManageCallback.HANDLER.invoker()
            .tryUpdate(world, player, region, true);
    }
    static boolean tryUnclaim(@NotNull ServerWorld world, @Nullable UUID player, @NotNull BlockRange region) {
        return RegionManageCallback.HANDLER.invoker()
            .tryUpdate(world, player, region, false);
    }
}
