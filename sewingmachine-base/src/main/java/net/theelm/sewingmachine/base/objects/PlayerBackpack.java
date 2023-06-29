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

package net.theelm.sewingmachine.objects;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerBackpack extends SimpleInventory {
    
    private final Set<Identifier> autopickup = new HashSet<>();
    private final int rows;
    private final @NotNull PlayerEntity player;
    
    // Create an entirely new backpack
    public PlayerBackpack(@NotNull PlayerEntity player, int rows) {
        super(MathHelper.clamp(rows, 1, 6) * 9);
        this.rows = rows;
        this.player = player;
    }
    public PlayerBackpack(@NotNull PlayerEntity player, @NotNull PlayerBackpack old) {
        this(player, old.getRows());
        
        // Transfer the contents of the inventory
        this.readTags(old.getTags());
        
        // Transfer the auto-pickup settings
        this.readPickupTags(old.getPickupTags());
    }
    
    // Copy items to the backpack from the previous
    public PlayerBackpack(@NotNull PlayerBackpack backpack) {
        this(backpack, backpack.getRows() + 1);
    }
    public PlayerBackpack(@NotNull PlayerBackpack backpack, int rows) {
        this(backpack.getPlayer(), rows);
        
        // Transfer the contents of the inventory
        this.readTags(backpack.getTags());
        
        // Transfer the auto-pickup settings
        this.readPickupTags(backpack.getPickupTags());
    }
    
    /*
     * Insert into the backpack
     */
    public boolean insertStack(@NotNull ItemStack itemStack) {
        return this.insertStack(-1, itemStack);
    }
    public boolean insertStack(int slot, @NotNull ItemStack itemStack) {
        if (itemStack.isEmpty())
            return false;
        try {
            // Damaged items must be in their own slot
            if (itemStack.isDamaged()) {
                // Search for a slot
                if (slot == -1)
                    slot = this.getEmptySlot();
                
                // If a slot is not found, fail
                if (slot < 0)
                    return false;
                
                this.setStack(slot, itemStack.copy());
                itemStack.decrement(1);
                return true;
            }
            
            int remainingStack;
            do {
                remainingStack = itemStack.getCount();
                if (slot == -1) itemStack.setCount(this.insertFrom(itemStack));
                else itemStack.setCount(this.insertFrom(slot, itemStack));
            } while( !itemStack.isEmpty() && itemStack.getCount() < remainingStack );
            
            return itemStack.getCount() <= remainingStack;
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.create(var6, "Adding item to inventory");
            CrashReportSection crashReportSection = crashReport.addElement("Item being added");
            crashReportSection.add("Item ID", Item.getRawId(itemStack.getItem()));
            crashReportSection.add("Item data", itemStack.getDamage());
            crashReportSection.add("Item name", () -> itemStack.getName().getString());
            throw new CrashException(crashReport);
        }
    }
    
    /**
     * Transfer items from the stack provided into the inventory
     * @param extStack The stack to take items from
     * @return The amount of items remaining in the extStack
     */
    private int insertFrom(@NotNull ItemStack extStack) {
        int i = this.getOccupiedSlotWithRoomForStack(extStack);
        if (i == -1)
            i = this.getEmptySlot();
        
        return i == -1 ? extStack.getCount() : this.insertFrom(i, extStack);
    }
    
    /**
     * Transfer items from the stack provided into a slot in the inventory
     * @param slot The slot to insert items into
     * @param extStack The stack to take items from
     * @return The amount of items remaining in the extStack
     */
    private int insertFrom(int slot, @NotNull ItemStack extStack) {
        Item item = extStack.getItem();
        int j = extStack.getCount();
        ItemStack invStack = this.getStack(slot);
        if (invStack.isEmpty()) {
            invStack = new ItemStack(item, 0);
            if (extStack.hasNbt()) {
                invStack.setNbt(extStack.getOrCreateNbt().copy());
            }
            
            this.setStack(slot, invStack);
        }
        
        int k = j;
        if (j > invStack.getMaxCount() - invStack.getCount()) {
            k = invStack.getMaxCount() - invStack.getCount();
        }
        
        if (k > this.getMaxCountPerStack() - invStack.getCount()) {
            k = this.getMaxCountPerStack() - invStack.getCount();
        }
        
        if (k == 0) {
            return j;
        } else {
            j -= k;
            invStack.increment(k);
            return j;
        }
    }
    
    private boolean canStackAddMore(@NotNull ItemStack mainStack, @NotNull ItemStack otherStack) {
        return !mainStack.isEmpty() && this.areItemsEqual(mainStack, otherStack) && mainStack.isStackable() && mainStack.getCount() < mainStack.getMaxCount() && mainStack.getCount() < this.getMaxCountPerStack();
    }
    private boolean areItemsEqual(@NotNull ItemStack mainStack, @NotNull ItemStack otherStack) {
        return mainStack.getItem() == otherStack.getItem() && ItemStack.areEqual(mainStack, otherStack);
    }
    
    public int getEmptySlot() {
        for(int i = 0; i < this.size(); ++i)
            if (this.getStack(i).isEmpty())
                return i;
        
        return -1;
    }
    public int getOccupiedSlotWithRoomForStack(ItemStack itemStack) {
        for(int slot = 0; slot < this.size(); ++slot) {
            if (this.canStackAddMore(this.getStack(slot), itemStack)) {
                return slot;
            }
        }
        
        return -1;
    }
    
    /*
     * Backpack Items as NBT
     */
    public void readTags(@NotNull NbtList listTag) {
        for(int slot = 0; slot < this.size(); ++slot) {
            this.setStack(slot, ItemStack.EMPTY);
        }
        
        for(int itemCount = 0; itemCount < listTag.size(); ++itemCount) {
            NbtCompound compoundTag = listTag.getCompound(itemCount);
            int slot = compoundTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.size()) {
                this.setStack(slot, ItemStack.fromNbt(compoundTag));
            }
        }
    }
    public @NotNull NbtList getTags() {
        NbtList listTag = new NbtList();
        
        for(int slot = 0; slot < this.size(); ++slot) {
            ItemStack itemStack = this.getStack(slot);
            if (!itemStack.isEmpty()) {
                NbtCompound compoundTag = new NbtCompound();
                compoundTag.putByte("Slot", (byte)slot);
                itemStack.writeNbt(compoundTag);
                listTag.add(compoundTag);
            }
        }
        
        return listTag;
    }
    
    /*
     * Backpack Auto-Pickup NBT
     */
    public void readPickupTags(@NotNull NbtList listTag) {
        for (int i = 0; i < listTag.size(); ++i) {
            this.autopickup.add(new Identifier(
                listTag.getString(i)
            ));
        }
    }
    public @NotNull NbtList getPickupTags() {
        NbtList listTag = new NbtList();
        
        for (Identifier identifier : this.autopickup) {
            listTag.add(NbtString.of(
                identifier.toString()
            ));
        }
        
        return listTag;
    }
    
    public @NotNull PlayerEntity getPlayer() {
        return this.player;
    }
    
    public int getRows() {
        return this.rows;
    }
    
    public @NotNull Text getName() {
        return Text.literal(this.player.getDisplayName().getString() + "'s Backpack");
    }
    
    public @Nullable GenericContainerScreenHandler createContainer(int syncId, @NotNull PlayerInventory playerInventory) {
        int slots = this.size();
        ScreenHandlerType<?> type = PlayerBackpack.getSizeType(slots);
        return type == null ? null : new GenericContainerScreenHandler(type, syncId, playerInventory, this, slots / 9);
    }
    
    public static @Nullable ScreenHandlerType<?> getSizeType(int slots) {
        switch (slots) {
            case 9:
                return ScreenHandlerType.GENERIC_3X3;
            case 18:
                return ScreenHandlerType.GENERIC_9X2;
            case 27:
                return ScreenHandlerType.GENERIC_9X3;
            case 36:
                return ScreenHandlerType.GENERIC_9X4;
            case 45:
                return ScreenHandlerType.GENERIC_9X5;
            case 54:
                return ScreenHandlerType.GENERIC_9X6;
        }
        return null;
    }
    
    public boolean addAutoPickup(@NotNull Item item) {
        Identifier id = Registries.ITEM.getId(item);
        boolean in;
        if (in = this.autopickup.contains( id ))
            this.autopickup.remove( id );
        else
            this.autopickup.add( id );
        return !in;
    }
    public boolean shouldAutoPickup(@NotNull ItemStack stack) {
        return this.shouldAutoPickup(stack.getItem());
    }
    public boolean shouldAutoPickup(@NotNull Item item) {
        return this.shouldAutoPickup(Registries.ITEM.getId(item));
    }
    public boolean shouldAutoPickup(@NotNull Identifier identifier) {
        return this.autopickup.contains(identifier);
    }
    
    @Override
    public void onOpen(@NotNull PlayerEntity player) {
        // Play sound
        player.playSound(SoundEvents.UI_TOAST_OUT, SoundCategory.BLOCKS, 0.5f, 1.0f);
        
        // Parent method
        super.onOpen(player);
    }
    @Override
    public void onClose(@NotNull PlayerEntity player) {
        // Play sound
        player.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.BLOCKS, 0.5f, 1.0f);
        
        // Parent method
        super.onClose(player);
    }
    
    public void dropAll() {
        this.dropAll(false);
    }
    public void dropAll(boolean vanish) {
        List<ItemStack> list = this.clearToList();
        for (ItemStack itemStack : list) {
            if (itemStack.isEmpty() || (vanish && EnchantmentHelper.hasVanishingCurse(itemStack)))
                continue;
            this.player.dropItem(itemStack, true, false);
        }
    }
}
