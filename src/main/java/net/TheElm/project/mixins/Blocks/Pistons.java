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

package net.TheElm.project.mixins.Blocks;

import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.utilities.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public class Pistons {
    
    @Inject(at = @At("RETURN"), method = "isMovable", cancellable = true)
    private static void isMovable(BlockState blockState, World world, BlockPos blockPos, Direction moveDir, boolean bl, Direction pistonDir, CallbackInfoReturnable<Boolean> callback) {
        BlockPos pistonPos, movePos;
        
        // If block is immovable
        if (!callback.getReturnValue())
            return;
        
        // If pushing
        if (pistonDir == moveDir) {
            pistonPos = blockPos.offset(pistonDir.getOpposite());
            movePos = blockState.isAir() ? blockPos // If AIR, don't offset
                : blockPos.offset(moveDir); // Move block position to where it is GOING to be
        }
        // If pulling
        else {
            pistonPos = blockPos.offset(moveDir, 2);
            movePos = blockPos;
        }
        
        // TODO: Fix desync when pulling blocks that suddenly vanish
        
        // Check that first chunk owner can modify the next chunk
        if (!BlockUtils.canBlockModifyBlock(world, movePos, pistonPos, ClaimPermissions.BLOCKS ))
            callback.setReturnValue(false);
    }
    
}
