package net.TheElm.project.mixins.Blocks;

import net.TheElm.project.interfaces.BossLootableContainer;
import net.TheElm.project.objects.LootInventory;
import net.TheElm.project.utilities.BossLootRewards;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.SimpleNamedContainerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public abstract class LootChest extends BlockWithEntity {
    
    protected LootChest(Settings settings) {
        super(settings);
    }
    
    @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)
    public void onInteract(BlockState blockState, World world, BlockPos blockPos, PlayerEntity player, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof BossLootableContainer) {
                Identifier identifier = ((BossLootableContainer)blockEntity).getBossLootIdentifier();
                if (identifier != null) {
                    BossLootRewards rewards = BossLootRewards.get(identifier);
                    if (rewards == null)
                        player.sendMessage(new LiteralText("Couldn't find any loot for that boss.").formatted(Formatting.RED));
                    else {
                        LootInventory inventory = rewards.getPlayerLoot(player.getUuid());
                        if (inventory.isInvEmpty())
                            player.sendMessage(new LiteralText("You don't have any loot from the ").formatted(Formatting.RED).append(rewards.getEntityName()).append("."));
                        else {
                            player.openContainer(new SimpleNamedContainerFactory((i, playerInventory, playerEntity) ->
                                inventory.createContainer(i, playerInventory),
                            rewards.getContainerName()));
                        }
                    }
                    
                    callback.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
    }
    
}
