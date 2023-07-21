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
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Get the name for a Region
 */
@FunctionalInterface
public interface RegionNameCallback {
    Event<RegionNameCallback> EVENT = EventFactory.createArrayBacked(RegionNameCallback.class, (listeners) -> (world, chunkPos, blockPos, entity, nameOnly, strict) -> {
        for (RegionNameCallback callback : listeners) {
            Text text = callback.getName(world, chunkPos, blockPos, entity, nameOnly, strict);
            if (text != null)
                return text;
        }
        return null;
    });
    
    /**
     * Get the name of a region
     * @param world The world
     * @param chunkPos The region chunk
     * @param blockPos The position of the region
     * @param entity The entity located within the region
     * @param nameOnly If no arbitrary text should be added ("TheElm" vs "TheElm's claim")
     * @param strict If a result MUST be returned
     * @return The name of the Region
     */
    @Nullable Text getName(@NotNull World world, @NotNull ChunkPos chunkPos, @Nullable BlockPos blockPos, @Nullable Entity entity, boolean nameOnly, boolean strict);
    
    /**
     * Get the name of a region
     * @param world The world
     * @param pos The position of the region
     * @param entity The entity located within the region
     * @param nameOnly If no arbitrary text should be added ("TheElm" vs "TheElm's claim")
     * @param strict If a result MUST be returned
     * @return The name of the Region
     */
    static @Nullable Text getName(@NotNull World world, @NotNull BlockPos pos, @Nullable Entity entity, boolean nameOnly, boolean strict) {
        return RegionNameCallback.EVENT.invoker()
            .getName(world, new ChunkPos(pos), pos, entity, nameOnly, strict);
    }
    
    /**
     * Get the name of a region
     * @param world The world
     * @param pos The region chunk
     * @param entity The entity located within the region
     * @param nameOnly If no arbitrary text should be added ("TheElm" vs "TheElm's claim")
     * @param strict If a result MUST be returned
     * @return The name of the Region
     */
    static @Nullable Text getName(@NotNull World world, @NotNull ChunkPos pos, @Nullable Entity entity, boolean nameOnly, boolean strict) {
        return RegionNameCallback.EVENT.invoker()
            .getName(world, pos, null, entity, nameOnly, strict);
    }
    
    /**
     * Get the name of a region that an entity is in
     * @param entity The entity to find the location of
     * @param nameOnly If no arbitrary text should be added ("TheElm" vs "TheElm's claim")
     * @param strict If a result MUST be returned
     * @return The name of the Region
     */
    static @Nullable Text getName(@NotNull Entity entity, boolean nameOnly, boolean strict) {
        World world = entity.getWorld();
        if (world.isClient())
            return null;
        return RegionNameCallback.getName(world, entity.getBlockPos(), entity, nameOnly, strict);
    }
}
