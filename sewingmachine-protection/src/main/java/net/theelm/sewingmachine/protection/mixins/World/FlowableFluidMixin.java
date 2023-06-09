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

package net.theelm.sewingmachine.protection.mixins.World;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.theelm.sewingmachine.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class FlowableFluidMixin extends Fluid {
    @Inject(at = @At("HEAD"), method = "canFlowThrough", cancellable = true)
    protected void gettingFluidDirections(BlockView view, Fluid fluid, BlockPos flowPos, BlockState state, Direction face, BlockPos sourcePos, BlockState fromState, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        // If world is Server World
        if (view instanceof World world) {
            // Get chunks
            Chunk startingChunk = world.getChunk(sourcePos);
            Chunk nextChunk = world.getChunk(flowPos);
            
            // If chunk is the same chunk, Allow
            if (startingChunk == nextChunk)
                return;
            
            // Check that first chunk owner can modify the next chunk
            if (!((IClaimedChunk) nextChunk).canPlayerDo(flowPos, ((IClaimedChunk) startingChunk).getOwnerId(sourcePos), ClaimPermissions.BLOCKS))
                cir.setReturnValue(false);
        }
    }
    
    @Inject(at = @At("HEAD"), method = "canFlow", cancellable = true)
    protected void gettingFluidDirections(BlockView view, BlockPos sourcePos, BlockState fluidBlockState, Direction flowDirection, BlockPos flowPos, BlockState flowToBlockState, FluidState fluidState, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        // If world is Server World
        if (view instanceof World world) {
            // Get chunks
            Chunk startingChunk = world.getChunk(sourcePos);
            Chunk nextChunk = world.getChunk(flowPos);
            
            // If chunk is the same chunk, Allow
            if (startingChunk == nextChunk)
                return;
            
            // Check that first chunk owner can modify the next chunk
            if (!((IClaimedChunk) nextChunk).canPlayerDo(flowPos, ((IClaimedChunk) startingChunk).getOwnerId(sourcePos), ClaimPermissions.BLOCKS))
                cir.setReturnValue(false);
        }
    }
}
