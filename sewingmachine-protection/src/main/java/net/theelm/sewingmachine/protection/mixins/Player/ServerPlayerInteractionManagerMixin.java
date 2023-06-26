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

package net.theelm.sewingmachine.protection.mixins.Player;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.theelm.sewingmachine.interfaces.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    private BlockPos miningPos;
    
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;
    
    @Shadow protected abstract void method_41250(BlockPos pos, boolean success, int sequence, String reason);
    
    @Inject(at = @At("HEAD"), method = "processBlockBreakingAction", cancellable = true)
    private void onBlockBreakChange(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo callback) {
        boolean result = BlockBreakCallback.EVENT.invoker()
            .canDestroy(this.player, this.world, this.player.preferredHand, pos, direction, action);
        if ( result ) {
            // Send the player a failed notice
            this.world.setBlockBreakingInfo(this.player.getId(), pos, -1);
            this.method_41250(pos, true, sequence, "may not interact");
            
            // Update the neighboring blocks on the client
            this.updateNeighboringBlockStates(pos);
            
            // Update the players mining position
            // Must to set to prevent (Block Mismatch) messages
            this.miningPos = pos;
            
            // Create a slow mining event
            //((LogicalWorld)this.world).addTickableEvent(new PlayerNoBreak(this.player, blockPos));
            
            // Cancel the rest of the event
            callback.cancel();
        }
    }
    
    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void beforeItemInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.getWorld().isClient) {
            ActionResult result = ItemUseCallback.EVENT.invoker().use(player, world, hand, itemStack);
            if (result != ActionResult.PASS)
                callback.setReturnValue(result);
        }
    }
    
    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void beforeBlockInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, final BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.getWorld().isClient) {
            ActionResult result = BlockInteractionCallback.EVENT.invoker().interact(player, world, hand, itemStack, blockHitResult);
            if (result != ActionResult.PASS) {
                this.updateNeighboringBlockStates(blockHitResult.getBlockPos());
                callback.setReturnValue(result);
            }
        }
    }
    
    private void updateNeighboringBlockStates(BlockPos blockPos) {
        final BlockState blockState = this.world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        BlockPos part = null;

        if ( block instanceof BedBlock) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            BedPart bedPart = blockState.get(BedBlock.PART);
            part = blockPos.offset(bedPart == BedPart.HEAD ? facing.getOpposite() : facing);
        } else if ( block instanceof HorizontalFacingBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            part = blockPos.offset(facing.getOpposite());
        } else if ( block instanceof TallPlantBlock) {
            DoubleBlockHalf half = blockState.get(TallPlantBlock.HALF);
            part = blockPos.offset(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        } else if ( block instanceof DoorBlock) {
            DoubleBlockHalf half = blockState.get(DoorBlock.HALF);
            part = half == DoubleBlockHalf.LOWER ? blockPos.up() : blockPos.down();
        }

        if (part != null) this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.world, part));
        this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, blockState));
    }
}
