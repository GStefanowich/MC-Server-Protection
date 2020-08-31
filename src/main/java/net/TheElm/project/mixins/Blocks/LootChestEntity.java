package net.TheElm.project.mixins.Blocks;

import net.TheElm.project.interfaces.BossLootableContainer;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class LootChestEntity extends LootableContainerBlockEntity implements SidedInventory, Tickable, BossLootableContainer {
    
    private Identifier bossLootLinkedIdentifier;
    
    protected LootChestEntity(BlockEntityType<?> blockEntityType) {
        super(blockEntityType);
    }
    
    /*
     * NBT Saving / Loading
     */
    
    @Inject(at = @At("TAIL"), method = "fromTag")
    public void nbtRead(BlockState state, CompoundTag tag, CallbackInfo callback) {
        this.bossLootLinkedIdentifier = (tag.contains("BossLootContainer", NbtType.STRING) ? new Identifier(tag.getString("BossLootContainer")) : null);
    }
    
    @Inject(at = @At("TAIL"), method = "toTag")
    public void nbtWrite(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback) {
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
