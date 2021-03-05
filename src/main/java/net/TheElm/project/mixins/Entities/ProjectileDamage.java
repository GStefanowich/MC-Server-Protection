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

import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ArrowEntity.class, TridentEntity.class, SpectralArrowEntity.class})
public abstract class ProjectileDamage extends PersistentProjectileEntity {
    
    protected ProjectileDamage(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.entityHitShouldApplyDamage(entityHitResult))
            super.onEntityHit(entityHitResult);
    }
    
    private boolean entityHitShouldApplyDamage(EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();
        
        // Ignore if arrow came from a non-player
        if ( this.getOwner() == null )
            return true;
        
        // Get the chunk that the player was harmed in
        WorldChunk chunk = hitEntity.getEntityWorld().getWorldChunk( hitEntity.getBlockPos() );
        if ( ( chunk != null ) && ( this.getOwner() instanceof ServerPlayerEntity ) ) {
            IClaimedChunk chunkInfo = (IClaimedChunk) chunk;
            ServerPlayerEntity owner = (ServerPlayerEntity) this.getOwner();
            
            // If player hurt themselves
            if ( owner.getUuid().equals( hitEntity.getUuid() ) )
                return true;
            
            if (hitEntity instanceof Monster) {
                /*
                 * Allow defending from hostiles
                 */
                return true;
                
            } if (hitEntity instanceof ServerPlayerEntity) {
                /*
                 * If the hit target is a player, check for PvP
                 */
                
                // If PvP is enabled, allow
                if ( chunkInfo.isSetting(hitEntity.getBlockPos(), ClaimSettings.PLAYER_COMBAT) )
                    return true;
                
            } else {
                /*
                 * If the hit target is a friendly
                 */
                
                // If entity is a tameable (Horse/Wolf/Cat) and is tamed
                if ((hitEntity instanceof TameableEntity) && (((TameableEntity)hitEntity).getOwnerUuid() != null )) {
                    TameableEntity pet = (TameableEntity) hitEntity;
                    
                    // Player can hit their owned tamed
                    if (owner.getUuid().equals(pet.getOwnerUuid()))
                        return true;
                    
                    // If pet is currently targeting something (attacking)
                    LivingEntity petAttacking = ((TameableEntity) hitEntity).getTarget();
                    if (petAttacking instanceof TameableEntity || petAttacking instanceof ServerPlayerEntity)
                        return true;
                    
                    // If player is allowed to harm tamed creatures
                    if (chunkInfo.isSetting(hitEntity.getBlockPos(), ClaimSettings.HURT_TAMED) )
                        return true;
                    
                } else {
                    /*
                     * If entity is a creature (Cow/Sheep/Chicken/Pig) or untamed
                     */
                    
                    // If player is allowed to harm creatures
                    if (chunkInfo.canPlayerDo(new BlockPos(entityHitResult.getPos()), owner.getUuid(), ClaimPermissions.CREATURES))
                        return true;
                }
            }
            
            // Play a sound
            hitEntity.playSound(this.getSound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            
            // If piercing is 0, delete arrow
            if (this.getPierceLevel() <= 0) {
                this.remove();
            }
            
            // Cancel damaging effects
            return false;
        }
        
        return true;
    }
    
}
