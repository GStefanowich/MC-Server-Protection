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

package net.TheElm.project.mixins.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FireballEntity.class)
public abstract class FireballFix extends AbstractFireballEntity {
    
    @Shadow public int explosionPower;
    
    public FireballFix(EntityType<? extends AbstractFireballEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /**
     * @author TheElm
     * @reason Mojang decided to pass (NULL) as the explosion entity??? :thonkJang:
     */
    @Overwrite
    public void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (this.world.isClient)
            return;
        boolean doMobGriefing;
        
        // If the projecticle hit an entity, damage that entity
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult)hitResult).getEntity();
            entity.damage(DamageSource.explosiveProjectile(this, this.owner), 6.0F);
            this.dealDamage(this.owner, entity);
        }
        
        // Create the explosion
        this.world.createExplosion(
            this,
            this.getX(),
            this.getY(),
            this.getZ(),
            (float)this.explosionPower,
            (doMobGriefing = this.world.getGameRules().getBoolean(GameRules.MOB_GRIEFING)),
            (doMobGriefing ? Explosion.DestructionType.DESTROY : Explosion.DestructionType.NONE)
        );
        
        // Remove this fireball
        this.remove();
    }
    
}
