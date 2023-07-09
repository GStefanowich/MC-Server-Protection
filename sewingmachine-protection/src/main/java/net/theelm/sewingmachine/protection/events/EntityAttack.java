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
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.protection.mixins.Interfaces.LightningAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.EntityLockUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
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
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;

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
    private static ActionResult attack(@NotNull final Entity target, @NotNull final World world, @NotNull final DamageSource source) {
        final Entity attacker = EntityAttack.getRootAttacker(source);
        
        // If self harm
        if (target == attacker)
            return ActionResult.PASS;
        
        if (attacker instanceof PlayerEntity player) {
            // Always allow defending self from hostiles
            if ((target instanceof Monster) && (!(target instanceof AbstractPiglinEntity abstractPiglin) || abstractPiglin.getTarget() instanceof PlayerEntity))
                return ActionResult.PASS;
            
            // Do special item frame interaction if NOT CROUCHING and HOLDING A TOOL
            if ((target instanceof ItemFrameEntity itemFrame) && (!(player.isSneaking() && player.getMainHandStack().isDamageable()))) {
                Direction direction = itemFrame.getHorizontalFacing().getOpposite();
                
                // Get the item in the item frame
                ItemStack itemStack = itemFrame.getHeldItemStack();
                
                // If the sign actually has an item on it
                if (!itemStack.isEmpty()) {
                    // Get blocks
                    BlockPos containerPos = itemFrame.getBlockPos().offset(direction, 1);
                    Block containerBlock = world.getBlockState(containerPos).getBlock();
                    
                    // If the block behind the item frame is a storage
                    if (containerBlock instanceof ChestBlock || containerBlock instanceof BarrelBlock) {
                        // Check chunk permissions
                        if (!ClaimChunkUtils.canPlayerLootChestsInChunk(player, containerPos))
                            return ActionResult.FAIL;
                        
                        Inventory containerInventory = InventoryUtils.getInventoryOf(world, containerPos);
                        if (containerInventory != null) {
                            // The amount the player wants to take
                            int takeStackSize = (player.isSneaking() ? Collections.min(Arrays.asList(64, itemStack.getMaxCount())) : 1);
                            
                            InventoryUtils.chestToPlayer((ServerPlayerEntity) player, containerPos, containerInventory, player.getInventory(), itemStack, takeStackSize);
                            return ActionResult.FAIL;
                        }
                    }
                }
                
                // If player should be able to interact with the item frame
                if (!ClaimChunkUtils.canPlayerBreakInChunk(player, itemFrame.getBlockPos()))
                    return ActionResult.FAIL;
                
                return ActionResult.PASS;
            }
            
            // If the player is in creative, allow
            if (player.isCreative() && SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS))
                return ActionResult.PASS;
            
            // Get chunk protection
            WorldChunk chunk = world.getWorldChunk(target.getBlockPos());
            
            // If entity is a player always allow PvP, and always allow defending self from hostiles
            if ((target instanceof PlayerEntity)) {
                if (chunk != null) {
                    // If PvP is disallowed, stop the swing
                    if (!((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.PLAYER_COMBAT))
                        return ActionResult.FAIL;
                }
                
                return ActionResult.PASS;
            }
            
            // Check if the tamed entity is tamed
            if ((target instanceof TameableEntity tameableEntity) && (((TameableEntity) target).getOwnerUuid() != null)) {
                // Deny if the entity belongs to the attacker (Can't hurt friendlies)
                if (player.getUuid().equals(tameableEntity.getOwnerUuid()))
                    return ActionResult.FAIL;
                
                // If player can interact with tameable mobs
                if ((chunk != null) && ((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.HURT_TAMED))
                    return ActionResult.PASS;
                
            } else {
                // If player can interact with docile mobs
                if (ClaimChunkUtils.canPlayerInteractFriendlies(player, target.getBlockPos()))
                    return ActionResult.PASS;
            }
            
            if (target instanceof LivingEntity)
                target.playSound(EntityLockUtils.getLockSound(target), 0.5f, 1);
            
        } else if (attacker instanceof CreeperEntity) {
            // Protect item frames if creeper damage is off
            if (target instanceof ItemFrameEntity itemFrame) {
                WorldChunk chunk = world.getWorldChunk(itemFrame.getBlockPos());
                if ((chunk == null) || ((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.CREEPER_GRIEFING))
                    return ActionResult.PASS;
            } else {
                return ActionResult.PASS;
            }
            
        } else {
            // Pass for all other entity attackers
            return ActionResult.PASS;
            
        }
        
        // Fail as a callback
        return ActionResult.FAIL;
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
