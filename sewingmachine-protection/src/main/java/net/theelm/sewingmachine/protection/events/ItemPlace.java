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

package net.theelm.sewingmachine.protection.events;

import net.theelm.sewingmachine.interfaces.BlockPlaceCallback;
import net.theelm.sewingmachine.protection.utilities.ChunkUtils;
import net.theelm.sewingmachine.utilities.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class ItemPlace {
    
    private ItemPlace() {}
    
    /**
     * Initialize our callback listener for Item Usage
     */
    public static void init() {
        BlockPlaceCallback.EVENT.register(ItemPlace::blockPlace);
    }
    
    private static @NotNull ActionResult blockPlace(final ServerPlayerEntity player, final World world, final BlockPos blockPos, final Direction direction, final ItemStack stack) {
        if (!ChunkUtils.canPlayerDoInChunk(ItemUtils.getPermission(stack), player, blockPos.offset( direction ) ))
            return ActionResult.FAIL;
        return ActionResult.PASS;
    }
    
}
