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

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.goals.EatMyceliumGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CowEntity.class)
public abstract class Cow extends AnimalEntity {
    
    private EatMyceliumGoal eatMyceliumGoal;
    private int eatMyceliumTimer;
    
    protected Cow(EntityType<? extends AnimalEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void mobTick() {
        if (this.eatMyceliumGoal != null)
            this.eatMyceliumTimer = this.eatMyceliumGoal.getTimer();
        super.mobTick();
    }
    
    @Inject(at = @At("TAIL"), method = "initGoals")
    protected void createGoals(final CallbackInfo callback) {
        if (SewConfig.get(SewConfig.COWS_EAT_MYCELIUM)) {
            this.eatMyceliumGoal = new EatMyceliumGoal(this);
            this.goalSelector.add(5, this.eatMyceliumGoal);
        } else {
            this.eatMyceliumGoal = null;
        }
    }
    
    @Override
    public void onEatingGrass() {
        this.world.addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getBodyY(0.5D), this.getZ(), 0.0D, 0.0D, 0.0D);
        if (!this.world.isClient) {
            // Remove this entity
            this.remove();
            
            MooshroomEntity mooshroom = EntityType.MOOSHROOM.create(this.world);
            assert mooshroom != null;
            
            // Update health health
            mooshroom.setHealth(this.getHealth());
            
            // Update positioning
            mooshroom.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.yaw, this.pitch);
            mooshroom.bodyYaw = this.bodyYaw;
            mooshroom.setBreedingAge(this.getBreedingAge());
            
            // If the entity has a custom name
            if (this.hasCustomName()) {
                mooshroom.setCustomName(this.getCustomName());
                mooshroom.setCustomNameVisible(this.isCustomNameVisible());
            }
            
            // If the entity is persistant
            if (this.isPersistent())
                mooshroom.setPersistent();
            
            // Copy invulnerability status
            mooshroom.setInvulnerable(this.isInvulnerable());
            
            // Spawn the new cow
            this.world.spawnEntity(mooshroom);
        }
    }
    
}
