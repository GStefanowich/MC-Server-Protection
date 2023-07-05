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

import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = {SheepEntity.class, MooshroomEntity.class, CowEntity.class, PigEntity.class, OcelotEntity.class, WolfEntity.class, ParrotEntity.class, CatEntity.class}, priority = 1)
public abstract class AnimalInteract extends AnimalEntity {
    protected AnimalInteract(EntityType<? extends AnimalEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    private void playerInteractMod(final PlayerEntity player, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        BlockPos mobPositioning = this.getBlockPos();
        
        // Check if the mob is owned by the player interacting
        if (((AnimalEntity)this) instanceof TameableEntity tameable) {
            if (Objects.equals(tameable.getOwnerUuid(), player.getUuid()))
                return;
        }
        
        if (!ClaimChunkUtils.canPlayerInteractFriendlies(player, mobPositioning))
            callback.setReturnValue(ActionResult.FAIL);
    }
}
