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

package net.theelm.sewingmachine.utilities;

import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class InventoryUtils {
    
    /*
     * Get the inventory of a block
     */
    public static @Nullable Inventory getInventoryOf(@NotNull World world, @NotNull BlockPos blockPos) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);
        return blockEntity instanceof LootableContainerBlockEntity ? InventoryUtils.getInventoryOf(world, (LootableContainerBlockEntity) blockEntity) : null;
    }
    public static @Nullable Inventory getInventoryOf(@NotNull LootableContainerBlockEntity container) {
        World world = container.getWorld();
        return world == null ? null : InventoryUtils.getInventoryOf(world, container);
    }
    public static @Nullable Inventory getInventoryOf(@NotNull World world, @NotNull LootableContainerBlockEntity container) {
        BlockState blockState = world.getBlockState(container.getPos());
        Block block = blockState.getBlock();
        
        if (block instanceof ChestBlock chestBlock)
            return ChestBlock.getInventory(chestBlock, blockState, world, container.getPos(), true);
        return container;
    }
    public static @NotNull ItemStack getFirstStack(@NotNull Inventory inventory) {
        return InventoryUtils.getFirstStack(inventory, InventoryUtils::notEmpty);
    }
    public static @NotNull ItemStack getFirstStack(@NotNull Inventory inventory, @NotNull Predicate<ItemStack> predicate) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slot = inventory.getStack(i);
            if (predicate.test(slot))
                return slot;
        }
        return new ItemStack(Items.AIR);
    }
    
    /*
     * Get the inventory that is attached to a shop sign
     */
    
    public static @Nullable LootableContainerBlockEntity getAttachedChest(@NotNull final World world, @NotNull final BlockPos signPos) {
        List<BlockPos> checkPositions = new ArrayList<>();
        
        // Add the blockPos BELOW the sign
        checkPositions.add(signPos.offset(Direction.DOWN, 1));
        
        // Add the blockPos BEHIND the sign
        BlockState signBlockState = world.getBlockState( signPos );
        if ( signBlockState.getBlock() instanceof WallSignBlock) {
            Direction signFacing = signBlockState.get(HorizontalFacingBlock.FACING).getOpposite();
            checkPositions.add(signPos.offset(signFacing, 1));
        }
        
        for ( BlockPos blockPos : checkPositions ) {
            BlockEntity chestBlockEntity = world.getBlockEntity(blockPos);
            if ( chestBlockEntity instanceof LootableContainerBlockEntity && EntityUtils.isValidShopContainer(chestBlockEntity) )
                return (LootableContainerBlockEntity) chestBlockEntity;
        }
        
        return null;
    }
    public static @Nullable LootableContainerBlockEntity getAttachedChest(@NotNull final ShopSignData signBuilder) {
        return InventoryUtils.getAttachedChest(Objects.requireNonNull(signBuilder.getSign().getWorld()), signBuilder.getSign().getPos());
    }
    public static boolean hasAttachedChest(@NotNull final ShopSignData signBuilder) {
        return InventoryUtils.getAttachedChest(signBuilder) != null;
    }
    
    /*
     * Get count of an item inside an inventory
     */
    public static int getInventoryCount(@NotNull Inventory inventory, @NotNull Item item) {
        return InventoryUtils.getInventoryCount(inventory, stack -> Objects.equals(stack.getItem(), item));
    }
    public static int getInventoryCount(@NotNull Inventory inventory, @NotNull Predicate<ItemStack> predicate) {
        int count = 0;
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (predicate.test(stack))
                count += stack.getCount();
        }
        
        return count;
    }
    
    /*
     * Check if stacks are empty or not
     */
    
    public static boolean isEmpty(@NotNull ItemStack stack) {
        return stack.isEmpty();
    }
    public static boolean notEmpty(@NotNull ItemStack stack) {
        return !stack.isEmpty();
    }
    
    /*
     * Transfer items from a player to a chest
     */
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Item item, final int count) {
        return InventoryUtils.playerToChest(player, sourcePos, playerInventory, chestInventory, s -> Objects.equals(s.getItem(), item), count);
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Item item, final int count, final boolean required) {
        return InventoryUtils.playerToChest(player, sourcePos, playerInventory, chestInventory, s -> s.getItem() == item, count, required);
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final ItemStack stack, final int count) {
        return InventoryUtils.playerToChest(player, sourcePos, playerInventory, chestInventory, s -> s.getItem() == stack.getItem(), count);
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final ItemStack stack, final int count, final boolean required) {
        return InventoryUtils.playerToChest(player, sourcePos, playerInventory, chestInventory, s -> s.getItem() == stack.getItem(), count, required);
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Predicate<ItemStack> loosePredicate, final int count) {
        return InventoryUtils.playerToChest(player, sourcePos, playerInventory, chestInventory, loosePredicate, count, false);
    }
    public static boolean playerToChest(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @NotNull final PlayerInventory playerInventory, @Nullable final Inventory chestInventory, @NotNull final Predicate<ItemStack> loosePredicate, final int count, final boolean required) {
        // World
        ServerWorld world = player.getWorld();
        
        // Check if enough in player inventory
        if ( required && (InventoryUtils.getInventoryCount(playerInventory, loosePredicate) < count) )
            return false;
        
        // Get stack size to take from player up to 64
        int itemStackSize = 0;
        
        boolean success;
        
        // If chest has item on the frame
        if ( InventoryUtils.getInventoryCount(playerInventory, loosePredicate) > 0) {
            final int invSize = playerInventory.size(); // Get the inventory size to iterate over
            
            // Get stack sizes
            for ( int i = 0; i < invSize; i++ ) {
                final ItemStack invItem = playerInventory.getStack( i );
                if ( loosePredicate.test(invItem) ) {
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
                        int chestSize = chestInventory.size();
                        for (int y = 0; y < chestSize; y++) {
                            // If the players stack is empty, break
                            if (invItem.getCount() <= 0)
                                break;
                            
                            // Get the item from chest Pos Y
                            final ItemStack chestItemStack = chestInventory.getStack(y);
                            final Item chestItem = chestItemStack.getItem();
                            
                            // If the slot is AIR or matching type
                            if (chestItem.equals(Items.AIR) || ((chestItemStack.getCount() < chestItem.getMaxCount()) && ItemUtils.areEqualStacks(invItem, chestItemStack))) {
                                final int put = Collections.min(Arrays.asList(chestItem.getMaxCount() - chestItemStack.getCount(), invItem.getCount(), putable));
                                if (chestItem.equals(Items.AIR)) {
                                    // Put a copy of the item from the players inventory
                                    final ItemStack clone = invItem.copy();
                                    clone.setCount(put);
                                    
                                    chestInventory.setStack(y, clone);
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
                                        world.playSound(null, sourcePos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f);
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
            world.playSound(null, sourcePos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f);
        return success;
    }
    
    /*
     * Transfer items from a chest to the player
     */
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final ItemStack stack, final int count) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, s -> s.getItem() == stack.getItem(), s -> ItemUtils.areEqualStacks(s, stack), count);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final ItemStack stack, final int count, final boolean required) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, s -> s.getItem() == stack.getItem(), s -> ItemUtils.areEqualStacks(s, stack), count, required);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> loosePredicate, @NotNull final Predicate<ItemStack> strictPredicate, final int count) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, loosePredicate, strictPredicate, count, false);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> predicate, final int count, final boolean required) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, predicate, predicate, count, required);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> loosePredicate, @NotNull final Predicate<ItemStack> strictPredicate, final int count, final boolean required) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, loosePredicate, strictPredicate, count, required, null);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> predicate, final int count, @Nullable ItemStackGenerator spawner) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, predicate, count, false, spawner);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> predicate, final int count, final boolean required, @Nullable ItemStackGenerator spawner) {
        return InventoryUtils.chestToPlayer(player, sourcePos, chestInventory, playerInventory, predicate, predicate, count, required, spawner);
    }
    public static boolean chestToPlayer(@NotNull ServerPlayerEntity player, @NotNull final BlockPos sourcePos, @Nullable final Inventory chestInventory, @NotNull final PlayerInventory playerInventory, @NotNull final Predicate<ItemStack> loosePredicate, @NotNull final Predicate<ItemStack> strictPredicate, final int count, final boolean required, @Nullable ItemStackGenerator spawner) {
        // World
        ServerWorld world = player.getWorld();
        
        // Check if enough in the chest
        if (required && (chestInventory != null) && (InventoryUtils.getInventoryCount(chestInventory, loosePredicate) < count))
            return false;
        
        // Get stack size to give to player up to 64
        int stackSize = 0;
        
        if ( chestInventory == null ) {
            if (spawner != null) {
                while ( stackSize < count ) {
                    // Create the new itemstack
                    ItemStack clone = spawner.create(count - stackSize);
                    
                    // Set amount given
                    stackSize += clone.getCount();
                    
                    // Insert items into player inventory
                    playerInventory.offerOrDrop(clone);
                }
            }
        } else {
            // If chest has item on the frame
            if (InventoryUtils.getInventoryCount(chestInventory, loosePredicate) > 0) {
                final int invSize = chestInventory.size(); // Get the inventory size to iterate over
                
                // Get stack sizes
                for (int i = invSize; i > 0; i--) {
                    final ItemStack chestItem = chestInventory.getStack(i - 1);
                    if (loosePredicate.test(chestItem)) {
                        int collectible = count - stackSize; // The amount of items remaining to give the player a full stack
                        // If stack size is full
                        if (collectible <= 0)
                            break;
                        int collect = Collections.min(Arrays.asList( collectible, chestItem.getCount()));
                        
                        // Create a copy of the item
                        final ItemStack clone = chestItem.copy();
                        
                        // Get a slot for the item
                        int slot = playerInventory.getOccupiedSlotWithRoomForStack( clone );
                        if ( slot == -1 || !strictPredicate.test(chestItem) ) {
                            clone.setCount(collect);
                            if (!playerInventory.insertStack( clone ))
                                break;
                        } else {
                            final ItemStack inInv = playerInventory.getStack(slot);
                            int invStackSize = inInv.getCount();
                            collect = Collections.min(Arrays.asList( collect, inInv.getItem().getMaxCount() - invStackSize));
                            inInv.setCount( invStackSize + collect );
                        }
                        
                        stackSize += collect;
                        
                        // Set the chest item stack size
                        chestItem.setCount(chestItem.getCount() - collect);
                    }
                }
            }
        }
        
        boolean success = ( required ? stackSize >= count : stackSize > 0 );
        if ( success )
            world.playSound(null, sourcePos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.MASTER,1.0f, 1.0f);
        return success;
    }
    
    /*
     * Check if an inventory contains items
     */
    public static boolean isInvEmpty(@Nullable Inventory inventory) {
        return (inventory == null) || inventory.isEmpty();
    }
    
    /*
     * Advanced enchanted books prevent combining
     */
    public static boolean areItemsAboveMaxLevel(@NotNull Enchantment enchantment, @NotNull ItemStack... stacks) {
        // Loop through all of the items
        for (ItemStack stack : stacks) {
            // Check the book enchantments
            int level = InventoryUtils.getBookOrItemEnchantLevel(enchantment, stack);
            if (level > enchantment.getMaxLevel())
                return true;
        }
        
        return false;
    }
    public static int getMaximumCombinedLevel(@NotNull Enchantment enchantment, @NotNull ItemStack... stacks) {
        List<@NotNull ItemStack> list = Arrays.asList(stacks);
        if (list.isEmpty())
            return enchantment.getMaxLevel();
        if (list.size() == 1)
            return InventoryUtils.getBookOrItemEnchantLevel(enchantment, list.get(0));
        
        int highest = enchantment.getMaxLevel();
        for (ItemStack stack : stacks) {
            int level = InventoryUtils.getBookOrItemEnchantLevel(enchantment, stack);
            if (level > highest)
                highest = level;
        }
        return highest;
    }
    public static int getBookOrItemEnchantLevel(@NotNull Enchantment enchantment, @NotNull ItemStack stack) {
        if (stack.isEmpty())
            return 0;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
        return enchants.getOrDefault(enchantment, 0);
    }
    
    public static @NotNull ItemRarity getItemRarity(@NotNull ItemStack stack) {
        float rarity = 0;
        
        // Get all enchantments
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        for ( Map.Entry<Enchantment, Integer> e : enchantments.entrySet() ) {
            Enchantment enchantment = e.getKey();
            
            // If the enchantment is a curse, the tool is ALWAYS cursed
            if (enchantment.isCursed()) return ItemRarity.CURSED;
            
            // Handle the rarity
            rarity += InventoryUtils.getEnchantmentRarity(enchantment, e.getValue());
        }
        
        return InventoryUtils.getItemRarity(
            rarity,
            enchantments.size(),
            (stack.getItem().equals(Items.ENCHANTED_BOOK))
        );
    }
    private static float getEnchantmentRarity(@NotNull Enchantment enchantment, int level) {
        int min = (level - enchantment.getMinLevel()),
            max = (enchantment.getMaxLevel() - enchantment.getMinLevel());
        
        // Get the percentage to max level
        float rarity = (max == 0 ? 1 : ((float)min / (float)max));
        
        // Increase the total item rarity
        Enchantment.Rarity weight = enchantment.getRarity();
        return ((float)(10 / weight.getWeight()) * rarity);
    }
    private static @NotNull ItemRarity getItemRarity(final float rarity, final int enchantments, boolean isBook) {
        // Offset the levels by TWO for books
        int o = ( isBook ? 2 : 1 );
        
        // Return the matching rarity
        if (rarity >= (24f / o))
            return ItemRarity.LEGENDARY;
        if (rarity >= (12f / o))
            return ItemRarity.EPIC;
        if (rarity >= (6f / o))
            return ItemRarity.RARE;
        if (rarity >= (3f / o))
            return ItemRarity.UNCOMMON;
        return ItemRarity.COMMON;
    }
    
    public enum ItemRarity {
        CURSED(Formatting.RED),
        COMMON(Formatting.GRAY),
        UNCOMMON(Formatting.GREEN),
        RARE(Formatting.AQUA),
        EPIC(Formatting.DARK_PURPLE),
        LEGENDARY(Formatting.GOLD);
        
        public final Formatting[] formatting;
        
        ItemRarity(Formatting... formatting) {
            this.formatting = formatting;
        }
    }
    @FunctionalInterface
    public interface ItemStackGenerator {
        ItemStack create(int count);
    }
}
