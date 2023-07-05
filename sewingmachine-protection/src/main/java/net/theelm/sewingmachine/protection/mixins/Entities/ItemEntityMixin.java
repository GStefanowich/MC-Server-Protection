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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    @Shadow private int pickupDelay;

    @Shadow @Nullable private UUID thrower;

    @Shadow @Nullable private UUID owner;

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
    
    @Inject(at = @At("HEAD"), method = "onPlayerCollision", cancellable = true)
    public void attemptPickup(PlayerEntity player, CallbackInfo callback) {
        if (!this.getWorld().isClient()) {
            if ( this.pickupDelay == 0 ) {
                // Check if the entity is owned by the player (They dropped it)
                if (player.getUuid().equals(this.thrower) || player.getUuid().equals(this.owner) || (player.isCreative() && SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS)))
                    return;
                
                // Check if the player can pickup items in the chunk
                BlockPos itemPos = this.getBlockPos();
                if (!ClaimChunkUtils.canPlayerLootDropsInChunk(player, itemPos))
                    callback.cancel();
            }
        }
    }
}
