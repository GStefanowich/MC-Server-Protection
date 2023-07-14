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

package net.theelm.sewingmachine.protection.events;

import net.fabricmc.fabric.api.event.Event;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.Test;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.protection.mixins.Interfaces.LightningAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.EntityLockUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EntityAttack {
    private EntityAttack() {}
    
    /**
     * Initialize our callback listener for Entity Attacks
     */
    public static void register(@NotNull Event<DamageEntityCallback> event) {
        // Register our event for when an entity is attacked
        event.register(EntityAttack::attack);
    }
    
    /**
     * The event to trigger when an attack event occurs on an entity
     * @param target The target entity of the attack
     * @param world The world that the damage took place in
     * @param source The damage source, damage applied, attacker, etc
     * @return SUCCESS - Cancel and return true; PASS - Allow the interaction; FAIL - Cancel the interaction
     */
    private static Test attack(@NotNull final Entity target, @NotNull final World world, @NotNull final DamageSource source) {
        final Entity attacker = EntityAttack.getRootAttacker(source);
        
        // If self harm
        if (target == attacker)
            return Test.SUCCESS;
        
        if (attacker instanceof ServerPlayerEntity player)
            return EntityAttack.playerAttacks(player, target, world, source);
        if (attacker instanceof CreeperEntity creeper)
            return EntityAttack.creeperAttacks(creeper, target, world, source);
        
        // Pass for all other entity attackers
        return Test.CONTINUE;
    }
    
    private static Test playerAttacks(@NotNull ServerPlayerEntity attacker, @NotNull final Entity target, @NotNull final World world, @NotNull final DamageSource source) {
        // Always allow defending self from hostiles
        if ((target instanceof Monster) && (!(target instanceof AbstractPiglinEntity abstractPiglin) || abstractPiglin.getTarget() instanceof PlayerEntity))
            return Test.SUCCESS;
        
        // If the player is in creative, allow
        if (attacker.isCreative() && SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS))
            return Test.SUCCESS;
        
        // Get chunk protection
        WorldChunk chunk = world.getWorldChunk(target.getBlockPos());
        
        // If entity is a player always allow PvP, and always allow defending self from hostiles
        if ((target instanceof PlayerEntity)) {
            if (chunk != null) {
                // If PvP is disallowed, stop the swing
                if (!((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.PLAYER_COMBAT))
                    return Test.FAIL;
            }
            
            return Test.SUCCESS;
        }
        
        // Check if the tamed entity is tamed
        if ((target instanceof TameableEntity tameableEntity) && (((TameableEntity) target).getOwnerUuid() != null)) {
            // Deny if the entity belongs to the attacker (Can't hurt friendlies)
            if (attacker.getUuid().equals(tameableEntity.getOwnerUuid()))
                return Test.FAIL;
            
            // If player can interact with tameable mobs
            if ((chunk != null) && ((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.HURT_TAMED))
                return Test.SUCCESS;
        }
        
        // Fail as a callback
        if (target instanceof LivingEntity) {
            // If player can interact with docile mobs
            if (ClaimChunkUtils.canPlayerInteractFriendlies(attacker, target.getBlockPos()))
                return Test.SUCCESS;
            
            target.playSound(EntityLockUtils.getLockSound(target), 0.5f, 1);
        }
        
        return Test.CONTINUE;
    }
    private static Test creeperAttacks(@NotNull CreeperEntity attacker, @NotNull final Entity target, @NotNull final World world, @NotNull final DamageSource source) {
        // Protect item frames if creeper damage is off
        if (target instanceof ItemFrameEntity itemFrame) {
            WorldChunk chunk = world.getWorldChunk(itemFrame.getBlockPos());
            if ((chunk != null) && !((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.CREEPER_GRIEFING))
                return Test.FAIL;
        }
        
        return Test.SUCCESS;
    }
    
    private static Entity getRootAttacker(@Nullable DamageSource source) {
        if (source == null)
            return null;
        return EntityAttack.getRootAttacker(source.getAttacker());
    }
    private static Entity getRootAttacker(@Nullable Entity source) {
        if (source == null)
            return null;
        Entity child = null;
        if (source instanceof LightningEntity)
            child = ((LightningAccessor) source).getChanneler();
        else if (source instanceof TntEntity tntEntity)
            child = tntEntity.getOwner();
        else if (source instanceof ProjectileEntity projectileEntity)
            child = projectileEntity.getOwner();
        return child == null ? source : EntityAttack.getRootAttacker(child);
    }
}
