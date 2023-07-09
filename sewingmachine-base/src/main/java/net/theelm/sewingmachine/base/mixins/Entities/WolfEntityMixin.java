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

package net.theelm.sewingmachine.base.mixins.Entities;

import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin extends TameableEntity implements Angerable {
    protected WolfEntityMixin(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Inject(at = @At(value = "HEAD", shift = At.Shift.AFTER), method = "damage", cancellable = true)
    public void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (this.isTamed() && source != null) {
            if (source.getAttacker() instanceof PlayerEntity && !SewConfig.get(SewBaseConfig.WOLF_DAMAGE_BOOST_PLAYERS))
                return;
            
            if (this.isInvulnerableTo(source))
                callback.setReturnValue(false);
            else {
                Entity entity = source.getAttacker();
                this.setSitting(false);
                if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity))
                    amount = (amount + 1.0F) / 2.0F;
                
                // Call damage at half the value
                callback.setReturnValue(super.damage(source, amount / SewConfig.get(SewBaseConfig.WOLF_DAMAGE_BUFF)));
            }
        }
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/Entity.damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), method = "tryAttack")
    public boolean onTryAttack(Entity entity, DamageSource source, float damage) {
        float buff = damage;
        if (this.isTamed() && (!(entity instanceof ServerPlayerEntity)))
            buff *= SewConfig.get(SewBaseConfig.WOLF_DAMAGE_RESIST);
        return entity.damage(source, buff);
    }
}
