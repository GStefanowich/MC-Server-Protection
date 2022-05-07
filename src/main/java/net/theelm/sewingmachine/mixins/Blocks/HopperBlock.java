package net.theelm.sewingmachine.mixins.Blocks;

import net.theelm.sewingmachine.enums.ClaimPermissions;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlock extends LootableContainerBlockEntity implements Hopper {
    
    protected HopperBlock(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }
    
    @Inject(at = @At("HEAD"), method = "extract", cancellable = true)
    private static void onExtract(World world, Hopper hopper, CallbackInfoReturnable<Boolean> callback) {
        BlockPos pos = new BlockPos(hopper.getHopperX(), hopper.getHopperY(), hopper.getHopperZ());
        if ((world != null) && (!BlockUtils.canBlockModifyBlock(world, pos.up(), pos, ClaimPermissions.STORAGE)))
            callback.setReturnValue(false);
    }
    
    @Inject(at = @At("HEAD"), method = "getOutputInventory", cancellable = true)
    private static void onGetOutput(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Inventory> callback) {
        if (world != null) {
            Direction direction = state.get(net.minecraft.block.HopperBlock.FACING);
            if (!BlockUtils.canBlockModifyBlock(world, pos.offset(direction), pos, ClaimPermissions.STORAGE))
                callback.setReturnValue(null);
        }
    }
    
}
