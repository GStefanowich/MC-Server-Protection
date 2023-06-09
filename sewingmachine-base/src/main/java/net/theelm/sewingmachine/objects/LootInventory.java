package net.theelm.sewingmachine.objects;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LootInventory extends SimpleInventory {
    
    public LootInventory() {
        super(54);
    }
    
    public @NotNull GenericContainerScreenHandler createContainer(int syncId, PlayerInventory playerInventory) {
        int slots = this.getUsedInventorySlots();
        
        ScreenHandlerType<?> type = PlayerBackpack.getSizeType(slots);
        if (type == null)
            throw new NullPointerException("Could not find an inventory of size " + slots);
        return new GenericLootInventoryScreenHandler(type, syncId, playerInventory, this, slots / 9);
    }
    
    public int getInvItems() {
        int slots = this.size();
        int count = 0;
        
        for (int i = 0; i < slots; i++)
            if (!this.getStack(i).isEmpty())
                count = i + 1;
        
        return count;
    }
    public int getUsedInventorySlots() {
        int quotient = (int)Math.ceil((float)(this.getInvItems() / 9.0));
        return Math.min(54, Math.max(quotient * 9, 9));
    }
    
    /**
     * Give the players loot inventory a new reward item
     * @param stack The players reward
     * @return If the item was accepted into the inventory
     */
    public boolean insertLoot(ItemStack stack) {
        return super.addStack(stack)
            .isEmpty();
    }
    
    @Override
    public ItemStack addStack(ItemStack stack) {
        return stack;
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }
    
    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
    }
    
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return stack.isEmpty();
    }
    
    @Override
    public void onClose(@Nullable PlayerEntity player) {
        if (player != null) {
            for (int slot = 0; slot < this.getInvItems(); slot++) {
                ItemStack stack = this.getStack(slot);
                if (!stack.isEmpty()) {
                    PlayerInventory inventory = player.getInventory();
                    if (!inventory.insertStack(stack))
                        break;
                }
            }
        }
        super.onClose(player);
    }
    
    public NbtList toTag(@NotNull NbtList tag) {
        for (int slot = 0; slot < this.getInvItems(); slot++) {
            ItemStack stack = this.getStack(slot);
            if (!stack.isEmpty())
                tag.add(stack.writeNbt(new NbtCompound()));
        }
        return tag;
    }
    
    private static final class GenericLootInventoryScreenHandler extends GenericContainerScreenHandler {
        public GenericLootInventoryScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, LootInventory inventory, int rows) {
            super(type, syncId, playerInventory, inventory, rows);
        }
        
        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            if (this.canInteractSlot(slotIndex, actionType)) {
                super.onSlotClick(slotIndex, button, actionType, player);
            }
        }
        
        private boolean canInteractSlot(int slotIndex, SlotActionType actionType) {
            LootInventory inventory = (LootInventory)this.getInventory();
            ItemStack stack = this.getCursorStack();
            
            int totalSlots = inventory.getUsedInventorySlots();
            /*System.out.println(slotIndex + " / " + totalSlots + " (" + actionType + ")");*/
            return (slotIndex >= totalSlots && actionType != SlotActionType.QUICK_MOVE)
                || (slotIndex < totalSlots && actionType == SlotActionType.QUICK_MOVE)
                || (stack.isEmpty() && actionType == SlotActionType.PICKUP);
        }
    }
}
