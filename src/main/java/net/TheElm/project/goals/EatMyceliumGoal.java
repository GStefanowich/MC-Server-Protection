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

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.EnumSet;

public class EatMyceliumGoal extends Goal {
    private final MobEntity mob;
    private final World world;
    private int timer;
    
    public EatMyceliumGoal(MobEntity mobEntity) {
        this.mob = mobEntity;
        this.world = mobEntity.world;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }
    
    public boolean canStart() {
        if (this.mob.getRandom().nextInt( 50 ) != 0) {
            return false;
        } else {
            BlockPos blockPos = this.mob.getBlockPos();
            return this.world.getBlockState(blockPos.down()).getBlock() == Blocks.MYCELIUM;
        }
    }
    
    public void start() {
        this.timer = 40;
        this.world.sendEntityStatus(this.mob, (byte)10);
        this.mob.getNavigation().stop();
    }
    
    public void stop() {
        this.timer = 0;
    }
    
    public boolean shouldContinue() {
        return this.timer > 0;
    }
    
    public int getTimer() {
        return this.timer;
    }
    
    public void tick() {
        this.timer = Math.max(0, this.timer - 1);
        if (this.timer == 4) {
            BlockPos blockPos = this.mob.getBlockPos().down();
            if (this.world.getBlockState(blockPos).getBlock() == Blocks.MYCELIUM) {
                if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                    this.world.syncGlobalEvent(2001, blockPos, Block.getRawIdFromState(Blocks.MYCELIUM.getDefaultState()));
                    this.world.setBlockState(blockPos, Blocks.DIRT.getDefaultState(), 2);
                }
                
                this.mob.onEatingGrass();
            }
        }
    }
}
