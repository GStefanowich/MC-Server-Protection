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

package net.theelm.sewingmachine.protection.mixins.Entities;

import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TntEntity - TNT
 * CreeperEntity - Creepers
 * WitherEntity - Wither mob
 * ExplosiveProjectileEntity - Wither projects
 */
@Mixin(value = {TntEntity.class, CreeperEntity.class, WitherEntity.class, ExplosiveProjectileEntity.class}, priority = 10000)
public abstract class ExplodingEntitiesMixin extends Entity {
    public ExplodingEntitiesMixin(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public boolean canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos blockPos, @NotNull BlockState blockState, float damage) {
        if ((!blockState.isAir()) && (world instanceof ServerWorld serverWorld)) {
            ActionResult result = BlockBreakCallback.TEST.invoker()
                .destroy(this, serverWorld, Hand.MAIN_HAND, blockPos, null, null);
            if (result != ActionResult.PASS)
                return (result == ActionResult.SUCCESS);
        }
        return super.canExplosionDestroyBlock(explosion, world, blockPos, blockState, damage);
    }
}
