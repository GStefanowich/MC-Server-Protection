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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.interfaces.BlockBreakCallback;
import net.TheElm.project.interfaces.BlockInteractionCallback;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.packet.BlockUpdateS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.network.packet.PlayerActionC2SPacket.Action;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class BlockInteraction {
    
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;
    
    @Inject(at = @At("HEAD"), method = "method_14263", cancellable = true)
    private void onBlockBreakChange(BlockPos blockPos, Action action, Direction direction, int i, CallbackInfo info) {
        ActionResult result = BlockBreakCallback.EVENT.invoker().interact(this.player, this.world, Hand.MAIN_HAND, blockPos, direction, action);
        if ( result != ActionResult.PASS ) {
            this.updateNeighboringBlockStates( blockPos );
            info.cancel();
        }
    }
    
    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void beforeItemInteract(final PlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {}
    
    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void beforeBlockInteract(final PlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, final BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.world.isClient) {
            ActionResult result = BlockInteractionCallback.EVENT.invoker().interact((ServerPlayerEntity)player, world, hand, itemStack, blockHitResult);
            if (result != ActionResult.PASS) {
                this.updateNeighboringBlockStates(blockHitResult.getBlockPos());
                callback.setReturnValue(result);
            }
        }
    }
    
    private void updateNeighboringBlockStates(BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        BlockPos part = null;
        
        if ( block instanceof BedBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            BedPart bedPart = blockState.get(BedBlock.PART);
            part = blockPos.offset(bedPart == BedPart.HEAD ? facing.getOpposite() : facing);
        } else if ( block instanceof HorizontalFacingBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            part = blockPos.offset(facing.getOpposite());
        } else if ( block instanceof TallPlantBlock ) {
            DoubleBlockHalf half = blockState.get(TallPlantBlock.HALF);
            part = blockPos.offset(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        } else if ( block instanceof DoorBlock ) {
            DoubleBlockHalf half = blockState.get(DoorBlock.HALF);
            part = half == DoubleBlockHalf.LOWER ? blockPos.up() : blockPos.down();
        }
        
        if (part != null) this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, part));
        this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, blockPos));
    }
}
