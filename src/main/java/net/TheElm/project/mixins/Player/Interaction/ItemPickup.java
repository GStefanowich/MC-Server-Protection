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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemEntity.class)
public abstract class ItemPickup extends Entity {
    
    @Shadow private int pickupDelay;
    @Shadow private UUID thrower;
    @Shadow private UUID owner;
    
    public ItemPickup(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "onPlayerCollision", cancellable = true)
    public void attemptPickup(PlayerEntity player, CallbackInfo callback) {
        if (!this.world.isClient) {
            if ( this.pickupDelay == 0 ) {
                // Check if the entity is owned by the player (They dropped it)
                if (player.getUuid().equals( this.thrower ) || player.getUuid().equals( this.owner ) || (player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS)))
                    return;
                
                // Check if the player can pickup items in the chunk
                BlockPos itemPos = this.getBlockPos();
                if (!ChunkUtils.canPlayerLootDropsInChunk(player, itemPos))
                    callback.cancel();
            }
        }
    }
    
    @Inject(at = @At("RETURN"), method = "damage")
    public void onDamage(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> callback) {
        ItemEntity entity = ((ItemEntity)(Entity)this);
        ItemStack stack = entity.getStack();
        if ((damageSource == DamageSource.LAVA) && (Items.GUNPOWDER.equals(stack.getItem()))) {
            float volume = ((float) stack.getCount() / stack.getMaxCount() );
            if (volume > 0)
                this.world.playSound( null, this.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.MASTER, volume, 1.0f );
        }
    }
    
}
