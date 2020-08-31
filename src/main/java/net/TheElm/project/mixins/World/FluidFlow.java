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

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class FluidFlow extends Fluid {
    
    @Shadow
    protected abstract boolean isInfinite();
    
    @Inject(at = @At("HEAD"), method = {"canFlowThrough", "canFlow"}, cancellable = true)
    protected void gettingFluidDirections(BlockView world, Fluid fluid, BlockPos sourcePos, BlockState sourceState, Direction flowDirection, BlockPos flowPos, BlockState flowState, FluidState fluidState, CallbackInfoReturnable<Boolean> callback) {
        // If world is Server World
        if (world instanceof ServerWorld) {
            // Get chunks
            WorldChunk startingChunk = ((ServerWorld) world).getWorldChunk( sourcePos );
            WorldChunk nextChunk = ((ServerWorld) world).getWorldChunk( flowPos );
            
            // If chunk is the same chunk, Allow
            if (startingChunk == nextChunk)
                return;
            
            // Check that first chunk owner can modify the next chunk
            if (!((IClaimedChunk) nextChunk).canPlayerDo(flowPos, ((IClaimedChunk) startingChunk).getOwner(), ClaimPermissions.BLOCKS))
                callback.setReturnValue(false);
        }
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/fluid/FlowableFluid.isInfinite()Z"), method = "getUpdatedState")
    protected boolean onUpdate(FlowableFluid fluid, WorldView worldView, BlockPos blockPos, BlockState blockState) {
        if (SewConfig.get(SewConfig.NETHER_INFINITE_LAVA) // Is ENABLED
            && (fluid.equals(Fluids.FLOWING_LAVA) || fluid.equals(Fluids.LAVA)) // Is LAVA
            && worldView.getDimension().isUltrawarm()) // NETHER
            return true;
        return this.isInfinite();
    }
    
}
