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

package net.TheElm.project.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class AvoidRainGoal extends Goal {
    // Mob information
    protected final PathAwareEntity mob;
    private final World world;
    
    // Get the position to move toward
    private double targetX;
    private double targetY;
    private double targetZ;
    
    public AvoidRainGoal(PathAwareEntity mob) {
        this.mob = mob;
        this.world = mob.world;
    }
    
    @Override
    public boolean canStart() {
        // If not raining, don't sweat it
        if (!world.isRaining())
            return false;
        // If tamed mob is sitting (Stay put)
        if ((this.mob instanceof TameableEntity) && ((TameableEntity) this.mob).isSitting())
            return false;
        Box box = this.mob.getBoundingBox();
        // If mob isn't visible to the sky
        if (!this.world.isSkyVisible(new BlockPos(this.mob.getX(), box.minY, this.mob.getZ())))
            return false;
        // Don't ALWAYS do it (Save the CPU!)
        if (this.mob.getRandom().nextInt(120) != 0)
            return false;
        // Find cover
        return this.mob.isTouchingWaterOrRain() && this.foundCover( locateCover() );
    }
    
    @Override
    public void start() {
        this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, 1.0D);
    }
    
    private boolean foundCover(Vec3d location) {
        if (location == null)
            return false;
        
        this.targetX = location.x;
        this.targetY = location.y;
        this.targetZ = location.z;
        
        return true;
    }
    
    @Nullable
    private Vec3d locateCover() {
        Random rand = this.mob.getRandom();
        Box box = this.mob.getBoundingBox();
        BlockPos blockPos = new BlockPos(this.mob.getX(), box.minY, this.mob.getZ());
        
        for( int i = 0; i < 10; ++i ) {
            BlockPos goalPos = blockPos.add( rand.nextInt(20) - 10, rand.nextInt(6) - 3, rand.nextInt(20) - 10 );
            if (!this.world.isSkyVisible(goalPos) && (!this.world.isWater(goalPos)) && this.mob.getPathfindingFavor(goalPos) < 0.0F) {
                return new Vec3d(goalPos.getX(), goalPos.getY(), goalPos.getZ());
            }
        }
        
        return null;
    }
    
}
