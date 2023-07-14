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

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.theelm.sewingmachine.enums.Test;
import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.theelm.sewingmachine.events.ContainerAccessCallback;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created on Jul 14 2023 at 12:21 AM.
 * By greg in sewingmachine
 */
public final class ItemFrameTransfer {
    private ItemFrameTransfer() {}
    
    /**
     * Initialize our callback listener for Entity Attacks
     */
    public static void register(@NotNull Event<DamageEntityCallback> event) {
        event.register(ItemFrameTransfer::attack);
    }
    
    private static Test attack(@NotNull final Entity target, @NotNull final World world, @NotNull final DamageSource source) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player))
            return Test.CONTINUE;
        
        // Do special item frame interaction if NOT CROUCHING and HOLDING A TOOL
        if ((!(target instanceof ItemFrameEntity itemFrame)) || (player.isSneaking() && player.getMainHandStack().isDamageable()))
            return Test.CONTINUE;
        
        Direction direction = itemFrame.getHorizontalFacing().getOpposite();
        
        // Get the item in the item frame
        ItemStack itemStack = itemFrame.getHeldItemStack();
        
        // If the sign actually has an item on it
        if (!itemStack.isEmpty()) {
            // Get blocks
            BlockPos containerPos = itemFrame.getBlockPos().offset(direction, 1);
            Block containerBlock = world.getBlockState(containerPos).getBlock();
            
            // If the block behind the item frame is a storage
            if (containerBlock instanceof ChestBlock || containerBlock instanceof BarrelBlock) {
                // Check chunk permissions
                if (!ContainerAccessCallback.canAccess(player, containerPos))
                    return Test.FAIL;
                
                Inventory containerInventory = InventoryUtils.getInventoryOf(world, containerPos);
                if (containerInventory != null) {
                    // The amount the player wants to take
                    int takeStackSize = (player.isSneaking() ? Collections.min(Arrays.asList(64, itemStack.getMaxCount())) : 1);
                    
                    InventoryUtils.chestToPlayer(player, containerPos, containerInventory, player.getInventory(), itemStack, takeStackSize);
                    return Test.FAIL;
                }
            }
        }
        
        // If player should be able to interact with the item frame
        if (!BlockBreakCallback.canDestroy(player, (ServerWorld) world, Hand.MAIN_HAND, itemFrame.getBlockPos(), direction, null))
            return Test.FAIL;
        
        return Test.SUCCESS;
    }
}
