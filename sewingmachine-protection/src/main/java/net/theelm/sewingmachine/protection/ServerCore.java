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
import net.theelm.sewingmachine.events.ClaimUpdateCallback;
import net.theelm.sewingmachine.interfaces.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.BlockPlaceCallback;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.protection.events.BlockBreak;
import net.theelm.sewingmachine.protection.events.BlockInteraction;
import net.theelm.sewingmachine.protection.events.EntityAttack;
import net.theelm.sewingmachine.protection.events.ItemPlace;
import net.theelm.sewingmachine.protection.events.ItemUse;
import net.theelm.sewingmachine.protection.interfaces.PlayerMovement;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.PlayerTravel;

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
        
        ClaimUpdateCallback.EVENT.register((owner, refresh) -> {
            // Update the name in the claim cache
            ClaimantPlayer claim = ((PlayerTravel) owner).getClaim();
            
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
    }
}
