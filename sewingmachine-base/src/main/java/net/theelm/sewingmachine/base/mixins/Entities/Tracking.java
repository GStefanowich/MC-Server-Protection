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

package net.theelm.sewingmachine.base.mixins.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.theelm.sewingmachine.interfaces.TrackedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created on May 11 2022 at 8:54 PM.
 * By greg in SewingMachineMod
 */
@Mixin(Entity.class)
public abstract class Tracking implements TrackedEntity {
    private int customTrackingRange = 0;
    private boolean hasCustomTracking = false;
    
    @Override
    public int getTrackingRange() {
        return this.customTrackingRange;
    }
    
    @Override
    public boolean hasCustomTracking() {
        return this.hasCustomTracking;
    }
    
    @Inject(at = @At("TAIL"), method = "readNbt")
    public void onNbtRead(NbtCompound tag, CallbackInfo callback) {
        if (this.hasCustomTracking = tag.contains("TrackingRange", NbtElement.NUMBER_TYPE))
            this.customTrackingRange = tag.getInt("TrackingRange");
    }
    
    @Inject(at = @At("TAIL"), method = "writeNbt")
    public void onNbtWrite(NbtCompound tag, CallbackInfoReturnable<NbtCompound> callback) {
        if (this.hasCustomTracking)
            tag.putInt("TrackingRange", this.customTrackingRange);
    }
}
