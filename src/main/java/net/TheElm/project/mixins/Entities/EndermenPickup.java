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

import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.EndermanGoal;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(targets = "net/minecraft/entity/mob/EndermanEntity$PickUpBlockGoal")
public abstract class EndermenPickup extends Goal implements EndermanGoal {
    
    @Shadow
    private EndermanEntity enderman;
    
    @Override
    public void tick() {
        // Get endermans information
        Random random = this.enderman.getRandom();
        World world = this.enderman.getEntityWorld();
        
        // Get random vector
        int int_1 = MathHelper.floor(this.enderman.getX() - 2.0D + random.nextDouble() * 4.0D);
        int int_2 = MathHelper.floor(this.enderman.getY() + random.nextDouble() * 3.0D);
        int int_3 = MathHelper.floor(this.enderman.getZ() - 2.0D + random.nextDouble() * 4.0D);
        
        // Get the block position to target
        BlockPos blockPos = new BlockPos(int_1, int_2, int_3);
        
        // Get the block info
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        
        // Get the vector
        Vec3d vec3d_1 = new Vec3d((double) MathHelper.floor(this.enderman.getX()) + 0.5D, (double) int_2 + 0.5D, (double) MathHelper.floor(this.enderman.getZ()) + 0.5D);
        Vec3d vec3d_2 = new Vec3d((double) int_1 + 0.5D, (double) int_2 + 0.5D, (double) int_3 + 0.5D);
        
        // Attack the block
        BlockHitResult blockHitResult = world.rayTrace(new RayTraceContext(vec3d_1, vec3d_2, RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, this.enderman));
        
        boolean bool = blockHitResult.getType() != HitResult.Type.MISS && blockHitResult.getBlockPos().equals(blockPos);
        
        // If the block is able to be held my endermen
        if (block.isIn(BlockTags.ENDERMAN_HOLDABLE) && bool) {
            
            // Get the chunk permissions
            WorldChunk chunk = world.getWorldChunk( blockPos );
            
            // Check if enderman griefing is allowed (Invert because FALSE == NOT ALLOWED)
            if ((chunk != null) && (!((IClaimedChunk) chunk).isSetting( blockPos, ClaimSettings.ENDERMAN_GRIEFING ))) {
                this.sadEnderman();
                return;
            }
            
            this.enderman.setCarriedBlock(blockState); // Set as held
            world.removeBlock(blockPos, false); // Remove from the world
        }
    }
    
    private void sadEnderman() {
        this.enderman.getEntityWorld()
            .playSoundFromEntity((PlayerEntity)null, this.enderman, SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1, 1);
    }
    
}
