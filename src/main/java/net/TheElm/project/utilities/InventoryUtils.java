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

package net.TheElm.project.utilities;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public final class InventoryUtils {
    
    /*
     * Get the inventory of a block
     */
    @Nullable
    public static Inventory getInventoryOf(@NotNull World world, @NotNull BlockPos blockPos) {
        BlockEntity blockEntity = world.getBlockEntity( blockPos );
        BlockState blockState = world.getBlockState( blockPos );
        Block block = blockState.getBlock();
        
        if ( block instanceof ChestBlock )
            return ChestBlock.getInventory( blockState, world, blockPos, true );
        if ( blockEntity instanceof BarrelBlockEntity )
            return (BarrelBlockEntity) blockEntity;
        return null;
    }
    
    /*
     * Transfer items from a player to a chest
     */
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Item item, final int count) {
        return InventoryUtils.playerToChest( player, playerInventory, chestInventory, item, count, false );
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Item item, final int count, final boolean required) {
        // Check if enough in player inventory
        if ( required && ( playerInventory.countInInv( item ) < count ) )
            return false;
        
        // Get stack size to take from player up to 64
        int itemStackSize = 0;
        
        boolean success;
        
        // If chest has item on the frame
        if ( playerInventory.countInInv( item ) > 0) {
            final int invSize = playerInventory.getInvSize(); // Get the inventory size to iterate over
            
            // Get stack sizes
            for ( int i = 0; i < invSize; i++ ) {
                final ItemStack invItem = playerInventory.getInvStack( i );
                if ( invItem.getItem().equals( item ) ) {
                    int putable = count - itemStackSize; // The amount of items remaining to take a stack from the player
                    // If stack size is full
                    if ( putable <= 0 )
                        break;
                    
                    if ( chestInventory == null ) {
                        final int put = Collections.min(Arrays.asList( invItem.getCount(), putable ));
                        
                        // Increase the amount that we've put into the chest
                        itemStackSize += put;
                        
                        // Set the player inventory item stack size
                        invItem.setCount(invItem.getCount() - put);
                        
                    } else {
                        // Set the amount to give to the chest
                        int chestSize = chestInventory.getInvSize();
                        for (int y = 0; y < chestSize; y++) {
                            // If the players stack is empty, break
                            if (invItem.getCount() <= 0)
                                break;
                            
                            // Get the item from chest Pos Y
                            final ItemStack chestItemStack = chestInventory.getInvStack(y);
                            final Item chestItem = chestItemStack.getItem();
                            
                            // If the slot is AIR or matching type
                            if (chestItem.equals(Items.AIR) || ((chestItemStack.getCount() < chestItem.getMaxCount()) && chestItem.equals(item))) {
                                final int put = Collections.min(Arrays.asList(chestItem.getMaxCount() - chestItemStack.getCount(), invItem.getCount(), putable));
                                if (chestItem.equals(Items.AIR)) {
                                    // Put a copy of the item from the players inventory
                                    final ItemStack clone = invItem.copy();
                                    clone.setCount(put);
                                    
                                    chestInventory.setInvStack(y, clone);
                                } else {
                                    chestItemStack.setCount(chestItemStack.getCount() + put);
                                    
                                }
                                
                                // Set the player inventory item stack size
                                invItem.setCount(invItem.getCount() - put);
                                
                                // Increase the amount that we've put into the chest
                                itemStackSize += put;
                                
                                // Change the remainder that we want to take
                                putable -= put;
                                
                                // If none left to take, break the loop
                                if (putable <= 0) {
                                    success = ( put > 0 );
                                    if (success)
                                        player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f );
                                    return success;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        success = ( itemStackSize > 0 );
        if (success)
            player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f );
        return success;
    }
    
    /*
     * Transfer items from a chest to the player
     */
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Item item, final int count) {
        return InventoryUtils.chestToPlayer( player, chestInventory, playerInventory, item, count, false);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Item item, final int count, final boolean required) {
        // Check if enough in the chest
        if (required && (chestInventory != null) && (chestInventory.countInInv( item ) < 0))
            return false;
        
        // Get stack size to give to player up to 64
        int stackSize = 0;
        
        if ( chestInventory == null) {
            final int maxStack = item.getMaxCount();
            while ( stackSize < count ) {
                int giveCount = Collections.min(Arrays.asList( count - stackSize, maxStack ));
                
                // Create the new itemstack
                ItemStack clone = new ItemStack( item );
                clone.setCount( giveCount );
                
                // Insert items into player inventory
                playerInventory.offerOrDrop( player.getEntityWorld(), clone );
                
                // Send amount given
                stackSize += giveCount;
            }
            
        } else {
            // If chest has item on the frame
            if (chestInventory.countInInv(item) > 0) {
                final int invSize = chestInventory.getInvSize(); // Get the inventory size to iterate over
                
                // Get stack sizes
                for (int i = 0; i < invSize; i++) {
                    final ItemStack chestItem = chestInventory.getInvStack(i);
                    if (chestItem.getItem().equals(item)) {
                        int collectible = count - stackSize; // The amount of items remaining to give the player a full stack
                        // If stack size is full
                        if (collectible <= 0)
                            break;
                        int collect = Collections.min(Arrays.asList( collectible, chestItem.getCount()));
                        
                        // Create a copy of the item
                        final ItemStack clone = chestItem.copy();
                        
                        int slot;
                        // Get a slot for the item
                        slot = playerInventory.getOccupiedSlotWithRoomForStack( clone );
                        if ( slot == -1 ) {
                            clone.setCount(collect);
                            if (!playerInventory.insertStack( clone ))
                                break;
                        } else {
                            final ItemStack inInv = playerInventory.getInvStack( slot );
                            int invStackSize = inInv.getCount();
                            collect = Collections.min(Arrays.asList( collect, inInv.getItem().getMaxCount() - invStackSize ));
                            inInv.setCount( invStackSize + collect );
                        }
                        
                        stackSize += collect;
                        
                        // Set the chest item stack size
                        chestItem.setCount(chestItem.getCount() - collect);
                    }
                }
            }
        }
        
        boolean success = ( stackSize >= count );
        if ( success )
            player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f );
        return success;
    }
    
    public static ItemRarity getItemRarity(ItemStack stack) {
        float total = 0.0f;
        
        // Get all enchantments
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments( stack );
        for ( Map.Entry<Enchantment, Integer> e : enchantments.entrySet() ) {
            Enchantment enchantment = e.getKey();
            
            // If the enchantment is a curse, the tool is ALWAYS cursed
            if (enchantment.isCursed()) return ItemRarity.CURSED;
            
            int level = e.getValue();
            float levelP = (float)level / enchantment.getMaximumLevel();
            if (enchantment.isTreasure()) levelP += 1;
            
            float rarity = (float)( 11 - enchantment.getWeight().getWeight() ) * levelP;
            total += rarity;
        }
        
        int iRarity = Math.round(total / enchantments.size());
        
        // Offset the levels by TWO for books
        int o = ( stack.getItem() == Items.ENCHANTED_BOOK ? 2 : 0 );
        
        if (iRarity >= (8 + o))
            return ItemRarity.LEGENDARY;
        if (iRarity >= (6 + o))
            return ItemRarity.EPIC;
        if (iRarity >= (4 + o))
            return ItemRarity.RARE;
        if (iRarity >= (2 + o))
            return ItemRarity.UNCOMMON;
        return ItemRarity.COMMON;
    }
    public enum ItemRarity {
        CURSED( Formatting.RED ),
        COMMON( Formatting.GRAY ),
        UNCOMMON( Formatting.GREEN ),
        RARE( Formatting.AQUA ),
        EPIC( Formatting.DARK_PURPLE ),
        LEGENDARY( Formatting.GOLD );
        
        public final Formatting[] formatting;
        
        ItemRarity(Formatting... formatting) {
            this.formatting = formatting;
        }
    }
}
