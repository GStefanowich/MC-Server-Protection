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

package net.TheElm.project.mixins.Blocks;

import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class WaystoneBlock extends BlockEntity {
    
    private long tickTimer = 30;
    private UUID wayStoneOwner = null;
    
    public WaystoneBlock(BlockPos blockPos, BlockState blockState) {
        super(null, blockPos, blockState); // TODO: Replace NULL (Can't be null)
    }
    
    // TODO: Convert the tick to use the static ticking
    public void tick() {
        if ((this.wayStoneOwner != null) && (this.world != null)) {
            this.tickTimer--;
            
            // Check that enough time has passed
            if (this.tickTimer > 0)
                return;
            
            // Reset the timer
            this.tickTimer = 30;
            
            int low;
            int x = this.world.random.nextInt((this.pos.getX() + 2) - (low = (this.pos.getX() - 1))) + low;
            int y = this.world.random.nextInt((this.pos.getY() + 5) - (low = (this.pos.getY() + 3))) + low;
            int z = this.world.random.nextInt((this.pos.getZ() + 2) - (low = (this.pos.getZ() - 1))) + low;
            
            int shiftX = this.world.random.nextInt(100);
            int shiftZ = this.world.random.nextInt(100);
            
            ((ServerWorld)this.world).spawnParticles(ParticleTypes.PORTAL, x + (double)(shiftX / 100), y, z + (double)(shiftZ / 100), 8, 0.1D, -0.2D, 0.1D, 0.0D);
        }
    }
    
    /*
     * NBT read/write
     */
    
    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        
        if (NbtUtils.hasUUID(tag, "sewingWaystone"))
            this.wayStoneOwner = NbtUtils.getUUID(tag, "sewingWaystone");
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        if (this.wayStoneOwner != null)
            tag.putUuid( "sewingWaystone", this.wayStoneOwner );
        
        return super.writeNbt(tag);
    }
}