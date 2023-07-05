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

package net.theelm.sewingmachine.protection.screen;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.interfaces.NameCache;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.packets.ClaimRankPacket;
import net.theelm.sewingmachine.screens.SettingScreen;
import net.theelm.sewingmachine.screens.SettingScreenListWidget;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created on Jul 05 2023 at 2:35 PM.
 * By greg in sewingmachine
 */
public final class RankSettingsScreen extends SettingScreen {
    protected RankSettingsScreen() {
        super(Text.literal("Rank Settings"));
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        ClaimantPlayer claim = ((PlayerClaimData) this.client).getClaim();
        Map<UUID, Text> players = this.getAllPlayers(claim);
        for (Map.Entry<UUID, Text> entry : players.entrySet()) {
            // Skip over the self player (Can't change own rank)
            UUID self = this.client.getSession()
                .getUuidOrNull();
            if (self != null && self.equals(entry.getKey()))
                continue;
            UUID player = entry.getKey();
            
            list.addCycleButton(
                ClaimRanks.class,
                entry.getValue(),
                null,
                claim.getFriendRank(player),
                (button, state) -> NetworkingUtils.send(this.client, new ClaimRankPacket(player, state.get()))
            );
        }
    }
    
    private @NotNull Map<UUID, Text> getAllPlayers(@NotNull ClaimantPlayer claim) {
        Map<UUID, Text> players = new HashMap<>();
        
        // Add friends
        for (UUID uuid : claim.getFriends()) {
            // Get the friend name
            Text name = ((NameCache) this.client).getPlayerName(uuid);
            if (name == null)
                name = Text.literal("Offline player");
            
            players.put(uuid, name);
        }
        
        // Add players that are online
        Collection<PlayerListEntry> playerList = this.client.getNetworkHandler()
            .getPlayerList();
        for (PlayerListEntry entry : playerList)
            players.put(entry.getProfile().getId(), entry.getDisplayName().copyContentOnly());
        
        return players;
    }
}
