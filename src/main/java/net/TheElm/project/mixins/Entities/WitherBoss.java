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

import net.TheElm.project.interfaces.ConstructableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(WitherEntity.class)
public abstract class WitherBoss extends HostileEntity implements RangedAttackMob, ConstructableEntity {
    
    UUID chunkSpawnedInOwner = null;
    
    protected WitherBoss(EntityType<? extends HostileEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At(value = "INVOKE", target = "net/minecraft/world/World.syncGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V", shift = At.Shift.BEFORE), method = "mobTick")
    public void overrideWorldSound(CallbackInfo callback) {
        this.world.syncGlobalEvent(1023, this.getBlockPos(), 0);
    }
    
    @Override
    public @Nullable UUID getEntityOwner() {
        return this.chunkSpawnedInOwner;
    }
    
    @Override
    public void setEntityOwner(UUID owner) {
        this.chunkSpawnedInOwner = owner;
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        // Save the players money
        if (this.getEntityOwner() != null)
            tag.putUuid( "WitherCreator", this.getEntityOwner() );
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        // Read the players money
        if (tag.containsUuid( "WitherCreator" ))
            this.setEntityOwner(tag.getUuid("WitherCreator" ));
    }
    
}
