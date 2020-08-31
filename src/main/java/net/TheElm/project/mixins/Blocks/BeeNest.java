package net.TheElm.project.mixins.Blocks;

import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeehiveBlock.class)
public class BeeNest {
    
    @Inject(at = @At("TAIL"), method = "onUse", cancellable = true)
    public void onInteractWith(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> callback) {
        // If the player has an empty hand
        if (callback.getReturnValue() == ActionResult.PASS && hand == Hand.MAIN_HAND && player.getStackInHand(hand).isEmpty()) {
            // If the block entity is a hive
            BlockEntity hiveEntity = world.getBlockEntity(pos);
            if (hiveEntity instanceof BeehiveBlockEntity) {
                int bees = ((BeehiveBlockEntity)hiveEntity).getBeeCount();
                
                // Get the translation key for the count of bees
                MutableText hiveInfo = TranslatableServerSide.text(player, "bee_hive." + bees);
                
                player.playSound(SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.MASTER, ((float) bees / 3), 1.0F);
                
                // Send the translated text
                player.sendSystemMessage(hiveInfo.formatted(Formatting.GRAY, Formatting.ITALIC), Util.NIL_UUID);
                
                // Set the use result as a success
                callback.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
    
}
