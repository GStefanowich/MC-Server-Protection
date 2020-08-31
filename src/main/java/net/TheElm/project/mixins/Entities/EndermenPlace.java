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
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(targets = "net/minecraft/entity/mob/EndermanEntity$PlaceBlockGoal")
public abstract class EndermenPlace extends Goal implements EndermanGoal {
    
    @Shadow
    private EndermanEntity enderman;
    @Shadow
    public abstract boolean method_7033(WorldView worldView_1, BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2, BlockState blockState_3, BlockPos blockPos_2);
    
    @Override
    public void tick() {
        // Get endermans information
        Random random = this.enderman.getRandom();
        WorldAccess world = this.enderman.world;
        
        // Get random vector
        int int_1 = MathHelper.floor(this.enderman.getX() - 1.0D + random.nextDouble() * 2.0D);
        int int_2 = MathHelper.floor(this.enderman.getY() + random.nextDouble() * 2.0D);
        int int_3 = MathHelper.floor(this.enderman.getZ() - 1.0D + random.nextDouble() * 2.0D);
        
        // Get the block position to target
        BlockPos blockPositionPlace = new BlockPos(int_1, int_2, int_3);
        BlockState blockStatePlace = world.getBlockState(blockPositionPlace);
        
        // Get the ground below the target
        BlockPos blockPositionGround = blockPositionPlace.down();
        BlockState blockStateGround = world.getBlockState(blockPositionGround);
        
        BlockState carriedBlock = this.enderman.getCarriedBlock();
        
        if (carriedBlock != null && this.method_7033(world, blockPositionPlace, carriedBlock, blockStatePlace, blockStateGround, blockPositionGround)) {
            
            // Get the chunk permissions
            WorldChunk chunk = world.getWorld().getWorldChunk( blockPositionPlace );
            
            // Check if enderman griefing is allowed (Invert because FALSE == NOT ALLOWED)
            if ((chunk != null) && (!((IClaimedChunk) chunk).isSetting( blockPositionPlace, ClaimSettings.ENDERMAN_GRIEFING ))) {
                this.sadEnderman();
                return;
            }
            
            world.setBlockState(blockPositionPlace, carriedBlock, 3);
            this.enderman.setCarriedBlock((BlockState)null);
        }
    }
    
    private void sadEnderman() {
        this.enderman.playSound( SoundEvents.ENTITY_ENDERMAN_SCREAM, 1, 1 );
    }
    
}
