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

import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class Death extends Entity {
    
    @Shadow protected boolean dead;
    @Shadow public abstract boolean addStatusEffect(StatusEffectInstance statusEffectInstance);
    
    @Shadow public abstract void setHealth(float f);
    @Shadow public abstract boolean clearStatusEffects();
    @Shadow public abstract ItemStack getStackInHand(Hand hand);
    
    public Death(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /*
     * Add mobs to a Mob Spawner
     */
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource damageSource, CallbackInfo callback) {
        // If disabled
        if (!SewConfig.get(SewConfig.SPAWNER_ABSORB_MOBS))
            return;
        
        // If not dead or is player
        if ((!this.dead) || (((Entity) this) instanceof PlayerEntity))
            return;
        
        // If attacker is not a player
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player))
            return;
        
        NbtCompound spawnerTag;
        
        // Get the attacker
        ItemStack itemStack = player.getStackInHand(Hand.OFF_HAND);
        if ((!(itemStack.getItem().equals(Items.SPAWNER))) || ((spawnerTag = itemStack.getNbt()) == null) || ((spawnerTag = spawnerTag.copy()) == null) || (!spawnerTag.contains("EntityIds", NbtElement.LIST_TYPE)))
            return;
        
        // Check if mob type is allowed to be spawned
        EntityType<?> type = this.getType();
        if (!EntityUtils.canBeSpawnered(type))
            return;
        
        // Get the identifier of the mob we killed
        NbtString mobId = NbtString.of(EntityType.getId(type).toString());
        
        // Get current entity IDs
        NbtList entityIds = spawnerTag.getList("EntityIds", NbtElement.STRING_TYPE);
        int rolls = 1 + EnchantmentHelper.getLevel(Enchantments.LOOTING, player.getMainHandStack());
        
        // Spawn particles
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.SOUL,
            this.getX(),
            this.getY(),
            this.getZ(),
            8 * rolls,
            0.25D,
            0.5D,
            0.25D,
            0.01D
        );
        
        for (int roll = 0; roll < rolls; ++roll) {
            Integer random = null;
            // Test the odds
            if ((!entityIds.contains(mobId)) && ((random = player.world.getRandom().nextInt(800)) == 0)) {
                // Add mob to the list
                entityIds.add(mobId);
                
                // Update the dropped items tag
                spawnerTag.put("EntityIds", entityIds);
                spawnerTag.put("display", NbtUtils.getSpawnerDisplay(entityIds));
                
                // Play sound
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 1, false, false));
                
                // Should drop a new spawner
                boolean dropNew = itemStack.getCount() > 1;
                
                // Update the existing item in hand
                if (dropNew) {
                    // Update a new item (NOT an entire stack)
                    itemStack.decrement(1);
                    itemStack = new ItemStack(Items.SPAWNER);
                }
                
                // Update the itemstack
                itemStack.setNbt(spawnerTag);
                
                // Give the player
                if (dropNew)
                    player.getInventory()
                        .offerOrDrop(itemStack);
                break;
            }
            System.out.println(random);
        }
    }
    
    /*
     * Check for totem of undying
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/LivingEntity.tryUseTotem(Lnet/minecraft/entity/damage/DamageSource;)Z"), method = "damage")
    public boolean onUseTotem(@NotNull LivingEntity entity, @NotNull DamageSource source) {
        if (source.isOutOfWorld())
            return false;
        else {
            ItemStack totem = null;
            
            // Check the hands
            Hand[] hands = Hand.values();
            for (int slot = 0; slot < hands.length; slot++) {
                ItemStack hand = this.getStackInHand(hands[slot]);
                if (hand.getItem() == Items.TOTEM_OF_UNDYING) {
                    totem = hand.copy();
                    hand.decrement(1);
                    break;
                }
            }
            
            // Check the inventory
            if (totem == null && (entity instanceof ServerPlayerEntity player) && SewConfig.get(SewConfig.TOTEM_ANYWHERE)) {
                for (int slot = 0; slot < player.getInventory().size(); slot++) {
                    ItemStack pack = player.getInventory().getStack(slot);
                    if (pack.getItem() == Items.TOTEM_OF_UNDYING) {
                        totem = pack.copy();
                        pack.decrement(1);
                        break;
                    }
                }
            }
            
            // If a totem was found
            if (totem != null) {
                // If totem user is a player, increase stats
                if (entity instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));
                    Criteria.USED_TOTEM.trigger(serverPlayer, totem);
                }
                
                // Set the health back
                this.setHealth(1.0F);
                this.clearStatusEffects();
                
                // Apply status effects
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
                
                // Update the entity status as alive
                this.world.sendEntityStatus(this, (byte)35);
            }
            
            return totem != null;
        }
    }
    
    /*
     * Check for falling into the Void in The End
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/LivingEntity.damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), method = "tickInVoid")
    protected boolean onDamage(@NotNull LivingEntity self, @NotNull DamageSource source, float damage) {
        if (source.equals(DamageSource.OUT_OF_WORLD) && !self.world.isInBuildLimit(this.getBlockPos())) {
            // If the player isn't actually falling (Break the teleport loop and give time to update ticks)
            if (self.fallDistance < 0.25d)
                return false;
            
            // If the player can fall from the end into the sky of the overworld
            if (World.END.equals(self.world.getRegistryKey()) && SewConfig.get(SewConfig.END_FALL_FROM_SKY)) {
                WarpUtils.teleportEntity(World.OVERWORLD, this, new BlockPos(this.getX(), 400, this.getZ()));
                return false;
            }
            
            // If the player can fall from any worlds void back to spawn
            if (SewConfig.get(SewConfig.VOID_FALL_TO_SPAWN) && !(self instanceof HostileEntity)) { // Teleport to the spawn world
                WarpUtils.teleportEntity(ServerCore.defaultWorldKey(), this);
                return false;
            }
        }
        
        return self.damage(source, damage);
    }
}
