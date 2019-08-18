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

import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.CoreMod;
import net.TheElm.project.enums.ClaimSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperDamage extends HostileEntity {
    @Shadow
    private int explosionRadius;
    @Shadow
    private native void spawnEffectsCloud();
    @Shadow
    public abstract boolean isCharged();
    
    protected CreeperDamage(EntityType<? extends HostileEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "explode", cancellable = true)
    public void creeperExplodes(CallbackInfo callback) {
        BlockPos blockPos = this.getBlockPos();
        
        if ( this.world.isClient )
            return;
        
        // Get the chunk info
        ClaimedChunk claimedChunkInfo = ClaimedChunk.convert( this.getEntityWorld(), blockPos );
        if ( claimedChunkInfo != null ) {
            // If the creeper griefing is disallowed
            if ( claimedChunkInfo.isSetting( ClaimSettings.CREEPER_GRIEFING ) ) {
                
                // Log the creeper explosion
                CoreMod.logMessage("Stopped creeper block damage at X " + blockPos.getX() + ", Z" + blockPos.getZ() + ", Y" + blockPos.getZ() + ".");
                
                // Imitate the real explosion that happened;
                this.dead = true;
                this.world.createExplosion(this,
                    this.x,
                    this.y,
                    this.z,
                    (float) this.explosionRadius * ( this.isCharged() ? 2.0F : 1.0F ), // The explosion radius based on the charge
                    Explosion.DestructionType.NONE // NO EXPLOSION
                );
                this.remove(); // Delete our creeper
                this.spawnEffectsCloud(); // Spawn the explosion cloud
                
                // Stop the real explosion
                callback.cancel();
            }
        }
    }
    
}
