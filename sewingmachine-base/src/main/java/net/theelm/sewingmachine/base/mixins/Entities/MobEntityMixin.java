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

import net.theelm.sewingmachine.interfaces.SpawnerMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity implements SpawnerMob {
    private boolean wasFromSpawner = false;
    
    protected MobEntityMixin(EntityType<? extends AnimalEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public boolean checkIfFromSpawner() {
        return this.wasFromSpawner;
    }
    
    /**
     * Try to dismount mobs out of vehicles
     * @param player The player that interacted with this mob
     * @param hand The hand the player uses
     * @param callback The callback
     */
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    private void onInteractMob(@NotNull PlayerEntity player, @NotNull Hand hand, @NotNull final CallbackInfoReturnable<ActionResult> callback) {
        // If the player is not sneaking or is holding an item
        if (!player.isSneaking() || !player.getStackInHand(hand).isEmpty())
            return;
        Entity vehicle;
        if ((vehicle = this.getVehicle()) != null) {
            // Prevent removing baby striders from their parents
            if (((LivingEntity) this) instanceof StriderEntity && this.isBaby() && vehicle instanceof StriderEntity)
                return;
            // If mob is riding a player (Ie; a parrot) don't let other players dismount it
            if (vehicle instanceof PlayerEntity && player != vehicle)
                return;
            
            this.stopRiding();
            callback.setReturnValue(ActionResult.SUCCESS);
        }
    }
    
    /**
     * When the mob is initialized
     * We want to know if it is from a spawner
     * @param world World spawned in
     * @param difficulty Area difficulty
     * @param spawnReason Cause of spawning
     * @param entityData Current entity data
     * @param entityNbt Current entity nbt
     * @param callback Return type
     */
    @Inject(at = @At("HEAD"), method = "initialize")
    private void onInitializeMob(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt, CallbackInfoReturnable<EntityData> callback) {
        if (spawnReason == SpawnReason.SPAWNER || spawnReason == SpawnReason.SPAWN_EGG)
            this.wasFromSpawner = true;
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Save the players money
        if (this.checkIfFromSpawner())
            tag.putBoolean("FromSpawner", this.checkIfFromSpawner());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Read the players money
        if (tag.contains("FromSpawner", NbtElement.NUMBER_TYPE))
            this.wasFromSpawner = tag.getBoolean("FromSpawner");
    }
}
