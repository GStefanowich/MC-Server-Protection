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

package net.theelm.sewingmachine.protection.mixins.Player.Interaction;

import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.EntityLockUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartEntity.class)
public abstract class MinecartEntityMixin extends AbstractMinecartEntity {
    protected MinecartEntityMixin(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "interact", cancellable = true)
    private void tryMinecartEnter(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> callback) {
        // Player is in creative
        if ((player.isCreative() && SewConfig.get(SewCoreConfig.CLAIM_CREATIVE_BYPASS)) || player.isSpectator())
            return;
        
        // If player can enter Minecart
        if (ClaimChunkUtils.canPlayerRideInChunk(player, this.getBlockPos()))
            return;
        
        // Player sound
        EntityLockUtils.playLockSoundFromSource(this, player);
        
        // Cancel the event
        callback.setReturnValue(false);
    }
    
}
