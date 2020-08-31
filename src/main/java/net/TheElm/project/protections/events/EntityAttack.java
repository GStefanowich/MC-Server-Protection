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

package net.TheElm.project.protections.events;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.DamageEntityCallback;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.Collections;

public final class EntityAttack {
    
    private EntityAttack() {}
    
    /**
     * Initialize our callback listener for Entity Attacks
     */
    public static void init() {
        // Register our event for when an entity is attacked
        DamageEntityCallback.EVENT.register( EntityAttack::attack );
    }
    
    /**
     * The event to trigger when an attack event occurs on an entity
     * @param target The target entity of the attack
     * @param world The world that the damage took place in
     * @param source The damage source, damage applied, attacker, etc
     * @return SUCCESS - Cancel and return true; PASS - Allow the interaction; FAIL - Cancel the interaction
     */
    private static ActionResult attack(final Entity target, final World world, final DamageSource source, float damage) {
        final Entity attacker = source.getAttacker();
        SoundEvent sound = null;
        
        // If self harm
        if (target == attacker)
            return ActionResult.PASS;
        
        if (attacker instanceof PlayerEntity) {
            final PlayerEntity player = (PlayerEntity) attacker;
            
            // Always allow defending self from hostiles
            if (target instanceof HostileEntity && ((!(target instanceof PiglinEntity)) || (((PiglinEntity)target).getTarget() instanceof PlayerEntity)))
                return ActionResult.PASS;
            
            // Do special item frame interaction if NOT CROUCHING and HOLDING A TOOL
            if ((target instanceof ItemFrameEntity) && (!(player.isSneaking() && player.getMainHandStack().isDamageable()))) {
                ItemFrameEntity itemFrame = (ItemFrameEntity) target;
                Direction direction = itemFrame.getHorizontalFacing().getOpposite();
                
                // Get the item in the item frame
                ItemStack itemStack = itemFrame.getHeldItemStack();
                
                // Get blocks
                BlockPos containerPos = itemFrame.getBlockPos().offset(direction, 1);
                Block containerBlock = world.getBlockState(containerPos).getBlock();
                
                // If the block behind the item frame is a storage
                if (containerBlock instanceof ChestBlock || containerBlock instanceof BarrelBlock) {
                    // Check chunk permissions
                    if (!ChunkUtils.canPlayerLootChestsInChunk(player, containerPos))
                        return ActionResult.FAIL;
                    
                    Inventory containerInventory = InventoryUtils.getInventoryOf(world, containerPos);
                    if (containerInventory != null) {
                        // The amount the player wants to take
                        int takeStackSize = (player.isSneaking() ? Collections.min(Arrays.asList(64, itemStack.getMaxCount())) : 1);

                        InventoryUtils.chestToPlayer((ServerPlayerEntity) player, containerPos, containerInventory, player.inventory, itemStack.getItem(), takeStackSize);
                        return ActionResult.FAIL;
                    }
                }
                
                // If player should be able to interact with the item frame
                if (!ChunkUtils.canPlayerBreakInChunk(player, itemFrame.getBlockPos()))
                    return ActionResult.FAIL;
                
                return ActionResult.PASS;
            }
            
            // If the player is in creative, allow
            if (player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS))
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
            if ((target instanceof TameableEntity) && (((TameableEntity) target).getOwner() != null)) {
                // Deny if the entity belongs to the attacker (Can't hurt friendlies)
                if (player.getUuid().equals(((TameableEntity) target).getOwnerUuid()))
                    return ActionResult.FAIL;
                
                // If player can interact with tameable mobs
                if ((chunk != null) && ((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.HURT_TAMED))
                    return ActionResult.PASS;
                
            } else {
                // If player can interact with docile mobs
                if (ChunkUtils.canPlayerInteractFriendlies(player, target.getBlockPos()))
                    return ActionResult.PASS;
            }
            
            if (target instanceof LivingEntity)
                sound = EntityUtils.getLockSound(target);
            
            if (sound != null)
                target.playSound(EntityUtils.getLockSound(target), 0.5f, 1);
            
        } else if (attacker instanceof CreeperEntity) {
            final CreeperEntity creeper = (CreeperEntity) attacker;
    
            // Protect item frames if creeper damage is off
            if (target instanceof ItemFrameEntity) {
                ItemFrameEntity itemFrame = (ItemFrameEntity) target;
                WorldChunk chunk = world.getWorldChunk(itemFrame.getBlockPos());
                if ((chunk == null) || ((IClaimedChunk) chunk).isSetting(target.getBlockPos(), ClaimSettings.CREEPER_GRIEFING))
                    return ActionResult.PASS;
            }
        } else {
            // Pass for all other entity attackers
            return ActionResult.PASS;
            
        }
        
        // Fail as a callback
        return ActionResult.FAIL;
    }
    
}
