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

import net.theelm.sewingmachine.utilities.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

/**
 * Created on Apr 24 2022 at 11:53 PM.
 * By greg in SewingMachineMod
 */
@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends PassiveEntity {
    private boolean autoBreed = false;
    private int breedAttemptTick = 0;
    
    @Shadow public abstract boolean canEat();
    @Shadow public abstract void lovePlayer(@Nullable PlayerEntity player);
    @Shadow public abstract boolean canBreedWith(AnimalEntity other);
    
    protected AnimalEntityMixin(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /**
     * During the mob tick check if this animal can breed automatically (Keep pens stocked!)
     * @param callback The Mixin Callback
     */
    @Inject(at = @At("TAIL"), method = "mobTick")
    private void onMobTick(CallbackInfo callback) {
        if (!this.getWorld().isClient && this.autoBreed && this.getBreedingAge() == 0 && this.canEat()) {
            if (this.breedAttemptTick++ >= 1100) {
                if (this.random.nextBoolean()) {
                    AnimalEntity mate = this.getNearbyMate();
                    if (mate != null) {
                        mate.lovePlayer(null);
                        this.lovePlayer(null);
                    }
                }
                this.breedAttemptTick = 0;
            }
        }
    }
    
    /**
     * Find a compatible mob nearby that can be mated with
     * @return The mate
     */
    private @Nullable AnimalEntity getNearbyMate() {
        Box box = Box.of(this.getPos(), 8, 2, 8);
        List<AnimalEntity> entities = this.getWorld()
            .getEntitiesByClass(AnimalEntity.class, box, animal -> Objects.equals(animal.getClass(), this.getClass()));
        if (entities.isEmpty() || entities.size() > 20)
            return null;
        return entities.stream()
            .filter(mate -> EntityUtils.areBreedable(this, mate))
            .findAny().orElse(null);
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Save the players money
        if (this.autoBreed)
            tag.putBoolean("Breeder", true);
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Read the players money
        if (tag.contains("Breeder", NbtElement.NUMBER_TYPE) && tag.getBoolean("Breeder"))
            this.autoBreed = true;
    }
}
