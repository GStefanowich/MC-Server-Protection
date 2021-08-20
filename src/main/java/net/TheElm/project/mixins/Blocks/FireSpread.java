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

import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/**
 * Created on Aug 16 2021 at 12:18 AM.
 * By greg in SewingMachineMod
 */
@Mixin(FireBlock.class)
public abstract class FireSpread extends AbstractFireBlock {
    public FireSpread(Settings settings, float damage) {
        super(settings, damage);
    }
    
    @Shadow
    private native int getSpreadChance(BlockState state);
    
    @Shadow
    private native int getBurnChance(WorldView worldView, BlockPos pos);
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/block/FireBlock.getSpreadChance(Lnet/minecraft/block/BlockState;)I"), method = "trySpreadingFire")
    public int getFireSpreadChance(@NotNull FireBlock block, @NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, int spreadFactor, @NotNull Random rand, int currentAge) {
        if (!ChunkUtils.isSetting(ClaimSettings.FIRE_SPREAD, world, pos))
            return 0;
        return this.getSpreadChance(state);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/block/FireBlock.getBurnChance(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)I"), method = "scheduledTick")
    public int getIgniteChance(@NotNull FireBlock block, @NotNull WorldView view, BlockPos pos, BlockState tickState, ServerWorld tickWorld, BlockPos tickPos, Random random) {
        if (!ChunkUtils.isSetting(ClaimSettings.FIRE_SPREAD, view, pos))
            return 0;
        return this.getBurnChance(view, pos);
    }
}
