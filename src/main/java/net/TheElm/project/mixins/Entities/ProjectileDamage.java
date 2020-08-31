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
import net.TheElm.project.interfaces.EntityOwner;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class ProjectileDamage extends Entity implements EntityOwner {
    
    @Shadow
    public abstract byte getPierceLevel();
    
    @Shadow private SoundEvent sound;
    
    public ProjectileDamage(EntityType<?> type, World world) {
        super(type, world);
    }
    
    @Inject(at = @At("HEAD"), method = "onEntityHit", cancellable = true)
    protected void onHitTarget(EntityHitResult entityHitResult, CallbackInfo callback) {
        Entity hitEntity = entityHitResult.getEntity();
        
        // Ignore if arrow came from a non-player
        if ( this.getOwner() == null )
            return;
        
        // Get the chunk that the player was harmed in
        WorldChunk chunk = hitEntity.getEntityWorld().getWorldChunk( hitEntity.getBlockPos() );
        if ( ( chunk != null ) && ( this.getOwner() instanceof ServerPlayerEntity ) ) {
            IClaimedChunk chunkInfo = (IClaimedChunk) chunk;
            ServerPlayerEntity owner = (ServerPlayerEntity) this.getOwner();
            
            // If player hurt themselves
            if ( owner.getUuid().equals( hitEntity.getUuid() ) )
                return;
            
            if (hitEntity instanceof Monster) {
                /*
                 * Allow defending from hostiles
                 */
                return;
                
            } if (hitEntity instanceof ServerPlayerEntity) {
                /*
                 * If the hit target is a player, check for PvP
                 */
                
                // If PvP is enabled, allow
                if ( chunkInfo.isSetting(hitEntity.getBlockPos(), ClaimSettings.PLAYER_COMBAT) )
                    return;
                
            } else {
                /*
                 * If the hit target is a friendly
                 */
                
                // If entity is a tameable (Horse/Wolf/Cat) and is tamed
                if ((hitEntity instanceof TameableEntity) && (((TameableEntity)hitEntity).getOwnerUuid() != null )) {
                    TameableEntity pet = (TameableEntity) hitEntity;
                    
                    // Player can hit their owned tamed
                    if (owner.getUuid().equals(pet.getOwnerUuid()))
                        return;
                    
                    // If pet is currently targeting something (attacking)
                    LivingEntity petAttacking = ((TameableEntity) hitEntity).getTarget();
                    if (petAttacking instanceof TameableEntity || petAttacking instanceof ServerPlayerEntity)
                        return;
                    
                    // If player is allowed to harm tamed creatures
                    if (chunkInfo.isSetting(hitEntity.getBlockPos(), ClaimSettings.HURT_TAMED) )
                        return;
                    
                } else {
                    /*
                     * If entity is a creature (Cow/Sheep/Chicken/Pig) or untamed
                     */
                    
                    // If player is allowed to harm creatures
                    if (chunkInfo.canPlayerDo(new BlockPos(entityHitResult.getPos()), owner.getUuid(), ClaimPermissions.CREATURES))
                        return;
                }
            }
            
            // Play a sound
            hitEntity.playSound( this.sound, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            
            // If piercing is 0, delete arrow
            if (this.getPierceLevel() <= 0) {
                this.remove();
            }
            
            // Cancel damaging effects
            callback.cancel();
            
        }
    }
    
}
