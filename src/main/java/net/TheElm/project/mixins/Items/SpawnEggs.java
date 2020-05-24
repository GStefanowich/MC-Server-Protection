package net.TheElm.project.mixins.Items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggs extends Item {
    
    public SpawnEggs(Settings settings) {
        super(settings);
    }
    
    /*
     * Prevent Spawn eggs from modifying Spawners in survival mode
     */
    @Inject(at = @At(value = "HEAD", target = "net/minecraft/world/BlockView.getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;"), method = "useOnBlock", cancellable = true)
    public void onUse(ItemUsageContext context, CallbackInfoReturnable<ActionResult> callback) {
        World world = context.getWorld();
        if (!world.isClient) {
            BlockState state = world.getBlockState(context.getBlockPos());
            Block block = state.getBlock();
            PlayerEntity player = context.getPlayer();
            if (((player == null) || (!player.isCreative())) && (block == Blocks.SPAWNER))
                callback.setReturnValue(ActionResult.PASS);
        }
    }
    
}
