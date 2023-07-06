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

import com.mojang.authlib.GameProfile;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
        Map<ClaimRanks, Set<GameProfile>> ranked = this.getRankedPlayers(claim);
        for (ClaimRanks rank : ClaimRanks.values()) {
            Set<GameProfile> profiles = ranked.get(rank);
            if (profiles == null)
                continue;
            
            for (GameProfile profile : profiles) {
                // Skip over the self player (Can't change own rank)
                UUID self = this.client.getSession()
                    .getUuidOrNull();
                UUID player = profile.getId();
                if (self != null && self.equals(player))
                    continue;
                
                list.addCycleButton(
                    ClaimRanks.class,
                    Text.literal(profile.getName()),
                    null,
                    claim.getFriendRank(player),
                    (button, state) -> NetworkingUtils.send(this.client, new ClaimRankPacket(player, state.get()))
                );
            }
        }
    }
    
    private @NotNull Map<ClaimRanks, Set<GameProfile>> getRankedPlayers(@NotNull ClaimantPlayer claim) {
        Map<ClaimRanks, Set<GameProfile>> map = new HashMap<>();
        for (GameProfile profile : this.getAllPlayers(claim)) {
            ClaimRanks rank = claim.getFriendRank(profile.getId());
            
            Set<GameProfile> profiles = map.get(rank);
            if (profiles == null) {
                profiles = new TreeSet<>(ProfileComparator.INSTANCE);
                map.put(rank, profiles);
            }
            
            profiles.add(profile);
        }
        
        return map;
    }
    
    private @NotNull List<GameProfile> getAllPlayers(@NotNull ClaimantPlayer claim) {
        List<GameProfile> players = new ArrayList<>();
        
        // Add friends
        for (UUID uuid : claim.getFriends()) {
            // Get the friend name
            Text name = ((NameCache) this.client).getPlayerName(uuid);
            players.add(new GameProfile(uuid, name == null ? "Offline player" : name.getString()));
        }
        
        // Add players that are online
        Collection<PlayerListEntry> playerList = this.client.getNetworkHandler()
            .getPlayerList();
        for (PlayerListEntry entry : playerList)
            players.add(entry.getProfile());
        
        return players;
    }
    
    private static final class ProfileComparator implements Comparator<GameProfile> {
        public static final @NotNull ProfileComparator INSTANCE = new ProfileComparator();
        
        @Override
        public int compare(GameProfile one, GameProfile two) {
            return one.getName().compareTo(two.getName());
        }
    }
}
