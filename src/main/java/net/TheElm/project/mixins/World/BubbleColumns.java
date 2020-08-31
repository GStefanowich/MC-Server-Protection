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

package net.TheElm.project.mixins.World;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BubbleColumnBlock.class)
public class BubbleColumns {
    
    @Overwrite
    public static void update(WorldAccess world, BlockPos blockPos, boolean pullDownwards) {
        if (BubbleColumnBlock.isStillWater( world, blockPos ))
            world.setBlockState(blockPos, Blocks.BUBBLE_COLUMN.getDefaultState().with(BubbleColumnBlock.DRAG, pullDownwards), 2);
        else if (isPassableBlock( world, blockPos ) && BubbleColumnBlock.isStillWater( world, blockPos.up() ))
            world.setBlockState(blockPos.up(), Blocks.BUBBLE_COLUMN.getDefaultState().with(BubbleColumnBlock.DRAG, pullDownwards), 2);
        else
            System.out.println(world.getBlockState(blockPos).getBlock().getTranslationKey() + " is not a passable block");
    }
    
    @Inject(at = @At("HEAD"), method = "canPlaceAt", cancellable = true)
    public void onCheckPlacement(BlockState blockState, WorldView worldView, BlockPos blockPos, CallbackInfoReturnable<Boolean> callback) {
        BlockState blockState2 = worldView.getBlockState(blockPos.down());
        if (blockState2.contains(Properties.WATERLOGGED))
            callback.setReturnValue( true );
    }
    
    private static boolean isPassableBlock(WorldAccess world, BlockPos blockPos) {
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.contains(Properties.WATERLOGGED) && blockState.get(Properties.WATERLOGGED);
    }
    
}
