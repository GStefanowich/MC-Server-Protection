package net.theelm.sewingmachine.mixins.Blocks;

import net.theelm.sewingmachine.interfaces.BossLootableContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class LootChestEntity extends LootableContainerBlockEntity implements SidedInventory, BossLootableContainer {
    
    private Identifier bossLootLinkedIdentifier;
    
    protected LootChestEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    
    /*
     * NBT Saving / Loading
     */
    
    @Inject(at = @At("TAIL"), method = "readNbt")
    public void onNbtRead(NbtCompound tag, CallbackInfo callback) {
        this.bossLootLinkedIdentifier = (tag.contains("BossLootContainer", NbtElement.STRING_TYPE) ? new Identifier(tag.getString("BossLootContainer")) : null);
    }
    
    @Inject(at = @At("TAIL"), method = "writeNbt")
    public void onNbtWrite(NbtCompound tag, CallbackInfo callback) {
        if (this.bossLootLinkedIdentifier != null)
            tag.putString("BossLootContainer", this.bossLootLinkedIdentifier.toString());
    }
    
    /*
     * Getter
     */
    
    @Override
    public @Nullable Identifier getBossLootIdentifier() {
        return this.bossLootLinkedIdentifier;
    }
    
}
