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

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.theelm.sewingmachine.base.packets.CancelMinePacket;
import net.theelm.sewingmachine.events.TabRegisterEvent;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.ClientClaimData;
import net.theelm.sewingmachine.protection.interfaces.ClientMiner;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.interfaces.NameCache;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.inventory.ProtectionsTab;
import net.theelm.sewingmachine.protection.packets.ClaimCountPacket;
import net.theelm.sewingmachine.protection.packets.ClaimPermissionPacket;
import net.theelm.sewingmachine.protection.packets.ClaimRankPacket;
import net.theelm.sewingmachine.protection.packets.ClaimSettingPacket;
import net.theelm.sewingmachine.protection.packets.ClaimedChunkPacket;
import net.theelm.sewingmachine.utilities.NetworkingUtils;

import java.util.UUID;

/**
 * Created on Jul 01 2023 at 3:14 PM.
 * By greg in sewingmachine
 */
public class ClientCore implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkingUtils.clientReceiver(CancelMinePacket.TYPE, (client, network, packet, sender) -> {
            ((ClientMiner) client).stopMining(packet.pos());
        });
        
        NetworkingUtils.clientReceiver(ClaimSettingPacket.TYPE, (client, network, packet, sender) -> {
            ClaimantPlayer claim = ((PlayerClaimData) client).getClaim();
            claim.updateSetting(packet.setting(), packet.enabled());
        });
        
        NetworkingUtils.clientReceiver(ClaimPermissionPacket.TYPE, (client, network, packet, sender) -> {
            ClaimantPlayer claim = ((PlayerClaimData) client).getClaim();
            claim.updatePermission(packet.permission(), packet.rank());
        });
        
        NetworkingUtils.clientReceiver(ClaimRankPacket.TYPE, (client, network, packet, sender) -> {
            ClaimantPlayer claim = ((PlayerClaimData) client).getClaim();
            ((NameCache) client).setPlayerName(packet.player(), packet.text());
            
            // Store the rank
            claim.updateFriend(packet.player(), packet.rank());
        });
        
        NetworkingUtils.clientReceiver(ClaimCountPacket.TYPE, (client, network, packet, sender) -> {
            ((ClientClaimData) client).setClaimedChunks(packet.current());
            ((ClientClaimData) client).setMaximumChunks(packet.maximum());
        });
        
        NetworkingUtils.clientReceiver(ClaimedChunkPacket.TYPE, (client, network, packet, sender) -> {
            ClientWorld world = client.world;
            ChunkPos chunkPos = packet.chunkPos();
            
            // Update the chunks owner after receiving a query
            if (world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof IClaimedChunk chunk) {
                UUID owner = packet.owner();
                
                // Update the name in our cache
                if (owner != null) {
                    Text name = packet.name();
                    if (name != null)
                        ((NameCache) client).setPlayerName(owner, name);
                }
                
                // Update the chunk owner
                chunk.updatePlayerOwner(owner, false);
            }
        });
        
        // Add the protections tab to the inventory
        TabRegisterEvent.register(ProtectionsTab::new);
    }
}
