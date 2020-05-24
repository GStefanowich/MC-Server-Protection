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

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.VillagerTownie;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InteractionObserver;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(VillagerEntity.class)
public abstract class Villager extends AbstractTraderEntity implements InteractionObserver, VillagerDataContainer, VillagerTownie {
    
    private UUID town = null;
    
    public Villager(EntityType<? extends AbstractTraderEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Shadow private native void sayNo();
    @Shadow public abstract VillagerData getVillagerData();
    
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    public void onAttemptInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> callback) {
        if (!ChunkUtils.canPlayerTradeAt( player, this.getBlockPos() )) {
            // Shake head
            this.sayNo();
            
            // Increment server statistic
            player.incrementStat(Stats.TALKED_TO_VILLAGER);
            
            // Set return value
            callback.setReturnValue( true );
        }
    }
    
    @Override
    public boolean setTown(ClaimantTown town) {
        if (town == null) {
            boolean wasNull = this.town == null;
            this.town = null;
            return wasNull;
        } else {
            if (town.getId().equals(this.town))
                return false;
            this.town = town.getId();
        }
        return town.addVillager((VillagerEntity)(Entity) this);
    }
    protected ClaimantTown getTown() {
        ClaimantTown town = null;
        if (this.town != null) {
            try {
                town = ClaimantTown.get(this.town);
            } catch (NbtNotFoundException ignored) {}
            // Reset the town
            if (town == null) this.setTown(null);
        }
        return town;
    }
    
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource source, CallbackInfo callback) {
        ClaimantTown town = this.getTown();
        if (town != null) town.removeVillager((VillagerEntity)(Entity)this);
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        if (this.town != null)
            tag.putString("smTown", this.town.toString());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains("smTown", NbtType.STRING)) {
            // Load the UUID from the tag
            this.town = UUID.fromString(tag.getString("smTown"));
            
            // Make sure the town is in the cache (Or return town to null if not exists)
            this.getTown();
        }
    }
    @Inject(at = @At("RETURN"), method = "readCustomDataFromTag")
    public void afterReadingData(CompoundTag tag, CallbackInfo callback) {
        if ((!this.hasCustomName()) && SewingMachineConfig.INSTANCE.RANDOM_NAME_VILLAGERS.get()) {
            Text name = EntityUtils.Naming.create(this.random, this.getVillagerData(), 4);
            if (name != null) {
                /*
                 * Assign random villager name
                 */
                this.setCustomName(name);
                this.setCustomNameVisible(false);
            }
        }
    }
    
}
