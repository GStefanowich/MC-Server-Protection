package net.TheElm.project.objects;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.NotNull;

public final class LootInventory extends SimpleInventory {
    
    public LootInventory() {
        super(54);
    }
    
    public @NotNull GenericContainerScreenHandler createContainer(int syncId, PlayerInventory playerInventory) {
        int slots = this.getUsedInventorySlots();
        
        ScreenHandlerType type = PlayerBackpack.getSizeType(slots);
        if (type == null)
            throw new NullPointerException("Could not find an inventory of size " + slots);
        return new GenericContainerScreenHandler(type, syncId, playerInventory, this, slots / 9);
    }
    
    public int getInvItems() {
        int slots = this.size();
        int count = 0;
        
        for (int i = 0; i < slots; i++)
            if (!this.getStack(i).isEmpty())
                count = i;
        
        return count;
    }
    public int getUsedInventorySlots() {
        int quotient = (int)Math.ceil((float)(this.getInvItems() / 9.0));
        return Math.min(54, Math.max(quotient * 9, 9));
    }
    
    public ListTag toTag(ListTag tag) {
        for (int i = 0; i < this.getInvItems(); i++) {
            tag.add(this.getStack(i)
                .toTag(new CompoundTag()));
        }
        return tag;
    }
    
}
