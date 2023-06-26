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

package net.theelm.sewingmachine.protection.mixins.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InteractionObserver;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.world.World;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.VillagerTownie;
import net.theelm.sewingmachine.protection.utilities.ChunkUtils;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements InteractionObserver, VillagerDataContainer, VillagerTownie {
    // TODO: Attempt saving the town UUID as a Memory, not directly as a property
    /*private static final MemoryModuleType<UUID> TOWN;*/
    private UUID town = null;
    
    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Shadow private native void sayNo();
    @Shadow public abstract VillagerData getVillagerData();
    
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    public void onAttemptInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (!ChunkUtils.canPlayerTradeAt(player, this.getBlockPos())) {
            // Shake head
            this.sayNo();

            // Increment server statistic
            player.incrementStat(Stats.TALKED_TO_VILLAGER);

            // Set return value
            callback.setReturnValue(ActionResult.FAIL);
        }
    }
    
    @Override
    public boolean setTown(ClaimantTown town) {
        if (town == null) {
            boolean wasNull = this.town == null;
            if (!wasNull) {
                ClaimantTown reference = this.getTown();
                reference.removeVillager((VillagerEntity)(Entity)this);
            }
            this.town = null;
            return wasNull;
        } else {
            if (town.getId().equals(this.town))
                return false;
            this.town = town.getId();
        }
        return town.addVillager((VillagerEntity)(Entity) this);
    }
    
    @Override
    public ClaimantTown getTown() {
        ClaimantTown town = null;
        if (this.town != null) {
            town = ((ClaimsAccessor)this.getServer()).getClaimManager()
                .getTownClaim(this.town);
            
            // Reset the town
            if (town == null)
                this.setTown(null);
        }
        return town;
    }
    
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource source, CallbackInfo callback) {
        this.setTown(null);
    }
    
    @Inject(at = @At("RETURN"), method = "createChild")
    public void onBirth(ServerWorld world, PassiveEntity spouse, CallbackInfoReturnable<VillagerEntity> callback) {
        VillagerEntity child = callback.getReturnValue();
        
        // Give the baby a name
        if (SewConfig.get(SewCoreConfig.RANDOM_NAME_VILLAGERS)) {
            Text name = EntityUtils.Naming.create(child.getRandom(), child.getVillagerData(), 4);
            if (name != null) {
                /*
                 * Assign random villager name
                 */
                child.setCustomName(name);
                child.setCustomNameVisible(false);
            }
        }
        
        // Add the baby to the Town
        if (SewConfig.get(SewCoreConfig.TOWN_VILLAGERS_INCLUDE)) {
            BlockPos pos = spouse.getBlockPos();
            ClaimantTown town = ((IClaimedChunk)this.getWorld().getChunk(pos))
                .getTown();
            
            // If the villager isn't standing in a town, try reading their own town
            if (town == null)
                town = this.getTown();
            
            // Try reading their spouses town too
            if (town == null && spouse instanceof VillagerEntity partner)
                town = ((VillagerTownie) partner).getTown();
            
            if ((town != null) && ((VillagerTownie)child).setTown(town)) {
                // Get names of all involved villagers
                Text pName = null,
                    sName = null,
                    bName = null;
                
                if (SewConfig.get(SewCoreConfig.RANDOM_NAME_VILLAGERS)) {
                    if (this.hasCustomName() && spouse.hasCustomName()) {
                        pName = ColorUtils.format(this.getDisplayName(), Formatting.WHITE);
                        sName = ColorUtils.format(spouse.getDisplayName(), Formatting.WHITE);
                    }
                    if (child.hasCustomName())
                        bName = ColorUtils.format(child.getDisplayName(), Formatting.WHITE);
                }
                
                town.send(
                    this.getServer(),
                    ((pName != null && sName != null && bName != null) ?
                        TextUtils.literal().append(pName).append(" and ")
                            .append(sName).append(" have welcome a new villager, ")
                            .append(bName).append(", into your town.")
                        : (bName != null ?
                        Text.literal("A new villager, ").append(bName).append(", has been welcomed into your town.")
                        : Text.literal("A new villager has been welcomed into your town.")
                    )
                    ).formatted(Formatting.GRAY, Formatting.ITALIC)
                );
            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(NbtCompound tag, CallbackInfo callback) {
        if (this.town != null)
            tag.putString("smTown", this.town.toString());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(NbtCompound tag, CallbackInfo callback) {
        if (tag.contains("smTown", NbtElement.STRING_TYPE)) {
            // Load the UUID from the tag
            this.town = UUID.fromString(tag.getString("smTown"));

            // Make sure the town is in the cache (Or return town to null if not exists)
            this.getTown();
        }
    }
}

