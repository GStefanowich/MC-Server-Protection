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

package net.theelm.sewingmachine.base.mixins.Blocks;

import net.theelm.sewingmachine.enums.ClaimPermissions;
import net.theelm.sewingmachine.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.utilities.ChunkUtils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created on Aug 29 2021 at 2:30 PM.
 * By greg in SewingMachineMod
 */
@Mixin(TntBlock.class)
public abstract class TntBlockMixin extends Block {
    public TntBlockMixin(AbstractBlock.Settings settings) {
        super(settings);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/projectile/ProjectileEntity.isOnFire()Z"), method = "onProjectileHit")
    public boolean onCheckEntityHitFire(@NotNull ProjectileEntity instance, @NotNull World world, @NotNull BlockState state, @NotNull BlockHitResult hit, @NotNull ProjectileEntity projectile) {
        boolean burning = instance.isOnFire();
        if (burning) {
            WorldChunk chunk = world.getWorldChunk(hit.getBlockPos());
            if (((IClaimedChunk)chunk).getOwnerId() != null) {
                boolean allow = instance.getOwner() instanceof ServerPlayerEntity && ChunkUtils.canPlayerDoInChunk(
                    ClaimPermissions.BLOCKS,
                    (ServerPlayerEntity)instance.getOwner(),
                    chunk,
                    hit.getBlockPos()
                );
                if (!allow) {
                    instance.extinguish();
                    if (!world.isClient) {
                        world.playSound(
                            null,
                            instance.getX(),
                            instance.getY(),
                            instance.getZ(),
                            SoundEvents.BLOCK_FIRE_EXTINGUISH,
                            SoundCategory.BLOCKS,
                            1.0f,
                            1.0f
                        );
                    }
                }
                return allow;
            }
        }
        return burning;
    }
}
