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

package net.TheElm.project.protections;

import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collections;

public class EntityAttack {
    
    private EntityAttack() {}
    
    public static void init() {
        
        // Register our event for when an entity is attacked
        AttackEntityCallback.EVENT.register( EntityAttack::attack );
        
    }
    
    private static ActionResult attack(final PlayerEntity player, final World world, final Hand hand, final Entity entity, final EntityHitResult entityHitResult) {
        SoundEvent sound = null;
        
        // Always allow defending self from hostiles
        if ( entity instanceof HostileEntity )
            return ActionResult.PASS;
        
        // Do special item frame interaction if NOT CROUCHING and HOLDING A TOOL
        if (( entity instanceof ItemFrameEntity ) && (!( player.isSneaking() && player.getStackInHand( hand ).isDamageable() ))) {
            ItemFrameEntity itemFrame = (ItemFrameEntity) entity;
            Direction direction = itemFrame.getHorizontalFacing().getOpposite();
            
            // Get the item in the item frame
            ItemStack itemStack = itemFrame.getHeldItemStack();
            
            // Get blocks
            BlockPos containerPos = itemFrame.getBlockPos().offset( direction, 1 );
            
            // Check chunk permissions
            if (!ChunkUtils.canPlayerLootChestsInChunk( player, containerPos ))
                return ActionResult.FAIL;
            
            Inventory containerInventory = InventoryUtils.getInventoryOf( world, containerPos );
            if ( containerInventory != null ) {
                // The amount the player wants to take
                int takeStackSize = ( player.isSneaking() ? Collections.min(Arrays.asList( 64, itemStack.getMaxCount() )) : 1 );
                
                InventoryUtils.chestToPlayer( (ServerPlayerEntity)player, containerInventory, player.inventory, itemStack.getItem(), takeStackSize );
                return ActionResult.FAIL;
            }
        }
        
        // Get chunk protection
        ClaimedChunk claimedChunkInfo = ClaimedChunk.convert(world, entity.getBlockPos());
        
        // If entity is a player always allow PvP, and always allow defending self from hostiles
        if ( ( entity instanceof PlayerEntity ) ) {
            if ( claimedChunkInfo != null ) {
                // If PvP is disallowed, stop the swing
                if ( !claimedChunkInfo.isSetting(ClaimSettings.PLAYER_COMBAT) )
                    return ActionResult.FAIL;
            }
            
            return ActionResult.PASS;
        }
        
        // If the player is in creative, allow
        if ( player.isCreative() )
            return ActionResult.PASS;
        
        // Check if the tamed entity is tamed
        if ( ( entity instanceof TameableEntity ) && (((TameableEntity) entity).getOwner() != null) ) {
            // Allow of the entity belongs to the attacker
            if (player.getUuid().equals(((TameableEntity) entity).getOwnerUuid()))
                return ActionResult.PASS;
            
            // If player can interact with tameable mobs
            if (( claimedChunkInfo != null ) && claimedChunkInfo.isSetting( ClaimSettings.HURT_TAMED ))
                return ActionResult.PASS;
            
        } else {
            // If player can interact with docile mobs
            if (ChunkUtils.canPlayerInteractFriendlies(player, entity.getBlockPos()))
                return ActionResult.PASS;
        }
        
        if (entity instanceof LivingEntity)
            sound = EntityUtils.getLockSound(entity);
        
        if (sound != null)
            entity.playSound( EntityUtils.getLockSound( entity ), 0.5f, 1 );
        
        return ActionResult.FAIL;
    }
    
}
