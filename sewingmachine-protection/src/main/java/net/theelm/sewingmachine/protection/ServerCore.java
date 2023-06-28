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

package net.theelm.sewingmachine.protection;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.events.MessageDeployer;
import net.theelm.sewingmachine.events.RegionManageCallback;
import net.theelm.sewingmachine.events.RegionUpdateCallback;
import net.theelm.sewingmachine.events.PlayerCanTeleport;
import net.theelm.sewingmachine.events.RegionNameCallback;
import net.theelm.sewingmachine.interfaces.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.BlockPlaceCallback;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.interfaces.variables.EntityVariableFunction;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.events.BlockBreak;
import net.theelm.sewingmachine.protection.events.BlockInteraction;
import net.theelm.sewingmachine.protection.events.EntityAttack;
import net.theelm.sewingmachine.protection.events.ItemPlace;
import net.theelm.sewingmachine.protection.events.ItemUse;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.interfaces.PlayerMovement;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.PlayerTravel;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.MessageClaimUtils;
import net.theelm.sewingmachine.utilities.EntityVariables;
import net.theelm.sewingmachine.utilities.text.TextUtils;

import java.util.UUID;

/**
 * Created on Jun 08 2023 at 11:58 PM.
 * By greg in sewingmachine
 */
public class ServerCore implements DedicatedServerModInitializer, SewPlugin {
    @Override
    public void onInitializeServer() {
        // Create registry based listeners
        BlockBreak.register(BlockBreakCallback.EVENT);
        BlockInteraction.register(BlockInteractionCallback.EVENT);
        EntityAttack.register(DamageEntityCallback.EVENT);
        ItemPlace.register(BlockPlaceCallback.EVENT);
        ItemUse.register(ItemUseCallback.EVENT);
        
        // Create a callback for when details about who owns an area is updated
        RegionUpdateCallback.EVENT.register((owner, refresh) -> {
            // Update the name in the claim cache
            ClaimantPlayer claim = ((PlayerClaimData) owner).getClaim();
            
            // Update claim details
            if (refresh) {
                claim.updateName();
            }
            
            // Notify players of change
            claim.visitors(player -> {
                PlayerMovement movement = ((PlayerMovement) player.networkHandler);
                movement.showPlayerNewLocation(player, player.getWorld().getWorldChunk(player.getBlockPos()));
            });
        });
        
        // Create a handler for claiming regions
        RegionManageCallback.HANDLER.register((world, player, region, claimed) -> {
            if (claimed) {
                if (!ClaimChunkUtils.canPlayerClaimSlices(world, region))
                    return false;
                
                ClaimChunkUtils.claimSlices(world, player, region);
                return true;
            }
            
            ClaimChunkUtils.unclaimSlices(world, region);
            return true;
        });
        
        // Create a callback for getting a players whereabouts
        RegionNameCallback.EVENT.register((world, pos, entity, nameOnly, strict) -> {
            IClaimedChunk chunk = (IClaimedChunk) world.getWorldChunk(pos);
            
            // If the chunk is a Town, return the Town
            ClaimantTown town = chunk.getTown();
            if (town != null)
                return town.getName();
            
            // If the position is unowned, return Wilderness
            UUID owner = chunk.getOwnerId(pos);
            if (owner == null) {
                if (strict && entity instanceof PlayerEntity player)
                    return ClaimChunkUtils.getPlayerWorldWilderness(player);
                return null;
            }
            
            ClaimCache claims = chunk.getClaimCache();
            ClaimantPlayer claim = claims.getPlayerClaim(owner);
            
            Text name = claim.getName(entity instanceof PlayerEntity player ? player.getUuid() : null);
            if (nameOnly)
                return name;
            
            return Text.literal("")
                .append(name)
                .append("'s claimed area");
        });
        
        // Allow teleporting to warps and other locations if the player is within Spawn
        PlayerCanTeleport.TEST.register((server, player, uuid) -> ClaimChunkUtils.isPlayerWithinSpawn((ServerPlayerEntity) player));
        
        // Get town name
        EntityVariables.add("town", (EntityVariableFunction)(source, entity, casing) -> {
            ClaimantTown town = null;
            if (entity instanceof ServerPlayerEntity player) {
                ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
                if (claim != null)
                    town = claim.getTown();
            }
            return town == null ? TextUtils.literal() : town.getName();
        });
        
        MessageDeployer.EVENT.register((room, player, tags, message) -> {
            // Message to the players town
            if (room instanceof Enum<?> e && "TOWN".equals(e.name())) {
                ClaimantPlayer claimantPlayer = ((PlayerClaimData) player).getClaim();
                return MessageClaimUtils.sendToTown(claimantPlayer.getTown(), tags, message);
            }
            
            return false;
        });
    }
}
