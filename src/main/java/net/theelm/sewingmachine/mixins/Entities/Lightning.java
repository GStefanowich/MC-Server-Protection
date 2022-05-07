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

package net.theelm.sewingmachine.mixins.Entities;

import net.theelm.sewingmachine.enums.ClaimPermissions;
import net.theelm.sewingmachine.enums.ClaimSettings;
import net.theelm.sewingmachine.interfaces.IClaimedChunk;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created on Aug 16 2021 at 12:30 AM.
 * By greg in SewingMachineMod
 */
@Mixin(LightningEntity.class)
public abstract class Lightning extends Entity {
    
    @Nullable @Shadow
    private ServerPlayerEntity channeler;
    
    public Lightning(EntityType<?> type, World world) {
        super(type, world);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/block/BlockState.canPlaceAt(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z"), method = "spawnFire")
    private boolean canSpawnFireAt(@NotNull BlockState state, @NotNull WorldView world, @NotNull BlockPos pos, int attempts) {
        IClaimedChunk chunk = (IClaimedChunk) world.getChunk(pos);
        if (!chunk.isSetting(pos, ClaimSettings.WEATHER_GRIEFING)) {
            if (this.channeler == null || !chunk.canPlayerDo(pos, this.channeler.getUuid(), ClaimPermissions.BLOCKS))
                return false;
        }
        return state.canPlaceAt(world, pos);
    }
}
